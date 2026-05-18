import Foundation

enum StreakService {
    /// GET /api/streak/me — 현재 사용자의 연속 학습 정보
    static func me() async throws -> StreakInfo {
        try await APIClient.shared.get("/api/streak/me")
    }
}
