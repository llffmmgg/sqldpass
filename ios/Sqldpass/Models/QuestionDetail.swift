import Foundation

/// 백엔드 응답: GET /api/questions/{id} (정답·해설 포함).
///
/// MCQ: correctOption(1~4), answer/keywords nil.
/// SHORT_ANSWER/DESCRIPTIVE: answer(모범답안) + keywords(허용 표기/채점 키워드).
struct QuestionDetail: Codable, Equatable, Identifiable {
    let id: Int64
    let subjectId: Int64?
    let content: String
    let questionType: String
    let correctOption: Int?
    let answer: String?
    let keywords: [String]
    let explanation: String?

    enum CodingKeys: String, CodingKey {
        case id, subjectId, content, questionType, correctOption, answer, keywords, explanation
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int64.self, forKey: .id)
        subjectId = try c.decodeIfPresent(Int64.self, forKey: .subjectId)
        content = try c.decode(String.self, forKey: .content)
        questionType = try c.decode(String.self, forKey: .questionType)
        correctOption = try c.decodeIfPresent(Int.self, forKey: .correctOption)
        answer = try c.decodeIfPresent(String.self, forKey: .answer)
        keywords = (try? c.decode([String].self, forKey: .keywords)) ?? []
        explanation = try c.decodeIfPresent(String.self, forKey: .explanation)
    }
}
