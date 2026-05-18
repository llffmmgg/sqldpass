import Foundation

/// 백엔드 응답: GET /api/mock-exams/{id}
struct MockExamDetail: Codable, Equatable, Identifiable {
    let id: Int64
    let name: String
    let examType: String
    let sequence: Int
    let totalQuestions: Int
    let createdAt: String
    let expertVerified: Bool
    let kind: String              // MOCK | PAST_EXAM
    let examYear: Int?
    let examRound: Int?
    let examDate: String?
    let questions: [MockExamQuestionItem]
}

struct MockExamQuestionItem: Codable, Equatable, Identifiable {
    let id: Int64
    let displayOrder: Int
    let content: String
    let questionType: String      // MULTIPLE_CHOICE | SHORT_ANSWER ...
    let subjectId: Int64
    let subjectName: String
}
