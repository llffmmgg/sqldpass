import Foundation

/// Google Sign-In 어댑터. **현재 placeholder** — Phase 2.4 에서 GoogleSignIn-iOS SPM
/// 의존성 추가 후 실제 흐름을 구현한다.
///
/// 실제 구현 예정 흐름:
/// 1. `GIDSignIn.sharedInstance.signIn(presenting:)` 호출
/// 2. `user.idToken?.tokenString` 추출
/// 3. `POST /api/auth/login/google/idtoken` 호출
/// 4. AuthStore.signIn 으로 JWT 저장
enum GoogleAuthService {
    /// SDK 미설치 상태에서는 항상 실패.
    static func signIn() async throws {
        throw APIError.unknown(message: "Google 로그인은 곧 지원 예정입니다. 우선 Apple 로 로그인해주세요.")
    }
}
