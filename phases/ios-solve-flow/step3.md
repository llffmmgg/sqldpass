# Step 3 — SolveView UI (헤더/문항/OMR/액션)

## Background

Step 2 의 `SolveViewModel` 위에 SwiftUI 화면을 얹는다. 디자인 토큰은 웹과 통일하되 iOS 관습을 최대한 살린다:

- **NavigationStack 안의 push 화면**, Large Title 사용 안 함 (시험 중 헤더는 진행률·타이머 정보 우선)
- **하단 고정 액션 바** — 이전/다음/종료 버튼
- **OMR 답안** — 가로 그리드, 선택 시 `sensoryFeedback` 햅틱
- **다시 보기 표시** 토글, **북마크** 토글 — 네비게이션 바 trailing
- **종료 확인 시트** — `confirmationDialog` 시스템 다이얼로그

## Workdir

```bash
ios/
```

## Dependencies

- Step 2: `Features/Solve/SolveViewModel.swift`
- 기존:
  - `Features/MockExams/MockExamDetailView.swift` (시험 시작 진입점은 Step 5 에서 연결)
  - `Core/DesignSystem/*` (Color, Spacing, Radius, Typography)
  - `Services/BookmarkService` (Step 1)
- 향후: Step 5 의 SolveResultView 푸시 navigation

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Solve/SolveView.swift` | 신규 — 메인 화면 |
| `ios/Sqldpass/Features/Solve/Components/SolveHeader.swift` | 신규 — 진행률 + 타이머 + 답 수 |
| `ios/Sqldpass/Features/Solve/Components/QuestionBody.swift` | 신규 — 문제 본문 + 과목 라벨 |
| `ios/Sqldpass/Features/Solve/Components/OMRAnswerGrid.swift` | 신규 — 선택지 그리드 + 햅틱 |
| `ios/Sqldpass/Features/Solve/Components/SolveActionBar.swift` | 신규 — 하단 이전/다음/제출 |
| `ios/Sqldpass/Features/Solve/Components/BookmarkToggleButton.swift` | 신규 — 북마크 비동기 토글 + 옵티미스틱 |

## Implementation

### `SolveView.swift`

```swift
import SwiftUI

struct SolveView: View {
    @State var viewModel: SolveViewModel
    @State private var showExitConfirm = false
    @Environment(\.dismiss) private var dismiss

