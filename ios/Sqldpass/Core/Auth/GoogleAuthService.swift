import Foundation
import GoogleSignIn
import UIKit

/// Google Sign-In → 백엔드 `POST /api/auth/login/google/idtoken` 교환 후 AuthStore 갱신.
///
/// 호출자(AuthView)가 `await GoogleAuthService.signIn()` 으로 진입.
/// 백엔드는 idToken 의 audience 가 iOS Client ID 와 일치하는지 검증한다.
enum GoogleAuthService {
    static func signIn() async throws {
        let presenter = try await topViewController()
        let signInResult: GIDSignInResult
        do {
            signInResult = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
        } catch {
            // 사용자 취소는 별도 에러 코드 — 호출자가 UI 알림 안 띄우도록 그대로 throw.
            throw error
        }

        guard let idToken = signInResult.user.idToken?.tokenString else {
            throw APIError.unknown(message: "Google ID Token 을 받지 못했습니다.")
        }

        let request = GoogleIdTokenLoginRequest(idToken: idToken)
        let response: OAuthLoginResponse = try await APIClient.shared.post(
            "/api/auth/login/google/idtoken",
            body: request
        )

        await MainActor.run {
            AuthStore.shared.signIn(token: response.token, nickname: response.nickname)
        }
    }

    /// 현재 키 윈도우의 최상위 ViewController. presenting controller 로 사용.
    @MainActor
    private static func topViewController() throws -> UIViewController {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        guard let window = scene?.windows.first(where: { $0.isKeyWindow }) ?? scene?.windows.first,
              var top = window.rootViewController else {
            throw APIError.unknown(message: "최상위 화면을 찾지 못했습니다.")
        }
        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
