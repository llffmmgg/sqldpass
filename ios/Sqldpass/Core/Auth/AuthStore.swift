import Foundation
import Observation

/// 인증 상태 + JWT 보관 단일 출처. Keychain 기반 영속화.
///
/// - JWT 가 있으면 `isAuthenticated == true` 로 평가, SessionGate 가 MainTabView 진입.
/// - APIClient 의 `tokenProvider` 에 메서드 참조를 주입한다.
/// - 401 응답 시 `signOut()` 으로 토큰 폐기.
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
            print("[AuthStore] keychain save failed: \(error)")
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
}
