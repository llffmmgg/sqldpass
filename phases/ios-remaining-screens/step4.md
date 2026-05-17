# Step 4 — 북마크 화면 (BookmarksView)

## Background

ProfileView 의 "북마크" 메뉴에서 진입하는 화면. 사용자가 풀이 중 북마크한 문제들의 리스트. 탭하면 문제 본문 시트로 미리보기.

데이터 모델 한계:
- `Bookmark` DTO 는 `questionId, createdAt` 만 — 문제 본문은 별도 fetch 필요(`QuestionService.detail(id:)`)
- 리스트 표시는 `questionId` + `createdAt` 만으로 (가벼운 리스트)
- 상세 시트에서 본문 lazy fetch

## Workdir

```bash
ios/
```

## Dependencies

- 기존 `Services/BookmarkService` (Step 1 이전 phase 에서 추가됨)
- 기존 `Services/QuestionService.detail(id:)`
- 기존 `Models/Bookmark.swift`, `Models/Question.swift`

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Bookmarks/BookmarksView.swift` | 신규 |
| `ios/Sqldpass/Features/Bookmarks/BookmarksViewModel.swift` | 신규 |
| `ios/Sqldpass/Features/Bookmarks/BookmarkDetailSheet.swift` | 신규 — 본문 lazy fetch |

## Implementation

### `BookmarksViewModel.swift`

```swift
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
```

### `BookmarksView.swift`

```swift
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
                        HStack(spacing: Spacing.md) {
                            Image(systemName: "bookmark.fill")
                                .foregroundStyle(Color.brandPrimary)
                            VStack(alignment: .leading, spacing: Spacing.xxs) {
                                Text("문제 #\(bookmark.questionId)")
                                    .font(AppType.bodyEmph)
                                    .foregroundStyle(Color.appTextPrimary)
                                Text(formatDate(bookmark.createdAt))
                                    .font(AppType.footnote)
                                    .foregroundStyle(Color.appTextSubtle)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.footnote)
                                .foregroundStyle(Color.appTextSubtle)
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
```

### `BookmarkDetailSheet.swift`

```swift
import SwiftUI

struct BookmarkDetailSheet: View {
    let questionId: Int64

    @State private var question: Question?
    @State private var isLoading = true
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.md) {
                    if isLoading {
                        ProgressView().controlSize(.large).frame(maxWidth: .infinity, minHeight: 200)
                    } else if let errorMessage {
                        Text(errorMessage)
                            .font(AppType.body)
                            .foregroundStyle(Color.semanticDanger)
                    } else if let question {
                        Text("문제 #\(question.id)")
                            .font(AppType.caption.weight(.semibold))
                            .foregroundStyle(Color.brandPrimary)
                        Text(question.content)
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                            .textSelection(.enabled)
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)
            .navigationTitle("북마크 미리보기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
            .task { await load() }
        }
    }

    private func load() async {
        isLoading = true
        do {
            question = try await QuestionService.detail(id: questionId)
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
```

## Validation

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대: `** BUILD SUCCEEDED **`

## 금지사항

- 북마크 리스트에서 문제 본문 prefetch 하지 마라. 이유: 북마크 100개 있으면 100번 fetch — 시트에서 lazy.
- 시트에서 풀이 모드 진입 금지(`SolveView` 푸시 등). 이유: 북마크는 미리보기만. 풀이는 모의고사/오답에서.
- swipe 액션 외 다른 삭제 UI 추가 금지. 이유: iOS 표준 = swipe to delete. 별도 "편집" 모드 도입은 과함.
