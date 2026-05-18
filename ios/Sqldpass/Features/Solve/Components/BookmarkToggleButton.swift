import SwiftUI

/// 북마크 토글 아이콘 버튼 (Inked OMR 디자인 시스템).
///
/// 초기 상태는 `BookmarkService.exists` 로 동기화, 토글은 optimistic update 후 실패 시 롤백.
struct BookmarkToggleButton: View {
    let questionId: Int64

    @State private var isBookmarked: Bool = false
    @State private var isLoading: Bool = false

    var body: some View {
        Button {
            Task { await toggle() }
        } label: {
            Image(systemName: isBookmarked ? "bookmark.fill" : "bookmark")
                .font(AppType.body)
                .foregroundStyle(isBookmarked ? Color.brandPrimary : Color.appTextMuted)
                .frame(width: 40, height: 40)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isBookmarked ? "북마크 해제" : "북마크 추가")
        .disabled(isLoading)
        .task {
            // 초기 상태 동기화 — 실패해도 기본값 false
            if let exists = try? await BookmarkService.exists(questionId: questionId) {
                isBookmarked = exists
            }
        }
    }

    private func toggle() async {
        let previous = isBookmarked
        isBookmarked.toggle() // 옵티미스틱
        isLoading = true
        Haptics.light()
        do {
            if previous {
                try await BookmarkService.remove(questionId: questionId)
            } else {
                try await BookmarkService.add(questionId: questionId)
            }
        } catch {
            isBookmarked = previous // 롤백
        }
        isLoading = false
    }
}
