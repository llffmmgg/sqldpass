# Step 5 — Wiring + 최종 빌드/시뮬레이터 검증

## Background

Step 3 의 SolveView 와 Step 4 의 SolveResultView 를 MockExamDetailView 와 묶어서 풀이 사이클을 완성한다.

흐름:
1. **MockExamsList** → MockExamDetail 푸시 (이미 됨)
2. **MockExamDetail "시험 시작"** → SolveView 푸시 (본 step)
3. **SolveView "제출"** → SolveResultView 푸시 (본 step)
4. **SolveResultView "목록으로"** → MockExams 루트로 pop (본 step)

NavigationStack 경로 관리는 `NavigationPath` 사용.

## Workdir

```bash
ios/
```

## Dependencies

- Step 1: AGENTS.md, QuestionService, BookmarkService, SolveAnswerEntry
- Step 2: SolveViewModel
- Step 3: SolveView + components
- Step 4: SolveResultView + components
- 기존: `Features/MockExams/MockExamsListView.swift`, `MockExamDetailView.swift`

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/MockExams/MockExamsListView.swift` | 수정 — NavigationPath 도입, destination 분기 (Detail / Solve / Result) |
| `ios/Sqldpass/Features/MockExams/MockExamDetailView.swift` | 수정 — "시험 시작" 버튼 → path.append(SolveRoute) |
| `ios/Sqldpass/Features/MockExams/MockExamRoutes.swift` | 신규 — Hashable enum 라우트 |

## Implementation

### `Features/MockExams/MockExamRoutes.swift` (신규)

```swift
import Foundation

/// MockExams 탭의 NavigationStack 라우트.
/// Sheet 모달 대신 push 로 풀이→결과 흐름을 연결한다.
enum MockExamRoute: Hashable {
    case detail(examId: Int64)
    case solve(examId: Int64, questions: [MockExamQuestionItem])
    case result(result: Solve, questions: [MockExamQuestionItem])
}
```

### `Features/MockExams/MockExamsListView.swift` (수정)

기존 구현에서 `NavigationLink(value: exam)` + `navigationDestination(for: MockExamSummary.self)` 를 사용하고 있다. `NavigationPath` 로 교체하고 라우트 분기 추가.

```swift
import SwiftUI

struct MockExamsListView: View {
    @State private var viewModel = MockExamsViewModel()
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            content
                .background(Color.appPage)
                .navigationTitle("모의고사")
                .navigationBarTitleDisplayMode(.large)
                .refreshable {
                    await viewModel.load()
                }
                .task {
                    if viewModel.exams.isEmpty {
                        await viewModel.load()
                    }
                }
                .navigationDestination(for: MockExamRoute.self) { route in
                    switch route {
                    case .detail(let examId):
                        MockExamDetailView(examId: examId, path: $path)
                    case .solve(let examId, let questions):
                        SolveView(
                            viewModel: SolveViewModel(mockExamId: examId, questions: questions),
                            onSubmitted: { result in
                                path.append(MockExamRoute.result(result: result, questions: questions))
                            }
                        )
                    case .result(let result, let questions):
                        SolveResultView(
                            result: result,
                            questions: questions,
                            onDone: {
                                path = NavigationPath() // 모의고사 목록으로 복귀
                            }
                        )
                    }
                }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.exams.isEmpty {
            ProgressView()
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.exams.isEmpty {
            ContentUnavailableView {
                Label("불러오기 실패", systemImage: "exclamationmark.triangle")
            } description: {
                Text(errorMessage)
            } actions: {
                Button("재시도") {
                    Task { await viewModel.load() }
                }
            }
        } else if viewModel.exams.isEmpty {
            ContentUnavailableView(
                "모의고사가 없어요",
                systemImage: "doc.text",
                description: Text("관리자가 모의고사를 등록하면 여기에 표시됩니다")
            )
        } else {
            ScrollView {
                LazyVStack(spacing: Spacing.md) {
                    ForEach(viewModel.exams) { exam in
                        Button {
                            path.append(MockExamRoute.detail(examId: exam.id))
                        } label: {
                            ExamCard(exam: exam)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(Spacing.base)
            }
        }
    }
}

private struct ExamCard: View {
    let exam: MockExamSummary

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                Text(exam.typeLabel)
                    .font(AppType.caption.weight(.semibold))
                    .foregroundStyle(exam.typeAccentColor)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xxs)
                    .overlay(
                        RoundedRectangle(cornerRadius: Radius.full)
                            .stroke(exam.typeAccentColor.opacity(0.5), lineWidth: 1)
                    )

                if exam.isPastExam {
                    Text("기출")
                        .font(AppType.caption.weight(.semibold))
                        .foregroundStyle(Color.semanticInfo)
                        .padding(.horizontal, Spacing.sm)
                        .padding(.vertical, Spacing.xxs)
                        .background(Color.semanticInfo.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: Radius.full))
                }

                if exam.expertVerified {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.caption)
                        .foregroundStyle(Color.brandPrimary)
                }

                Spacer()

