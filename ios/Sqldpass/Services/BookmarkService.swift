import Foundation

enum BookmarkService {
    /// GET /api/bookmarks — 내 북마크 목록
    static func list() async throws -> [Bookmark] {
        try await APIClient.shared.get("/api/bookmarks")
    }

    /// GET /api/bookmarks/exists/{questionId} — 특정 문제 북마크 여부
    static func exists(questionId: Int64) async throws -> Bool {
        let response: BookmarkExists = try await APIClient.shared.get("/api/bookmarks/exists/\(questionId)")
        return response.exists
    }

    /// POST /api/bookmarks/{questionId} — 북마크 추가
    static func add(questionId: Int64) async throws {
        let _: Bookmark = try await APIClient.shared.post("/api/bookmarks/\(questionId)", body: EmptyBookmarkBody())
    }

    /// DELETE /api/bookmarks/{questionId} — 북마크 제거
    static func remove(questionId: Int64) async throws {
        try await APIClient.shared.delete("/api/bookmarks/\(questionId)")
    }
}

private struct EmptyBookmarkBody: Encodable {}
