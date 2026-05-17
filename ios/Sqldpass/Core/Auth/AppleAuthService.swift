import AuthenticationServices
import Foundation

/// Sign in with Apple → 백엔드 `POST /api/auth/login/apple` 교환 후 AuthStore 갱신.
///
/// SwiftUI `SignInWithAppleButton` 의 `onCompletion` 콜백에서 결과를 받아
/// `exchange(credential:)` 에 넘긴다.
enum AppleAuthService {
    /// 백엔드와 교환. 성공 시 AuthStore.signIn 호출.
    static func exchange(credential: ASAuthorizationAppleIDCredential) async throws {
        guard let identityTokenData = credential.identityToken,
              let identityToken = String(data: identityTokenData, encoding: .utf8) else {
            throw APIError.unknown(message: "Apple ID Token 을 읽지 못했습니다.")
        }

        let authorizationCode: String?
        if let codeData = credential.authorizationCode,
           let code = String(data: codeData, encoding: .utf8) {
            authorizationCode = code
        } else {
            authorizationCode = nil
        }

        // 닉네임은 첫 로그인에서만 Apple 이 제공.
        let nicknameFromApple: String?
        if let formatter = credential.fullName,
           let nickname = [formatter.familyName, formatter.givenName]
                .compactMap({ $0 })
                .joined(separator: " ")
                .nonEmptyOrNil {
            nicknameFromApple = nickname
        } else {
            nicknameFromApple = nil
        }

        let request = AppleLoginRequest(
            idToken: identityToken,
            authorizationCode: authorizationCode,
            nickname: nicknameFromApple
        )

        let response: OAuthLoginResponse = try await APIClient.shared.post(
            "/api/auth/login/apple",
            body: request
        )

        await MainActor.run {
            AuthStore.shared.signIn(token: response.token, nickname: response.nickname)
        }
    }
}

private extension String {
    var nonEmptyOrNil: String? { isEmpty ? nil : self }
}
