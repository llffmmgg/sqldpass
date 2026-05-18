import Foundation

/// 백엔드 응답: GET /api/bookmarks
struct Bookmark: Codable, Equatable, Identifiable {
    let id: Int64
    let questionId: Int64
    let createdAt: String
}

struct BookmarkExists: Codable, Equatable {
    let exists: Bool
}
