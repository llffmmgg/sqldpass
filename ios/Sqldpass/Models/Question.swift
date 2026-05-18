import Foundation

/// 백엔드 응답: GET /api/questions (each)
struct Question: Codable, Equatable, Identifiable {
    let id: Int64
    let subjectId: Int64
    let content: String
    /// MULTIPLE_CHOICE | SHORT_ANSWER | ...
    let questionType: String
}
