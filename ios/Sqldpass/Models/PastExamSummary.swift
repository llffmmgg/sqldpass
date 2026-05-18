import Foundation

/// 백엔드 응답: GET /api/public/past-exams?cert={slug} (each)
///
/// Android 미러: `mobile/app/src/main/java/com/sqldpass/app/data/ApiModels.kt`
/// 의 `PastExamSummary` 와 동일 필드. iOS 는 `Int64` / `String?` 패턴.
struct PastExamSummary: Codable, Equatable, Hashable, Identifiable {
    let id: Int64
    let name: String
    let examType: String?
    let certSlug: String?
    let totalQuestions: Int
    let examYear: Int?
    let examRound: Int?
    let examDate: String?
    let expertVerified: Bool
    let createdAt: String?
    let solved: Bool
    let bestCorrectCount: Int?
    let bestTotalCount: Int?

    /// 서버 응답 누락 필드를 안전하게 기본값으로 디코딩하기 위한 init.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int64.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        examType = try c.decodeIfPresent(String.self, forKey: .examType)
        certSlug = try c.decodeIfPresent(String.self, forKey: .certSlug)
        totalQuestions = try c.decode(Int.self, forKey: .totalQuestions)
        examYear = try c.decodeIfPresent(Int.self, forKey: .examYear)
        examRound = try c.decodeIfPresent(Int.self, forKey: .examRound)
        examDate = try c.decodeIfPresent(String.self, forKey: .examDate)
        expertVerified = (try c.decodeIfPresent(Bool.self, forKey: .expertVerified)) ?? false
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
        solved = (try c.decodeIfPresent(Bool.self, forKey: .solved)) ?? false
        bestCorrectCount = try c.decodeIfPresent(Int.self, forKey: .bestCorrectCount)
        bestTotalCount = try c.decodeIfPresent(Int.self, forKey: .bestTotalCount)
    }

    private enum CodingKeys: String, CodingKey {
        case id, name, examType, certSlug, totalQuestions
        case examYear, examRound, examDate, expertVerified
        case createdAt, solved, bestCorrectCount, bestTotalCount
    }
}
