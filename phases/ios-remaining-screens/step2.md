# Step 2 — 풀이 히스토리 화면 (HistoryView)

## Background

사용자가 푼 모든 풀이(`/api/solves`) 를 리스트로 보고, 탭하면 기존 `SolveResultView` 를 재사용해 상세를 본다. 진입은 ProfileView 메뉴에서 (Step 6 에서 wiring).

## Workdir

```bash
ios/
```

## Dependencies

- Step 1: 새 모델 없음 — 기존 `Models/Solve.swift` 의 `Solve, SolveAnswer` 그대로 사용
- 기존: `Services/SolveService.myHistory()` (이미 존재)
- 기존: `Features/Solve/SolveResultView.swift` (재사용)
- 기존: `Models/MockExamDetail.swift` 의 `MockExamQuestionItem` — SolveResultView 가 요구하는 questions 인자에 빈 배열로 임시 전달 가능

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/History/HistoryView.swift` | 신규 — 메인 화면 |
| `ios/Sqldpass/Features/History/HistoryViewModel.swift` | 신규 — @Observable, 로드 + 정렬 |

## Implementation

### `HistoryViewModel.swift`

```swift
import Foundation
import Observation

@Observable
final class HistoryViewModel {
    private(set) var solves: [Solve] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let raw = try await SolveService.myHistory()
            // 최신순 정렬
            solves = raw.sorted { ($0.solvedAt) > ($1.solvedAt) }
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
```

### `HistoryView.swift`

```swift
import SwiftUI

struct HistoryView: View {
    @State private var viewModel = HistoryViewModel()

    var body: some View {
        content
            .background(Color.appPage)
            .navigationTitle("학습 기록")
            .navigationBarTitleDisplayMode(.large)
            .refreshable {
                await viewModel.load()
            }
            .task {
                if viewModel.solves.isEmpty {
                    await viewModel.load()
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.solves.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.solves.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
            }
        } else if viewModel.solves.isEmpty {
            ContentUnavailableView(
                "아직 푼 기록이 없어요",
                systemImage: "list.bullet.rectangle.portrait",
                description: Text("모의고사를 풀면 기록이 여기 쌓입니다")
            )
        } else {
            ScrollView {
                LazyVStack(spacing: Spacing.md) {
                    ForEach(viewModel.solves) { solve in
                        NavigationLink {
                            SolveResultView(
                                result: solve,
                                questions: [],
                                onDone: {}
                            )
                        } label: {
                            HistoryCard(solve: solve)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(Spacing.base)
            }
        }
    }
}

private struct HistoryCard: View {
    let solve: Solve

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(alignment: .firstTextBaseline) {
                Text(scoreLabel)
                    .font(AppType.monoNumericLarge)
                    .foregroundStyle(scoreColor)
                Text("점")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextMuted)
                Spacer()
                Text(formattedDate)
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextSubtle)
            }
            HStack(spacing: Spacing.sm) {
                Label("\(solve.correctCount) / \(solve.totalCount)", systemImage: "checkmark.circle")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)
                if let streak = solve.currentStreak, streak > 0 {
                    Label("\(streak)일 연속", systemImage: "flame")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.semanticWarning)
                }
                Spacer()
            }
        }
        .padding(Spacing.base)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var scoreLabel: String { "\(solve.score)" }

    private var scoreColor: Color {
        if solve.score >= 80 { return .brandPrimary }
        if solve.score >= 60 { return .semanticInfo }
        return .semanticDanger
    }

    /// ISO 8601 문자열 → "MM월 dd일 HH:mm" 형식. 실패 시 원본.
    private var formattedDate: String {
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let date = parser.date(from: solve.solvedAt)
            ?? ISO8601DateFormatter().date(from: solve.solvedAt)
        guard let date else { return solve.solvedAt }
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "MM월 dd일 HH:mm"
        return f.string(from: date)
    }
}

#Preview {
    NavigationStack {
        HistoryView()
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

진입점은 Step 6 에서 ProfileView 메뉴 NavigationLink 로 연결. 본 step 만으로 화면 접근 불가하나 빌드 통과로 충분.

## 금지사항

- `Solve` 가 `Identifiable` (id: Int64) 라 `ForEach` 에서 `id:` 명시 불필요. 이미 정의돼있다(`Models/Solve.swift`).
- 새 `Solve` 모델 확장하지 마라. 본 step 은 백엔드 응답을 그대로 표시할 뿐.
- `SolveResultView` 에 `questions: [MockExamQuestionItem]` 가 비어있어도 동작해야 한다. 실제로 fallback("(문제 정보 누락)") 처리가 이미 들어있음. 본 step 에서는 빈 배열 전달.
- 진입점(ProfileView 연결) 을 본 step 에 넣지 마라. Step 6 의 책임.
