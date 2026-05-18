import Foundation
import Observation

@Observable
final class BookmarksViewModel {
    private(set) var items: [Bookmark] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            items = try await BookmarkService.list()
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
