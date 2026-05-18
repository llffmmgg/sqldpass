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

    /// POST /api/solves — 풀이 제출
    struct SubmitRequest: Encodable {
        let subjectId: Int64?
        let mockExamId: Int64?
        let answers: [Answer]

        struct Answer: Encodable {
            let questionId: Int64
            let selectedOption: Int?
            let answerText: String?
        }
    }

    static func submit(_ request: SubmitRequest) async throws -> Solve {
        try await APIClient.shared.post("/api/solves", body: request)
    }
}
