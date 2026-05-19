import Foundation

enum BookmarkService {
    /// GET /api/bookmarks — 내 북마크 목록.
    /// 백엔드는 wrapper(`BookmarkListResponse`)로 반환. 호출자는 `items` 만 받아도
    /// 충분하므로 wrapper 를 풀어 `[Bookmark]` 로 노출한다.
    /// 잠금/한도 정보가 필요해지면 `fullList()` 를 추가하면 됨.
    static func list() async throws -> [Bookmark] {
        let response: BookmarkListResponse = try await APIClient.shared.get("/api/bookmarks")
        return response.items
    }

    /// GET /api/bookmarks/exists/{questionId} — 특정 문제 북마크 여부.
    /// 백엔드 필드명은 `bookmarked` (boolean).
    static func exists(questionId: Int64) async throws -> Bool {
        let response: BookmarkExists = try await APIClient.shared.get("/api/bookmarks/exists/\(questionId)")
        return response.bookmarked
    }

    /// POST /api/bookmarks/{questionId} — 북마크 추가.
    /// 백엔드는 `ResponseEntity<Void>` 로 빈 본문/200 을 반환 → `postVoid` 사용.
    static func add(questionId: Int64) async throws {
        try await APIClient.shared.postVoid("/api/bookmarks/\(questionId)", body: EmptyBookmarkBody())
    }

    /// DELETE /api/bookmarks/{questionId} — 북마크 제거. 204 응답.
    static func remove(questionId: Int64) async throws {
        try await APIClient.shared.delete("/api/bookmarks/\(questionId)")
    }
}

private struct EmptyBookmarkBody: Encodable {}