                if exam.isPremium && !exam.purchased {
                    Image(systemName: "lock.fill")
                        .font(.callout)
                        .foregroundStyle(Color.semanticWarning)
                }
            }

            Text(exam.name)
                .font(AppType.bodyEmph)
                .foregroundStyle(Color.appTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: Spacing.md) {
                Label("\(exam.totalQuestions)문제", systemImage: "list.number")
                    .font(AppType.footnote)
                    .foregroundStyle(Color.appTextMuted)

                if let label = exam.difficultyLabel {
                    Label(label, systemImage: "speedometer")
                        .font(AppType.footnote)
                        .foregroundStyle(Color.appTextMuted)
                }

                Spacer()

                if let best = exam.bestScoreLabel {
                    Text(best)
                        .font(AppType.monoNumeric.weight(.semibold))
                        .foregroundStyle(exam.solved ? Color.brandPrimary : Color.appTextMuted)
                }
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
    MockExamsListView()
}
```

### `Features/MockExams/MockExamDetailView.swift` (수정)

기존 구현의 "시험 시작하기" 버튼이 placeholder 였다. path binding 받고 `MockExamRoute.solve` 푸시.

수정할 부분:

1. **View 시그니처에 `path: Binding<NavigationPath>` 추가**
2. **시험 시작 버튼 action 에서 `path.wrappedValue.append(MockExamRoute.solve(...))` 호출**

```swift
import SwiftUI

struct MockExamDetailView: View {
    let examId: Int64
    @Binding var path: NavigationPath

    @State private var detail: MockExamDetail?
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.lg) {
                if let detail {
                    header(for: detail)
                    metadataGrid(for: detail)
                    subjectsSection(for: detail)
                }
            }
            .padding(Spacing.base)
        }
        .background(Color.appPage)
        .navigationTitle(detail?.name ?? "모의고사")
        .navigationBarTitleDisplayMode(.inline)
        .overlay {
            if isLoading && detail == nil {
                ProgressView().controlSize(.large)
            } else if let errorMessage, detail == nil {
                ContentUnavailableView {
                    Label("불러오기 실패", systemImage: "exclamationmark.triangle")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("재시도") { Task { await load() } }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if let detail {
                Button {
                    path.append(MockExamRoute.solve(examId: detail.id, questions: detail.questions))
                } label: {
                    Text("시험 시작하기")
                        .font(AppType.bodyEmph)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.brandPrimary)
                .padding(.horizontal, Spacing.base)
                .padding(.bottom, Spacing.sm)
                .background(Color.appPage)
                .disabled(detail.questions.isEmpty)
            }
        }
        .task {
            if detail == nil { await load() }
        }
    }

    // header(for:), metadataGrid(for:), metaCard(label:value:), subjectsSection(for:) — 기존 그대로 유지

    // ... (기존 helper 들 그대로 보존)

    private func load() async {
        isLoading = true
        do {
            detail = try await ExamService.detail(id: examId)
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

**중요**: 기존 파일의 `header`, `metadataGrid`, `metaCard`, `subjectsSection` private 함수들은 그대로 유지. 위에 표시한 것은 변경된 부분 (`@Binding var path`, `Button { path.append(...) }`)만이며, 나머지는 보존.

## Validation

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대: `** BUILD SUCCEEDED **`

### 시뮬레이터 스크린샷 검증 (필수)

본 phase 의 통합 검증. 빌드 후 시뮬레이터 부팅 + 앱 실행 + 모의고사 탭 화면을 캡쳐.

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl uninstall booted com.sqldpass.app
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
xcrun simctl io booted screenshot /tmp/ios-solve-flow-final.png
```

스크린샷 검증:
- 로그인 화면(SessionGate) 또는 메인 탭이 보여야 함 (토큰 유무에 따라)
- 빌드 산출물이 정상 동작하는지 시각 확인 (build success 만으로 충분하지만 추가 신뢰)

런타임 검증 (API dev 서버 실행은 본 phase 범위 외):
- 실 사용자 흐름(모의고사 선택 → 시험 시작 → 풀이 → 제출 → 결과) 은 dev 서버 + 로그인 토큰이 필요. **본 step 에서는 빌드 + 화면 진입 가능 여부만 검증**. 풀 사이클 검증은 별도 phase `ios-end-to-end-smoke` 에서 처리.

## 금지사항

- `Features/MockExams/MockExamsListView.swift` 의 기존 `navigationDestination(for: MockExamSummary.self)` 를 그대로 두지 마라(중복). 이유: `MockExamRoute` 로 통합한 후 unused 경로는 제거해야 함.
- 결과 화면에서 `path.removeLast(2)` 같이 정확한 단계 수를 가정한 pop 하지 마라. 이유: 향후 라우트 계층이 늘어나면 깨짐. `path = NavigationPath()` 로 루트 복귀가 안전.
- iOS 18.2 destination 으로 빌드하지 마라(`iPhone 16 Pro`/iOS 18.2 시뮬레이터 미설치). 이유: 현재 머신에 설치된 시뮬레이터 런타임은 iOS 17.5 만. iPhone 15 Pro/iOS 17.5 destination 사용.
- 시뮬레이터를 새 디바이스(iPhone 16 등) 로 띄우지 마라. 이유: 기존 booted 시뮬레이터(iPhone 15 Pro, iOS 17.5) 그대로 사용 — `simctl boot` 추가 호출 불필요.
