import Foundation

/// 자격증·과목 트리 조회 API. `/api/subjects` 는 로그인 필수.
///
/// 백엔드 응답은 트리 구조: 루트 노드 = 자격증, `children` = 해당 자격증 과목 목록.
/// 호출자(`SoloHubViewModel`) 가 트리를 그대로 받아 칩(루트) + 과목 카드(children) 로 렌더.
enum SubjectService {
    /// GET /api/subjects — 자격증 루트 + 과목 children 트리.
    static func all() async throws -> [SubjectResponse] {
        try await APIClient.shared.get("/api/subjects")
    }
}
