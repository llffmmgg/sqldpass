import Foundation
import Observation

/// 인증 상태 + JWT 보관 단일 출처. Keychain 기반 영속화.
///
/// - JWT 가 있으면 `isAuthenticated == true` 로 평가, SessionGate 가 MainTabView 진입.
/// - APIClient 의 `tokenProvider` 에 메서드 참조를 주입한다.
/// - 401 응답 시 `refresh()` 으로 토큰 갱신을 시도하고, 실패 시 `signOut()` 으로 폐기.
@Observable
final class AuthStore {
    static let shared = AuthStore()

    private let keychain = KeychainStore(service: "com.sqldpass.app.auth", account: "jwt")

    private(set) var token: String?
    private(set) var nickname: String?

    var isAuthenticated: Bool { token != nil }

    private init() {
        self.token = keychain.load()
    }

    func signIn(token: String, nickname: String) {
        do {
            try keychain.save(token)
            self.token = token
            self.nickname = nickname
        } catch {
            // Keychain 쓰기 실패는 드물지만 기록만 하고 메모리 상태는 유지
            self.token = token
            self.nickname = nickname
        }
    }

    func signOut() {
        keychain.delete()
        token = nil
        nickname = nil
    }

    /// APIClient.tokenProvider 에 주입할 수 있는 클로저.
    func tokenProvider() -> String? { token }

    /// 401 응답 시 `POST /api/auth/refresh` 로 새 JWT 를 발급받는다.
    /// APIClient 를 거치지 않고 raw URLSession 으로 호출 — refresh 자체가 또 401 을
    /// 받았을 때 재귀 갱신 루프에 빠지지 않도록.
    /// - Returns: 갱신 성공 여부. 실패하면 토큰은 그대로 두고 caller 가 signOut 처리.
    @MainActor
    func refresh() async -> Bool {
        guard let current = token else { return false }

        var request = URLRequest(url: APIEnvironment.current.appendingPathComponent("/api/auth/refresh"))
        request.httpMethod = "POST"
        request.setValue("Bearer \(current)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                return false
            }
            let decoded = try JSONDecoder().decode(RefreshResponse.self, from: data)
            try? keychain.save(decoded.token)
            self.token = decoded.token
            if let nick = decoded.nickname {
                self.nickname = nick
            }
            return true
        } catch {
            return false
        }
    }

    private struct RefreshResponse: Decodable {
        let token: String
        let nickname: String?
    }
}
