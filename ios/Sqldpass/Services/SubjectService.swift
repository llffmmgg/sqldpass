import Foundation

/// 자격증·과목 트리 조회 API. `/api/subjects` 는 로그인 필수.
///
/// Android 미러: `AppRepository.subjects()` / `SqldpassApi.getSubjects()`.
enum SubjectService {
    /// GET /api/subjects — parent(자격증) + children(과목) 단일 평면 리스트.
    /// 호출자가 `parentName` 으로 grouping 하여 자격증 칩 + 과목 카드 UI 를 구성한다.
    static func all() async throws -> [SubjectResponse] {
        try await APIClient.shared.get("/api/subjects")
    }
}
