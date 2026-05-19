import Foundation

/// 백엔드 응답: POST /api/solves, GET /api/solves/{id}
///
/// 전체 풀이 객체. `answers` 가 포함된다. 풀이 제출 후 / 단건 상세 조회에 사용.
/// 목록(GET /api/solves) 응답은 답안이 빠진 요약 형태 → `SolveSummary` 참고.
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

/// 백엔드 응답: GET /api/solves (목록 각 원소 — `SolveSummaryResponse`).
///
/// `answers` 가 포함되지 않으므로 목록 화면(`HistoryView`)은 본 모델을 사용한다.
/// 상세 진입 시 `GET /api/solves/{id}` 로 전체 `Solve` 를 다시 조회.
struct SolveSummary: Codable, Equatable, Identifiable, Hashable {
    let id: Int64
    let subjectId: Int64?
    let mockExamId: Int64?
    let totalCount: Int
    let correctCount: Int
    let score: Int
    let solvedAt: String
}

/// 전체 사용자 14일 평균 풀이 수 — Dashboard 차트
struct OverallStats: Codable, Equatable {
    let avgDailyCount: Double
}
