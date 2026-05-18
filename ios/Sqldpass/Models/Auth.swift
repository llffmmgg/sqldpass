import Foundation

/// 백엔드 응답: POST /api/auth/login/google/idtoken,  POST /api/auth/login/apple
struct OAuthLoginResponse: Codable, Equatable {
    let token: String
    let nickname: String
    let isNew: Bool
}

/// 요청 바디: Google ID Token 로그인
struct GoogleIdTokenLoginRequest: Codable {
    let idToken: String
}

/// 요청 바디: Sign in with Apple
struct AppleLoginRequest: Codable {
    let idToken: String
    let authorizationCode: String?
    /// 첫 로그인에서만 Apple 이 제공. 이후엔 nil.
    let nickname: String?
}
