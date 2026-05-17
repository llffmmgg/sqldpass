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
/// - 401 응답: `onUnauthorized` 콜백 발생 후 `APIError.unauthorized` throw.
///   호출자는 콜백에서 토큰 폐기 + 인증 화면 전환을 처리한다.
final class APIClient {
    let baseURL: URL
    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    /// 매 요청 직전에 호출. nil 이면 Authorization 헤더 미주입.
    var tokenProvider: () -> String? = { nil }
    /// 401 응답 시 호출. 메인 액터에서 토큰 폐기/라우팅 처리 권장.
    var onUnauthorized: (@Sendable () -> Void)?

    init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        self.decoder = decoder

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        self.encoder = encoder
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

    // MARK: - Core send

    private func send<R: Decodable, B: Encodable>(
        path: String,
        method: HTTPMethod,
        query: [URLQueryItem] = [],
        body: B?
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
            onUnauthorized?()
            throw APIError.unauthorized
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
