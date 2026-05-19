import Foundation

/// 백엔드 응답: POST /api/public/past-exams/{id}/grade
///
/// `PastExamPublicDtos.PastExamGradeResponse` 와 1:1.
/// - `subjectScores` 는 자격증의 합격 기준 과목 단위 점수.
/// - `passed` 는 자격증별 공식 컷오프 적용 후 최종 합격 여부.
struct PastExamGradeResponse: Codable, Equatable {
    let totalCount: Int
    let correctCount: Int
    let score: Int
    let items: [PastExamGradedItem]
    /// 로그인 사용자의 경우 채점과 함께 생성된 Solve row id.
    let solveId: Int64?
    let subjectScores: [PastExamSubjectScore]
    let passed: Bool
    let passReason: String?
    /// 학습 연속일 마일스톤 도달 일수 (도달 안 했으면 nil, 비로그인이면 nil)
    let milestoneReached: Int?
}

struct PastExamGradedItem: Codable, Equatable, Identifiable {
    let questionId: Int64
    let correct: Bool
    let partialScore: Double
    let selectedOption: Int?
    let submittedAnswerText: String?
    let correctOption: Int?
    let answer: String?
    let keywords: [String]
    let explanation: String?

    var id: Int64 { questionId }

    enum CodingKeys: String, CodingKey {
        case questionId, correct, partialScore, selectedOption, submittedAnswerText
        case correctOption, answer, keywords, explanation
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        questionId = try c.decode(Int64.self, forKey: .questionId)
        correct = try c.decode(Bool.self, forKey: .correct)
        partialScore = (try c.decodeIfPresent(Double.self, forKey: .partialScore)) ?? 0
        selectedOption = try c.decodeIfPresent(Int.self, forKey: .selectedOption)
        submittedAnswerText = try c.decodeIfPresent(String.self, forKey: .submittedAnswerText)
        correctOption = try c.decodeIfPresent(Int.self, forKey: .correctOption)
        answer = try c.decodeIfPresent(String.self, forKey: .answer)
        keywords = (try c.decodeIfPresent([String].self, forKey: .keywords)) ?? []
        explanation = try c.decodeIfPresent(String.self, forKey: .explanation)
    }
}

struct PastExamSubjectScore: Codable, Equatable, Identifiable {
    let subjectName: String
    let total: Int
    let correct: Int
    /// 정답률 0~100 (소수 1자리 가능)
    let rate: Double
    /// 100점 환산 점수
    let weighted: Int
    /// 단일 과목 자격증에선 항상 false
    let failed: Bool

    var id: String { subjectName }
}
