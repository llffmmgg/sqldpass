import Foundation
import Observation

@Observable
final class BookmarksViewModel {
    /// 최신 30개까지만 표시 — 백엔드가 더 많이 내려와도 클라이언트에서 가드.
    static let displayLimit = 30

    private(set) var items: [Bookmark] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let all = try await BookmarkService.list()
            items = Array(all.prefix(Self.displayLimit))
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// 단일 북마크 제거 (옵티미스틱 + 실패 시 롤백)
    func remove(_ bookmark: Bookmark) async {
        let backup = items
        items.removeAll { $0.id == bookmark.id }
        do {
            try await BookmarkService.remove(questionId: bookmark.questionId)
        } catch {
            items = backup
        }
    }
}
