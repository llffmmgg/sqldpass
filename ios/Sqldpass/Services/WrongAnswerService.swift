import Foundation

enum WrongAnswerService {
    /// GET /api/wrong-answers
    static func list() async throws -> [WrongAnswer] {
        try await APIClient.shared.get("/api/wrong-answers")
    }

    /// GET /api/wrong-answers/stats
    static func stats() async throws -> [WrongAnswerStats] {
        try await APIClient.shared.get("/api/wrong-answers/stats")
    }

    /// GET /api/wrong-answers/preview
    static func preview() async throws -> [WrongAnswerPreview] {
        try await APIClient.shared.get("/api/wrong-answers/preview")
    }

    /// POST /api/wrong-answers/{questionId}/retry
    static func retry(
        questionId: Int64,
        selectedOption: Int?,
        answerText: String?
    ) async throws -> WrongAnswerRetryResult {
        try await APIClient.shared.post(
            "/api/wrong-answers/\(questionId)/retry",
            body: RetryRequest(
                selectedOption: selectedOption,
                answerText: answerText?.trimmedForSubmission.nilIfEmpty
            )
        )
    }

    private struct RetryRequest: Encodable {
        let selectedOption: Int?
        let answerText: String?
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
