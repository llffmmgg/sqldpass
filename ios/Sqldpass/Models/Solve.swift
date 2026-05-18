import Foundation

/// 백엔드 응답: POST /api/solves, GET /api/solves/{id}
struct Solve: Codable, Equatable, Identifiable {
    let id: Int64
    let subjectId: Int64?
    let mockExamId: Int64?
    let totalCount: Int
    let correctCount: Int
    let score: Int
    let solvedAt: String
    let answers: [SolveAnswer]
    /// 풀이 제출 직후 반영된 연속 학습 일수
    let currentStreak: Int?
    /// 7/30/100/365 달성 시 채워짐
    let milestoneReached: Int?
}

struct SolveAnswer: Codable, Equatable, Identifiable {
    let questionId: Int64
    let selectedOption: Int?
    let correctOption: Int?
    let correct: Bool

    var id: Int64 { questionId }
}

/// 전체 사용자 14일 평균 풀이 수 — Dashboard 차트
struct OverallStats: Codable, Equatable {
    let avgDailyCount: Double
}
