import Foundation

/// 기출복원 공개 API. 인증 없이 접근 가능 — 백엔드 `PastExamPublicController`.
///
/// Android 미러: `AppRepository.pastExams(certSlug)` / `SqldpassApi.getPastExams`.
enum PastExamService {
    /// GET /api/public/past-exams?cert={slug} — 자격증 slug 별 기출 회차 목록.
    /// slug 가 nil 이면 전체. 본 step 의 PastExamsListView 는 항상 slug 지정.
    static func list(slug: String?) async throws -> [PastExamSummary] {
        var query: [URLQueryItem] = []
        if let slug, !slug.isEmpty {
            query.append(.init(name: "cert", value: slug))
        }
        return try await APIClient.shared.get("/api/public/past-exams", query: query)
    }

    /// GET /api/public/past-exams/{id} — 회차 상세 (정답/해설 미포함).
    /// 로그인 인터셉터는 `optionalMemberAuthInterceptor` — 토큰 있으면 회차 best score 반영, 없어도 200.
    static func detail(id: Int64) async throws -> PastExamDetailResponse {
        try await APIClient.shared.get("/api/public/past-exams/\(id)")
    }

    /// POST /api/public/past-exams/{id}/grade — 채점 + (로그인 시) solve 기록.
    struct GradeRequest: Encodable {
        let answers: [Answer]
        struct Answer: Encodable {
            let questionId: Int64
            let selectedOption: Int?
            let answerText: String?
        }
    }

    static func grade(id: Int64, answers: [GradeRequest.Answer]) async throws -> PastExamGradeResponse {
        try await APIClient.shared.post(
            "/api/public/past-exams/\(id)/grade",
            body: GradeRequest(answers: answers)
        )
    }
}
