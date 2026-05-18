import Foundation

enum QuestionService {
    /// GET /api/questions/{id} — 단일 문제 상세
    static func detail(id: Int64) async throws -> Question {
        try await APIClient.shared.get("/api/questions/\(id)")
    }

    /// GET /api/questions?subjectId=...&limit=... — 과목별 문제 목록 (옵션)
    static func list(subjectId: Int64? = nil, limit: Int? = nil) async throws -> [Question] {
        var query: [URLQueryItem] = []
        if let subjectId { query.append(.init(name: "subjectId", value: String(subjectId))) }
        if let limit { query.append(.init(name: "limit", value: String(limit))) }
        return try await APIClient.shared.get("/api/questions", query: query)
    }
}
