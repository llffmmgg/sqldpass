import Foundation

/// 백엔드 응답: GET /api/wrong-answers
struct WrongAnswer: Codable, Equatable, Identifiable {
    let id: Int64
    let questionId: Int64
    let chosenAnswer: String?
    let correctAnswer: String?
    let retryCount: Int
    let lastSolvedAt: String
}

struct WrongAnswerStats: Codable, Equatable {
    let totalCount: Int
    let bySubject: [SubjectCount]
}

struct SubjectCount: Codable, Equatable, Identifiable {
    let subjectId: Int64
    let subjectName: String
    let count: Int
    var id: Int64 { subjectId }
}
