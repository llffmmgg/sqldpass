import SwiftUI

struct BookmarksView: View {
    @State private var viewModel = BookmarksViewModel()
    @State private var selectedBookmark: Bookmark?

    var body: some View {
        content
            .background(Color.appPage)
            .navigationTitle("북마크")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.items.isEmpty {
                    await viewModel.load()
                }
            }
            .sheet(item: $selectedBookmark) { bookmark in
                BookmarkDetailSheet(questionId: bookmark.questionId)
                    .presentationDetents([.medium, .large])
            }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.items.isEmpty {
            ProgressView().controlSize(.large).frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.items.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
            }
        } else if viewModel.items.isEmpty {
            ContentUnavailableView(
                "북마크가 없어요",
                systemImage: "bookmark",
                description: Text("풀이 중에 ⭐︎ 버튼으로 북마크할 수 있어요")
            )
        } else {
            List {
                ForEach(viewModel.items) { bookmark in
                    Button {
                        selectedBookmark = bookmark
                    } label: {
                        HStack(alignment: .top, spacing: Spacing.md) {
                            Image(systemName: "bookmark.fill")
                                .foregroundStyle(Color.brandPrimary)
                                .padding(.top, 2)
                            VStack(alignment: .leading, spacing: Spacing.xxs) {
                                Text(bookmark.subjectName)
                                    .font(AppType.caption.weight(.semibold))
                                    .foregroundStyle(Color.brandPrimary)
                                Text(bookmark.questionContent)
                                    .font(AppType.bodyEmph)
                                    .foregroundStyle(Color.appTextPrimary)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)
                                Text(formatDate(bookmark.createdAt))
                                    .font(AppType.footnote)
                                    .foregroundStyle(Color.appTextSubtle)
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "chevron.right")
                                .font(.footnote)
                                .foregroundStyle(Color.appTextSubtle)
                                .padding(.top, 4)
                        }
                    }
                    .buttonStyle(.plain)
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        Button(role: .destructive) {
                            Task { await viewModel.remove(bookmark) }
                        } label: {
                            Label("제거", systemImage: "trash")
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
        }
    }

    private func formatDate(_ raw: String) -> String {
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = parser.date(from: raw) ?? ISO8601DateFormatter().date(from: raw)
        guard let date else { return raw }
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "yyyy.MM.dd"
        return f.string(from: date)
    }
}