    /// 제출 성공 후 결과 화면 푸시 트리거 (Step 5 에서 wiring)
    var onSubmitted: ((Solve) -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            SolveHeader(
                progress: viewModel.progress,
                currentIndex: viewModel.currentIndex,
                totalCount: viewModel.totalCount,
                answeredCount: viewModel.answeredCount,
                elapsedSeconds: viewModel.elapsedSeconds
            )
            .padding(.horizontal, Spacing.base)
            .padding(.vertical, Spacing.sm)
            .background(Color.appSurface)
            .overlay(alignment: .bottom) {
                Rectangle()
                    .fill(Color.appBorder)
                    .frame(height: 1)
            }

            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    if let question = viewModel.currentQuestion {
                        QuestionBody(question: question)
                        OMRAnswerGrid(
                            question: question,
                            chosen: viewModel.currentEntry?.chosenAnswer,
                            onSelect: { viewModel.select($0) }
                        )
                        if viewModel.currentEntry?.chosenAnswer != nil {
                            Button {
                                viewModel.clearAnswer()
                            } label: {
                                Label("선택 지우기", systemImage: "arrow.uturn.backward")
                                    .font(AppType.footnote)
                            }
                            .buttonStyle(.borderless)
                            .foregroundStyle(Color.appTextMuted)
                        }
                    } else {
                        Text("문제가 없습니다")
                            .foregroundStyle(Color.appTextMuted)
                    }
                }
                .padding(Spacing.base)
            }
            .background(Color.appPage)

            SolveActionBar(
                canGoPrevious: viewModel.currentIndex > 0,
                canGoNext: viewModel.currentIndex < viewModel.totalCount - 1,
                isLastQuestion: viewModel.currentIndex == viewModel.totalCount - 1,
                isSubmitting: viewModel.isSubmitting,
                onPrevious: { viewModel.goPrevious() },
                onNext: { viewModel.goNext() },
                onSubmit: {
                    Task {
                        await viewModel.submit()
                        if let result = viewModel.submittedResult {
                            onSubmitted?(result)
                        }
                    }
                }
            )
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("종료") { showExitConfirm = true }
                    .foregroundStyle(Color.semanticDanger)
            }
            ToolbarItem(placement: .topBarTrailing) {
                if let q = viewModel.currentQuestion {
                    HStack(spacing: Spacing.sm) {
                        BookmarkToggleButton(questionId: q.id)
                        Button {
                            viewModel.toggleMark()
                        } label: {
                            Image(systemName: viewModel.currentEntry?.markedForReview == true
                                  ? "flag.fill" : "flag")
                                .foregroundStyle(viewModel.currentEntry?.markedForReview == true
                                                 ? Color.semanticWarning : Color.appTextSubtle)
                        }
                        .accessibilityLabel("다시 보기 표시 토글")
                    }
                }
            }
        }
        .confirmationDialog(
            "정말 종료할까요?",
            isPresented: $showExitConfirm,
            titleVisibility: .visible
        ) {
            Button("종료하기", role: .destructive) {
                viewModel.stopTimer()
                dismiss()
            }
            Button("계속 풀기", role: .cancel) {}
        } message: {
            Text("지금까지 푼 답안은 저장되지 않습니다.")
        }
        .alert("제출 실패", isPresented: .constant(viewModel.errorMessage != nil), actions: {
            Button("확인", role: .cancel) { /* viewModel.errorMessage clearing handled implicitly on next submit */ }
        }, message: {
            Text(viewModel.errorMessage ?? "")
        })
        .onAppear {
            viewModel.start()
        }
    }
}
```

### `Components/SolveHeader.swift`

```swift
import SwiftUI

struct SolveHeader: View {
    let progress: Double
    let currentIndex: Int
    let totalCount: Int
    let answeredCount: Int
    let elapsedSeconds: Int

    var body: some View {
        VStack(spacing: Spacing.xs) {
            HStack {
                Text("\(currentIndex + 1) / \(totalCount)")
                    .font(AppType.bodyEmph)
                    .foregroundStyle(Color.appTextPrimary)
                Spacer()
                Text(formattedTime)
                    .font(AppType.monoNumeric.weight(.semibold))
                    .foregroundStyle(Color.brandPrimary)
            }
            ProgressView(value: progress)
                .tint(Color.brandPrimary)
            HStack {
                Text("답안 \(answeredCount) / \(totalCount)")
                    .font(AppType.caption)
                    .foregroundStyle(Color.appTextMuted)
                Spacer()
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("진행 \(currentIndex + 1)번째 문제 중 \(totalCount)번째, 경과 \(elapsedSeconds)초, 답안 \(answeredCount)개 작성")
    }

    private var formattedTime: String {
        let m = elapsedSeconds / 60
        let s = elapsedSeconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}
```

### `Components/QuestionBody.swift`

```swift
import SwiftUI

struct QuestionBody: View {
    let question: MockExamQuestionItem

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text(question.subjectName)
                .font(AppType.caption.weight(.semibold))
                .foregroundStyle(Color.brandPrimary)
            Text("문제 \(question.displayOrder)")
                .font(AppType.footnote)
                .foregroundStyle(Color.appTextMuted)
            Text(question.content)
                .font(AppType.body)
                .foregroundStyle(Color.appTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Spacing.base)
        .background(Color.appSurface)
        .overlay(
            RoundedRectangle(cornerRadius: Radius.lg)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.lg))
    }
}
```

### `Components/OMRAnswerGrid.swift`

```swift
import SwiftUI

struct OMRAnswerGrid: View {
    let question: MockExamQuestionItem
    let chosen: String?
    let onSelect: (String) -> Void

    /// 객관식 선택지 — 4지선다 가정. 단답형 등은 향후 확장.
    private let choices = ["1", "2", "3", "4"]

    @State private var feedbackTrigger: Int = 0

