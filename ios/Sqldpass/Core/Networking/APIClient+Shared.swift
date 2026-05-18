import Foundation

extension APIClient {
    /// 앱 전체에서 공유하는 클라이언트. AuthStore 와 자동 연결된다.
    /// - tokenProvider: AuthStore 현재 토큰
    /// - onUnauthorized: 401 응답 시 메인 액터에서 토큰 폐기 + 인증 화면 라우팅
    static let shared: APIClient = {
        let client = APIClient(baseURL: APIEnvironment.current)
        client.tokenProvider = { AuthStore.shared.tokenProvider() }
        client.onUnauthorized = {
            Task { @MainActor in
                AuthStore.shared.signOut()
            }
        }
        return client
    }()
}
