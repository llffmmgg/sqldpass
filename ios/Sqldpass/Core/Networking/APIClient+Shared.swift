import Foundation

extension APIClient {
    /// 앱 전체에서 공유하는 클라이언트. AuthStore 와 자동 연결된다.
    /// - tokenProvider: AuthStore 현재 토큰
    /// - tokenRefresher: 401 응답 시 한 번 갱신을 시도. 성공 시 같은 요청 재실행.
    /// - onUnauthorized: 갱신 실패 시 토큰 폐기 + 인증 화면 라우팅 (메인 액터)
    static let shared: APIClient = {
        let client = APIClient(baseURL: APIEnvironment.current)
        client.tokenProvider = { AuthStore.shared.tokenProvider() }
        client.tokenRefresher = {
            await AuthStore.shared.refresh()
        }
        client.onUnauthorized = {
            Task { @MainActor in
                AuthStore.shared.signOut()
            }
        }
        return client
    }()
}
