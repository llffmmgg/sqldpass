# Step 3 — 오답 노트 화면 (WrongAnswersView 재작성)

## Background

기존 `Features/WrongAnswers/WrongAnswersView.swift` 는 placeholder(ContentUnavailableView)만 있다. Step 1 에서 추가한 `WrongAnswerService` + 정정된 `WrongAnswer/WrongAnswerStats` 모델로 실데이터 화면을 만든다.

화면 구성:
- **상단**: 과목별 오답률 통계 카드 가로 스크롤 (`WrongAnswerStats`)
- **본문**: 오답 문항 리스트 (`WrongAnswer`) — 카드에 `wrongCount` 배지
- **탭 시**: retry 시트 띄움 — 4지선다 선택 → `POST /api/wrong-answers/{id}/retry` → 결과 표시(정/오답 + 해설)

## Workdir

```bash
ios/
```

## Dependencies

- Step 1: `WrongAnswerService`, `WrongAnswer`, `WrongAnswerStats`, `WrongAnswerRetryResult`

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/WrongAnswers/WrongAnswersView.swift` | 재작성 — 실데이터 표시 |
| `ios/Sqldpass/Features/WrongAnswers/WrongAnswersViewModel.swift` | 신규 — list/stats 로딩 |
| `ios/Sqldpass/Features/WrongAnswers/WrongAnswerRetrySheet.swift` | 신규 — retry 시트 |

## Implementation

### `WrongAnswersViewModel.swift`

```swift
import Foundation
import Observation

@Observable
final class WrongAnswersViewModel {
    private(set) var items: [WrongAnswer] = []
    private(set) var stats: [WrongAnswerStats] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let itemsTask = WrongAnswerService.list()
            async let statsTask = WrongAnswerService.stats()
            let (i, s) = try await (itemsTask, statsTask)
            items = i
            stats = s
            errorMessage = nil
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// retry 성공 시 해당 문항을 목록에서 제거 (백엔드 정책: 정답이면 자동 마스터)
    func markMastered(questionId: Int64) {
        items.removeAll { $0.questionId == questionId }
    }
}
```

### `WrongAnswersView.swift` (재작성)

기존 placeholder ContentUnavailableView 만 있는 파일을 통째로 교체.

```swift
import SwiftUI

struct WrongAnswersView: View {
    @State private var viewModel = WrongAnswersViewModel()
    @State private var retryTarget: WrongAnswer?

    var body: some View {
        NavigationStack {
            content
                .background(Color.appPage)
                .navigationTitle("오답")
                .navigationBarTitleDisplayMode(.large)
                .refreshable {
                    await viewModel.load()
                }
                .task {
                    if viewModel.items.isEmpty {
                        await viewModel.load()
                    }
                }
                .sheet(item: $retryTarget) { target in
                    WrongAnswerRetrySheet(
                        item: target,
                        onMastered: {
                            viewModel.markMastered(questionId: target.questionId)
                            retryTarget = nil
                        }
                    )
                    .presentationDetents([.medium, .large])
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.items.isEmpty && viewModel.stats.isEmpty {
            ProgressView().controlSize(.large).frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.items.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") { Task { await viewModel.load() } }
            }
        } else if viewModel.items.isEmpty && viewModel.stats.isEmpty {
            ContentUnavailableView(
                "오답이 없어요",
                systemImage: "checkmark.seal",
                description: Text("틀린 문제가 생기면 여기서 다시 풀 수 있어요")
            )
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    if !viewModel.stats.isEmpty {
                        statsSection
                    }
                    if !viewModel.items.isEmpty {
                        itemsSection
                    }
                }
                .padding(Spacing.base)
            }
        }
    }

    private var statsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("과목별 오답률")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.md) {
                    ForEach(viewModel.stats) { stat in
                        StatPill(stat: stat)
                    }
                }
            }
        }
    }

    private var itemsSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("오답 문항 \(viewModel.items.count)개")
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
            ForEach(viewModel.items) { item in
                Button {
                    retryTarget = item
                } label: {
                    WrongAnswerCard(item: item)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct StatPill: View {
    let stat: WrongAnswerStats

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(stat.subjectName)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.appTextPrimary)
            Text("\(stat.wrongRate)%")
                .font(AppType.title.weight(.bold))
                .foregroundStyle(rateColor)
            Text("\(stat.wrongCount) / \(stat.totalSolved)")
                .font(AppType.caption)
                .foregroundStyle(Color.appTextMuted)
        }
        .padding(Spacing.md)
        .frame(width: 120, alignment: .leading)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }

    private var rateColor: Color {
        if stat.wrongRate >= 50 { return .semanticDanger }
        if stat.wrongRate >= 25 { return .semanticWarning }
        return .brandPrimary
    }
}

