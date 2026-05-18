import Foundation

enum ExamService {
    /// GET /api/mock-exams — 모의고사 + 기출 통합 목록
    static func list() async throws -> [MockExamSummary] {
        try await APIClient.shared.get("/api/mock-exams")
    }

    /// GET /api/mock-exams/{id} — 상세 (문제 목록 포함)
    static func detail(id: Int64) async throws -> MockExamDetail {
        try await APIClient.shared.get("/api/mock-exams/\(id)")
    }
}
