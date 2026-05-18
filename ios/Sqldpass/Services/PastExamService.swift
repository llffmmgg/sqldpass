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
}
