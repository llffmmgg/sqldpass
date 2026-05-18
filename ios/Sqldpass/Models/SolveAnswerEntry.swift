import Foundation

/// SolveViewModel 메모리 전용 답안 보관 구조체.
/// 제출 시 SolveService.SubmitRequest.Answer 로 매핑.
struct SolveAnswerEntry: Equatable {
    let questionId: Int64
    var selectedOption: Int?
    var answerText: String?
    var markedForReview: Bool

    init(
        questionId: Int64,
        selectedOption: Int? = nil,
        answerText: String? = nil,
        markedForReview: Bool = false
    ) {
        self.questionId = questionId
        self.selectedOption = selectedOption
        self.answerText = answerText
        self.markedForReview = markedForReview
    }

    var isAnswered: Bool {
        selectedOption != nil || !(answerText?.trimmedForSubmission.isEmpty ?? true)
    }

    var toSubmitAnswer: SolveService.SubmitRequest.Answer {
        .init(
            questionId: questionId,
            selectedOption: selectedOption,
            answerText: answerText?.trimmedForSubmission.nilIfEmpty
        )
    }
}

private extension String {
    var trimmedForSubmission: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
