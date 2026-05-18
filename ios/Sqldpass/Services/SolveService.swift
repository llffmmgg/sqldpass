import Foundation

enum SolveService {
    /// GET /api/solves — 내 풀이 히스토리
    static func myHistory() async throws -> [Solve] {
        try await APIClient.shared.get("/api/solves")
    }

    /// GET /api/solves/{id} — 단일 풀이 상세
    static func detail(id: Int64) async throws -> Solve {
        try await APIClient.shared.get("/api/solves/\(id)")
    }

    /// GET /api/solves/stats/overall-avg — 전체 사용자 14일 평균 풀이 수
    static func overallStats() async throws -> OverallStats {
        try await APIClient.shared.get("/api/solves/stats/overall-avg")
    }

    /// POST /api/solves — 풀이 제출.
    /// `clientSubmissionId` 는 백엔드의 멱등키(@Size max=64). 같은 키로 재전송 시 중복 row 생성 X.
    /// Android 와 동등한 패턴: `"ios-\(UUID().uuidString)"`.
    struct SubmitRequest: Encodable {
        let subjectId: Int64?
        let mockExamId: Int64?
        let source: String?
        let answers: [Answer]
        let clientSubmissionId: String?

        init(
            subjectId: Int64? = nil,
            mockExamId: Int64? = nil,
            source: String? = nil,
            answers: [Answer],
            clientSubmissionId: String? = nil
        ) {
            self.subjectId = subjectId
            self.mockExamId = mockExamId
            self.source = source
            self.answers = answers
            self.clientSubmissionId = clientSubmissionId
        }

        /// Codable — SolveSyncManager 가 큐 row 의 answersJSON 을 디코드해 재전송하므로 Decodable 도 필요.
        struct Answer: Codable {
            let questionId: Int64
            let selectedOption: Int?
            let answerText: String?
        }
    }

    static func submit(_ request: SubmitRequest) async throws -> Solve {
        try await APIClient.shared.post("/api/solves", body: request)
    }
}
