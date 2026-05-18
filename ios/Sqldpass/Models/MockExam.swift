import Foundation

/// 백엔드 응답: GET /api/mock-exams (각 원소)
struct MockExamSummary: Codable, Equatable, Hashable, Identifiable {
    let id: Int64
    let name: String
    let examType: String              // SQLD | ENGINEER_PRACTICAL | ...
    let sequence: Int
    let totalQuestions: Int
    let createdAt: String
    let difficultyLabel: String?      // 쉬움/보통/어려움/매우 어려움
    let solved: Bool
    let bestCorrectCount: Int?
    let bestTotalCount: Int?
    let templateKey: String?
    let templateLabel: String?
    let visibility: String            // PUBLIC | PREMIUM
    let expertVerified: Bool
    let kind: String                  // MOCK | PAST_EXAM
    let examYear: Int?
    let examRound: Int?
    let examDate: String?
    let publishedAt: String?
    let pastExamLinkedAt: String?
    let purchased: Bool
    let isPremium: Bool
}
