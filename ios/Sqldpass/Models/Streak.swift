import Foundation

/// 백엔드 응답: GET /api/streak/me
struct StreakInfo: Codable, Equatable {
    let currentStreak: Int
    let longestStreak: Int
    /// ISO 8601 date (yyyy-MM-dd)
    let lastSolveDate: String?
    let solvedToday: Bool
}
