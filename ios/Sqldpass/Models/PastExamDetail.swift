import Foundation

/// 백엔드 응답: GET /api/public/past-exams/{id}
///
/// `PastExamPublicDtos.PastExamDetail` 와 1:1. `questions` 의 원소 필드(`id, displayOrder,
/// content, questionType, subjectId, subjectName`) 가 모의고사 상세와 동일하므로 iOS 는
/// `MockExamQuestionItem` 을 재사용한다 — 풀이 chrome(`QuestionBody`, `OMRAnswerGrid` 등) 을
/// 새로 만들지 않기 위해.
struct PastExamDetailResponse: Codable, Equatable, Identifiable {
    let id: Int64
    let name: String
    let examType: String?
    let certSlug: String?
    let totalQuestions: Int
    let examYear: Int?
    let examRound: Int?
    let examDate: String?
    let expertVerified: Bool
    let questions: [MockExamQuestionItem]

    enum CodingKeys: String, CodingKey {
        case id, name, examType, certSlug, totalQuestions
        case examYear, examRound, examDate, expertVerified, questions
    }

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
        questions = try c.decode([MockExamQuestionItem].self, forKey: .questions)
    }
}
