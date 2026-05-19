import Foundation

/// 백엔드 응답: GET /api/bookmarks 의 items 원소 (BookmarkResponse).
///
/// 백엔드 record 시그니처:
/// `{ questionId, questionContent, questionType, subjectId, subjectName, createdAt }`
/// — 별도의 row `id` 필드는 없다. Identifiable 의 id 는 `questionId` 로 매핑.
struct Bookmark: Codable, Equatable, Identifiable {
    let questionId: Int64
    let questionContent: String
    let questionType: String
    let subjectId: Int64
    let subjectName: String
    let createdAt: String

    var id: Int64 { questionId }
}

/// 백엔드 응답: GET /api/bookmarks
///
/// 프리미엄 미가입자에게는 30 개만 채워서 반환하고 `limited=true` 로 표시.
/// 본 step 의 UI 는 `items` 만 사용하지만 차후 잠금 안내 / 프리미엄 유도에서 사용 가능하도록 보존.
struct BookmarkListResponse: Codable, Equatable {
    let items: [Bookmark]
    let totalCount: Int64
    let limited: Bool
    let freeLimit: Int
}

/// 백엔드 응답: GET /api/bookmarks/exists/{questionId} — `{ bookmarked: bool }`.
struct BookmarkExists: Codable, Equatable {
    let bookmarked: Bool
}