    var body: some View {
        VStack(spacing: Spacing.sm) {
            ForEach(choices, id: \.self) { value in
                Button {
                    feedbackTrigger &+= 1
                    onSelect(value)
                } label: {
                    HStack(spacing: Spacing.md) {
                        ZStack {
                            Circle()
                                .stroke(
                                    chosen == value ? Color.brandPrimary : Color.appBorder,
                                    lineWidth: chosen == value ? 2 : 1
                                )
                                .frame(width: 32, height: 32)
                            Text(value)
                                .font(AppType.bodyEmph)
                                .foregroundStyle(
                                    chosen == value ? Color.brandPrimary : Color.appTextPrimary
                                )
                        }
                        Text("\(value)번")
                            .font(AppType.body)
                            .foregroundStyle(Color.appTextPrimary)
                        Spacer()
                    }
                    .padding(Spacing.md)
                    .background(
                        chosen == value ? Color.brandPrimary.opacity(0.1) : Color.appSurface
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.md)
                            .stroke(
                                chosen == value ? Color.brandPrimary : Color.appBorder,
                                lineWidth: 1
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: Radius.md))
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(value)번 선택")
                .accessibilityAddTraits(chosen == value ? .isSelected : [])
            }
        }
        .sensoryFeedback(.selection, trigger: feedbackTrigger)
    }
}
```

### `Components/SolveActionBar.swift`

```swift
import SwiftUI

struct SolveActionBar: View {
    let canGoPrevious: Bool
    let canGoNext: Bool
    let isLastQuestion: Bool
    let isSubmitting: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        HStack(spacing: Spacing.md) {
            Button(action: onPrevious) {
                Label("이전", systemImage: "chevron.left")
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
            }
            .buttonStyle(.bordered)
            .disabled(!canGoPrevious)

            if isLastQuestion {
                Button(action: onSubmit) {
                    if isSubmitting {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                    } else {
                        Text("제출하기")
                            .font(AppType.bodyEmph)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .disabled(isSubmitting)
            } else {
                Button(action: onNext) {
                    Label("다음", systemImage: "chevron.right")
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .disabled(!canGoNext)
            }
        }
        .padding(.horizontal, Spacing.base)
        .padding(.vertical, Spacing.sm)
        .background(Color.appSurface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color.appBorder)
                .frame(height: 1)
        }
    }
}
```

### `Components/BookmarkToggleButton.swift`

```swift
import SwiftUI

struct BookmarkToggleButton: View {
    let questionId: Int64

    @State private var isBookmarked: Bool = false
    @State private var isLoading: Bool = false

    var body: some View {
        Button {
            Task { await toggle() }
        } label: {
            Image(systemName: isBookmarked ? "bookmark.fill" : "bookmark")
                .foregroundStyle(isBookmarked ? Color.brandPrimary : Color.appTextSubtle)
        }
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
```

## Validation

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

### 스크린샷 검증 (선택, Step 5 통합 시 정식)

본 step 단독으로는 진입점이 없어서(MockExamDetail 연결은 Step 5), SwiftUI Preview 만으로 시각 확인 가능. 빌드 통과가 필수 검증.

## 금지사항

- `Timer.publish(every: 1, ...)` 같은 RunLoop 타이머를 View 안에서 직접 사용하지 마라. 이유: 이미 ViewModel 의 `tickToken` 이 SwiftUI 재평가 트리거 역할. 중복 타이머는 배터리/CPU 낭비.
- 객관식 선택지를 5개 이상으로 확장하지 마라. 이유: 현재 백엔드 questionType `MULTIPLE_CHOICE` 는 4지선다 가정 (sqldpass 의 SQLD/정처기/컴활 모두 4지선다). 단답형은 별도 컴포넌트가 필요한 별도 작업.
- `Image(systemName:)` 외 커스텀 SVG/PNG 아이콘 추가 금지. 이유: 현재 단계에서는 SF Symbols 만 사용 — Asset Catalog 추가 시 디자인 토큰 동기화 비용 발생.
- `BookmarkToggleButton` 의 `isBookmarked` 초기 fetch 가 실패해도 UI 가 멈추지 않게 하라. 이유: 네트워크 끊김 상태에서도 화면 진입은 가능해야 함.