private struct WrongAnswerCard: View {
    let item: WrongAnswer

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.md) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(item.subjectName)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(Color.brandPrimary)
                Text(item.questionContent)
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
            }
            Spacer()
            VStack(spacing: Spacing.xxs) {
                Text("\(item.wrongCount)")
                    .font(AppType.monoNumericLarge.weight(.bold))
                    .foregroundStyle(Color.semanticDanger)
                Text("회 틀림")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
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
}

#Preview {
    WrongAnswersView()
}
```

### `WrongAnswerRetrySheet.swift`

```swift
import SwiftUI

struct WrongAnswerRetrySheet: View {
    let item: WrongAnswer
    let onMastered: () -> Void

    @State private var chosen: String?
    @State private var result: WrongAnswerRetryResult?
    @State private var isSubmitting = false
    @State private var errorMessage: String?

    @Environment(\.dismiss) private var dismiss

    private let choices = ["1", "2", "3", "4"]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    Text(item.subjectName)
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.brandPrimary)
                    Text(item.questionContent)
                        .font(AppType.body)
                        .foregroundStyle(Color.appTextPrimary)
                        .fixedSize(horizontal: false, vertical: true)

                    if let result {
                        resultBlock(result)
                    } else {
                        choiceBlock
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(AppType.footnote)
                            .foregroundStyle(Color.semanticDanger)
                    }
                }
                .padding(Spacing.base)
            }
            .navigationTitle("다시 풀기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") {
                        if result?.correct == true {
                            onMastered()
                        } else {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private var choiceBlock: some View {
        VStack(spacing: Spacing.sm) {
            ForEach(choices, id: \.self) { value in
                Button {
                    chosen = value
                } label: {
                    HStack {
                        ZStack {
                            Circle()
                                .stroke(chosen == value ? Color.brandPrimary : Color.appBorder,
                                        lineWidth: chosen == value ? 2 : 1)
                                .frame(width: 28, height: 28)
                            Text(value)
                                .font(AppType.bodyEmph)
                                .foregroundStyle(chosen == value ? Color.brandPrimary : Color.appTextPrimary)
                        }
                        Text("\(value)번")
                            .font(AppType.body)
                        Spacer()
                    }
                    .padding(Spacing.md)
                    .background(chosen == value ? Color.brandPrimary.opacity(0.1) : Color.appSurface)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.md)
                            .stroke(chosen == value ? Color.brandPrimary : Color.appBorder, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                }
                .buttonStyle(.plain)
            }

            Button {
                Task { await submit() }
            } label: {
                if isSubmitting {
                    ProgressView().frame(maxWidth: .infinity).frame(height: 48)
                } else {
                    Text("제출")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.brandPrimary)
            .disabled(chosen == nil || isSubmitting)
        }
    }

    private func resultBlock(_ r: WrongAnswerRetryResult) -> some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                Image(systemName: r.correct ? "checkmark.circle.fill" : "xmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(r.correct ? Color.semanticSuccess : Color.semanticDanger)
                Text(r.correct ? "정답입니다!" : "틀렸어요")
                    .font(AppType.title.weight(.bold))
                    .foregroundStyle(Color.appTextPrimary)
            }
            if let option = r.correctOption {
                Text("정답: \(option)번")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
            } else if let answer = r.correctAnswer {
                Text("정답: \(answer)")
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextPrimary)
            }
            if let explanation = r.explanation, !explanation.isEmpty {
                Divider()
                Text("해설")
                    .font(AppType.bodyEmph)
                Text(explanation)
                    .font(AppType.body)
                    .foregroundStyle(Color.appTextMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if r.correct {
                Text("이 문제는 오답노트에서 제거됩니다.")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextSubtle)
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

    private func submit() async {
        guard let chosen else { return }
        isSubmitting = true
        errorMessage = nil
        do {
            result = try await WrongAnswerService.retry(questionId: item.questionId, chosen: chosen)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        isSubmitting = false
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

- `WrongAnswerRetrySheet` 안에 풀이 본문 외 추가 콘텐츠(이미지 등) 표시 금지. 이유: `WrongAnswer` DTO 는 본문 텍스트만 가짐.
- retry 시트에서 정답일 때 자동 닫기 금지. 이유: 사용자가 해설을 충분히 읽을 시간 필요. "닫기" 명시적 액션으로 마무리.
- `Sheet` 의 `.presentationDetents([.medium, .large])` 외에 `.fraction()` 같은 임의 비율 사용 금지. 이유: iOS 표준 detent 가 사용자 익숙 + 접근성 호환.
