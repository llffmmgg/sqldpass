import Foundation

enum QuestionService {
    /// GET /api/questions/{id} — 단일 문제 상세 (정답/해설 미포함 — 책갈피 상세 시트 등에서 사용).
    /// 풀이 화면이 정답/해설까지 받으려면 `fullDetail(id:)` 사용.
    static func detail(id: Int64) async throws -> Question {
        try await APIClient.shared.get("/api/questions/\(id)")
    }

    /// GET /api/questions/{id} — 단일 문제 상세 (정답·해설 포함).
    /// 단일 채점 풀이(SoloSolveView)의 즉시 채점에 사용.
    static func fullDetail(id: Int64) async throws -> QuestionDetail {
        try await APIClient.shared.get("/api/questions/\(id)")
    }

    /// GET /api/questions?subjectId=...&size=... — 과목별 랜덤 문제 목록.
    /// Android API 와 동일하게 `size` 쿼리 사용(백엔드 `@RequestParam size`).
    static func list(subjectId: Int64, size: Int) async throws -> [Question] {
        let query: [URLQueryItem] = [
            .init(name: "subjectId", value: String(subjectId)),
            .init(name: "size", value: String(size)),
        ]
        return try await APIClient.shared.get("/api/questions", query: query)
    }
}
