import Foundation

enum MemberService {
    /// GET /api/members/me — 현재 로그인 사용자 정보
    static func me() async throws -> MemberMe {
        try await APIClient.shared.get("/api/members/me")
    }

    /// DELETE /api/members/me — 계정 삭제 (App Store 필수)
    static func deleteAccount() async throws {
        try await APIClient.shared.delete("/api/members/me")
    }
}
