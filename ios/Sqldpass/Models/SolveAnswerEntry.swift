import Foundation

/// SolveViewModel 메모리 전용 답안 보관 구조체.
/// 제출 시 SolveService.SubmitRequest.Answer 로 매핑.
struct SolveAnswerEntry: Equatable {
    let questionId: Int64
    var chosenAnswer: String?
    var markedForReview: Bool

    init(questionId: Int64, chosenAnswer: String? = nil, markedForReview: Bool = false) {
        self.questionId = questionId
        self.chosenAnswer = chosenAnswer
        self.markedForReview = markedForReview
    }

    var toSubmitAnswer: SolveService.SubmitRequest.Answer {
        .init(questionId: questionId, chosenAnswer: chosenAnswer)
    }
}
