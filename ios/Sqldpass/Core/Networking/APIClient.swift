import Foundation

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case patch = "PATCH"
    case delete = "DELETE"
}

/// REST API 클라이언트. async/await URLSession 래퍼.
///
/// - Bearer 토큰: `tokenProvider` 클로저로 주입. 매 요청마다 호출되므로
///   AuthStore 가 토큰을 갱신해도 자동 반영.
/// - 401 응답: `tokenRefresher` 가 주입돼 있으면 한 번 갱신을 시도해 같은 요청을
///   재실행한다. 갱신 실패 시 `onUnauthorized` 콜백 발생 후 `APIError.unauthorized`
///   throw. 호출자는 콜백에서 토큰 폐기 + 인증 화면 전환을 처리한다.
final class APIClient {
    let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder
    private let refreshCoordinator = TokenRefreshCoordinator()

    /// 매 요청 직전에 호출. nil 이면 Authorization 헤더 미주입.
    var tokenProvider: () -> String? = { nil }
    /// 401 응답 시 새 토큰 발급을 시도한다. true 반환 시 같은 요청을 1회 재실행.
    /// 동시 다발 401 에 대해 내부 single-flight 가드가 한 번만 호출한다.
    var tokenRefresher: (@Sendable () async -> Bool)?
    /// 갱신 실패 또는 refresher 미주입 시 401 에서 호출. 메인 액터에서 토큰 폐기/라우팅 처리 권장.
    var onUnauthorized: (@Sendable () -> Void)?

    init(baseURL: URL, session: URLSession? = nil) {
        self.baseURL = baseURL
        self.session = session ?? APIClient.makeDefaultSession()

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder
    }

    /// APIClient 전용 URLSession.
    /// 모바일 네트워크 변동 상황에서 사용자가 60초씩 멈춰 보이지 않도록,
    /// URLSession.shared 기본값(request 60s / resource 7d) 대신 짧은 timeout 사용.
    /// `URLSession.shared` 동작은 그대로 두어 SwiftUI AsyncImage 등 다른 사용처에 영향 없음.
    private static func makeDefaultSession() -> URLSession {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15        // connect + 첫 응답 대기
        config.timeoutIntervalForResource = 30       // 전체 다운로드 한도
        config.waitsForConnectivity = false          // 즉시 실패해 사용자에게 빠른 피드백
        return URLSession(configuration: config)
    }

    // MARK: - High level helpers

    func get<R: Decodable>(_ path: String, query: [URLQueryItem] = []) async throws -> R {
        try await send(path: path, method: .get, query: query, body: Optional<EmptyBody>.none)
    }

    func post<R: Decodable, B: Encodable>(_ path: String, body: B) async throws -> R {
        try await send(path: path, method: .post, body: body)
    }

    func postVoid<B: Encodable>(_ path: String, body: B) async throws {
        let _: EmptyResponse = try await send(path: path, method: .post, body: body)
    }

    func delete(_ path: String) async throws {
        let _: EmptyResponse = try await send(path: path, method: .delete, body: Optional<EmptyBody>.none)
    }

    func patch<R: Decodable, B: Encodable>(_ path: String, body: B) async throws -> R {
        try await send(path: path, method: .patch, body: body)
    }

    // MARK: - Core send

    private func send<R: Decodable, B: Encodable>(
        path: String,
        method: HTTPMethod,
        query: [URLQueryItem] = [],
        body: B?,
        allowRefresh: Bool = true
    ) async throws -> R {
        guard var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false) else {
            throw APIError.invalidURL
        }
        if !query.isEmpty {
            components.queryItems = query
        }
        guard let url = components.url else { throw APIError.invalidURL }

        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let token = tokenProvider() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body, !(body is EmptyBody) {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            do {
                request.httpBody = try encoder.encode(body)
            } catch {
                throw APIError.decoding(message: "요청 본문 인코딩 실패: \(error.localizedDescription)")
            }
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            if let urlError = error as? URLError, urlError.code == .cancelled {
                throw APIError.cancelled
            }
            throw APIError.transport(message: error.localizedDescription)
        }

        guard let http = response as? HTTPURLResponse else {
            throw APIError.unknown(message: "응답이 HTTP 형식이 아닙니다.")
        }

        switch http.statusCode {
        case 200..<300:
            if R.self == EmptyResponse.self {
                return EmptyResponse() as! R
            }
            do {
                return try decoder.decode(R.self, from: data)
            } catch {
                throw APIError.decoding(message: error.localizedDescription)
            }
        case 401:
            if allowRefresh, let refresher = tokenRefresher {
                let refreshed = await refreshCoordinator.refresh(using: refresher)
                if refreshed {
                    return try await send(
                        path: path,
                        method: method,
                        query: query,
                        body: body,
                        allowRefresh: false
                    )
                }
            }
            onUnauthorized?()
            throw APIError.unauthorized
        case 402:
            // 무료 회원 일일 한도 초과. 백엔드는
            // `{ "error": "DAILY_QUESTION_LIMIT" | "DAILY_MOCK_LIMIT",
            //    "used": Int, "limit": Int, "resetAt": "YYYY-MM-DDTHH:mm:ss" }`
            // 형태로 응답한다. 디코딩 성공 시 quotaExceeded, 실패 시 일반 clientError 로 폴백.
            struct QuotaBody: Decodable {
                let error: String
                let used: Int
                let limit: Int
                let resetAt: String
            }
            if let body = try? decoder.decode(QuotaBody.self, from: data) {
                throw APIError.quotaExceeded(
                    code: body.error,
                    used: body.used,
                    limit: body.limit,
                    resetAt: body.resetAt
                )
            }
            throw APIError.clientError(status: 402, message: nil)
        case 403:
            throw APIError.forbidden
        case 404:
            throw APIError.notFound
        case 400..<500:
            let message = String(data: data, encoding: .utf8)
            throw APIError.clientError(status: http.statusCode, message: message)
        case 500..<600:
            throw APIError.serverError(status: http.statusCode)
        default:
            throw APIError.unknown(message: "예상치 못한 상태 코드: \(http.statusCode)")
        }
    }
}

private struct EmptyBody: Encodable {}

/// 204 No Content / 응답 본문이 없을 때 사용.
struct EmptyResponse: Decodable {
    init() {}
    init(from decoder: Decoder) throws {}
}

/// 401 폭주 시 refresh 호출이 동시에 여러 번 발사되는 것을 막는 single-flight 가드.
/// 진행 중인 갱신이 있으면 같은 결과를 await 해서 공유한다.
private actor TokenRefreshCoordinator {
    private var inFlight: Task<Bool, Never>?

    func refresh(using refresher: @escaping @Sendable () async -> Bool) async -> Bool {
        if let task = inFlight {
            return await task.value
        }
        let task = Task { await refresher() }
        inFlight = task
        let result = await task.value
        inFlight = nil
        return result
    }
}
