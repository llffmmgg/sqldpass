import Foundation

enum WrongAnswerService {
    /// GET /api/wrong-answers — 오답노트 목록
    static func list() async throws -> [WrongAnswer] {
        try await APIClient.shared.get("/api/wrong-answers")
    }

    /// GET /api/wrong-answers/stats — 과목별 오답률 통계
    static func stats() async throws -> [WrongAnswerStats] {
        try await APIClient.shared.get("/api/wrong-answers/stats")
    }

    /// GET /api/wrong-answers/preview — 잠금 화면용 미리보기 (구독 없이도 호출)
    static func preview() async throws -> [WrongAnswerPreview] {
        try await APIClient.shared.get("/api/wrong-answers/preview")
    }

    /// POST /api/wrong-answers/{questionId}/retry — 다시 풀기
    /// chosenOption: MCQ 선택 번호 ("1"~"4") 또는 단답형 답안 텍스트
    static func retry(questionId: Int64, chosen: String) async throws -> WrongAnswerRetryResult {
        try await APIClient.shared.post(
            "/api/wrong-answers/\(questionId)/retry",
            body: RetryRequest(chosenAnswer: chosen)
        )
    }

    private struct RetryRequest: Encodable {
        let chosenAnswer: String
    }
}
