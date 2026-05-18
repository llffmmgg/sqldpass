# Step 2 — SolveViewModel (페이징/타이머/답안 상태/제출)

## Background

풀이 화면의 비즈니스 로직만 분리해 ViewModel 로 작성한다. View 는 Step 3 에서. ViewModel 분리 이유:

1. **테스트 가능성** — SwiftUI 의존 없이 단위 테스트로 검증 가능.
2. **타이머의 백그라운드 안전성** — `Timer` 가 백그라운드/잠금 화면에서 중단되지만 `Date` 델타로 경과 시간을 다시 계산하면 정확함.
3. **재진입 대비** — Step 5 에서 MockExamDetail 에서 푸시할 때 ViewModel 을 새로 만들어 시작 상태로 초기화.

## Workdir

```bash
ios/
```

## Dependencies

- Step 1 산출물:
  - `Services/QuestionService.swift` — `QuestionService.detail(id:)` 사용
  - `Models/SolveAnswerEntry.swift` — ViewModel 내 답안 state
- 기존 산출물:
  - `Models/MockExamDetail.swift` (MockExamQuestionItem 사용)
  - `Models/Solve.swift` (Solve, SolveService.SubmitRequest 사용)
  - `Core/Networking/APIError.swift`

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Features/Solve/SolveViewModel.swift` | 신규 — @Observable, 풀이 세션 상태 일체 |

## Implementation

### `ios/Sqldpass/Features/Solve/SolveViewModel.swift`

```swift
import Foundation
import Observation

/// 풀이 세션 1 회분의 상태 + 액션.
///
/// 라이프사이클:
///   init(mockExamId:questions:) → start() → answer/navigation → submit() → result
///
/// View 는 `currentQuestion`, `progress`, `elapsedSeconds`, `selectedAnswer` 등을
/// 관찰하고 액션(`select`, `next`, `previous`, `toggleMark`, `submit`) 을 호출.
@Observable
final class SolveViewModel {
    // MARK: Inputs (불변)

    let mockExamId: Int64
    let questions: [MockExamQuestionItem]

    // MARK: State

    private(set) var currentIndex: Int = 0
    private(set) var answers: [Int64: SolveAnswerEntry] = [:]

    /// `start()` 호출 시점. 경과 시간 계산 기준.
    private(set) var startedAt: Date?

    /// 1 초마다 트리거되는 ticker. View 가 elapsedSeconds 를 다시 읽도록.
    private(set) var tickToken: Int = 0
    private var timerTask: Task<Void, Never>?

    // 제출 상태
    private(set) var isSubmitting = false
    private(set) var submittedResult: Solve?
    private(set) var errorMessage: String?

    // MARK: Derived

    var totalCount: Int { questions.count }

    var currentQuestion: MockExamQuestionItem? {
        guard questions.indices.contains(currentIndex) else { return nil }
        return questions[currentIndex]
    }

    var progress: Double {
        guard totalCount > 0 else { return 0 }
        return Double(currentIndex + 1) / Double(totalCount)
    }

    /// 답을 고른 문항 수
    var answeredCount: Int {
        answers.values.filter { $0.chosenAnswer != nil }.count
    }

    /// 다시보기 표시된 문항 수
    var markedCount: Int {
        answers.values.filter { $0.markedForReview }.count
    }

    /// `start()` 이후 경과 초. 백그라운드 진입 후 복귀해도 정확함.
    var elapsedSeconds: Int {
        guard let startedAt else { return 0 }
        _ = tickToken // tick 변화에 의존성 등록
        return Int(Date().timeIntervalSince(startedAt))
    }

    /// 현재 문항의 답안 (있으면)
    var currentEntry: SolveAnswerEntry? {
        guard let q = currentQuestion else { return nil }
        return answers[q.id]
    }

    var canSubmit: Bool {
        !isSubmitting && submittedResult == nil && totalCount > 0
    }

    // MARK: Init

    init(mockExamId: Int64, questions: [MockExamQuestionItem]) {
        self.mockExamId = mockExamId
        self.questions = questions
    }

    deinit {
        timerTask?.cancel()
    }

    // MARK: Lifecycle

    func start() {
        guard startedAt == nil else { return }
        startedAt = Date()
        startTicker()
    }

    func stopTimer() {
        timerTask?.cancel()
        timerTask = nil
    }

    private func startTicker() {
        timerTask?.cancel()
        timerTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                await MainActor.run { [weak self] in
                    self?.tickToken &+= 1
                }
            }
        }
    }

    // MARK: Navigation

    func goNext() {
        guard currentIndex < totalCount - 1 else { return }
        currentIndex += 1
    }

    func goPrevious() {
        guard currentIndex > 0 else { return }
        currentIndex -= 1
    }

    func go(to index: Int) {
        guard questions.indices.contains(index) else { return }
        currentIndex = index
    }

    // MARK: Answer actions

    func select(_ chosen: String) {
        guard let q = currentQuestion else { return }
        var entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
        entry.chosenAnswer = chosen
        answers[q.id] = entry
    }

    func clearAnswer() {
        guard let q = currentQuestion else { return }
        guard var entry = answers[q.id] else { return }
        entry.chosenAnswer = nil
        answers[q.id] = entry
    }

    func toggleMark() {
        guard let q = currentQuestion else { return }
        var entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
        entry.markedForReview.toggle()
        answers[q.id] = entry
    }

    // MARK: Submission

    @MainActor
    func submit() async {
        guard canSubmit else { return }
        isSubmitting = true
        errorMessage = nil

        let payload = SolveService.SubmitRequest(
            subjectId: nil,
            mockExamId: mockExamId,
            answers: questions.map { q in
                let entry = answers[q.id] ?? SolveAnswerEntry(questionId: q.id)
                return entry.toSubmitAnswer
            }
        )

        do {
            let result = try await SolveService.submit(payload)
            submittedResult = result
            stopTimer()
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

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

기대: `** BUILD SUCCEEDED **`

### 추가 검증

- `Features/Solve/SolveViewModel.swift` 가 추가됐는지.
- `xcodegen generate` 가 새 파일을 자동 sources 에 포함하는지 (sources path: `Sqldpass` 디렉토리 전체이므로 자동 포함).

## 금지사항

- `Timer.scheduledTimer` 같은 RunLoop 기반 타이머 사용 금지. 이유: 백그라운드 진입 시 멈추고 복귀해도 누락된 tick 이 발생. 이미 `elapsedSeconds` 는 `Date()` 델타라 백그라운드 안전. tick 은 단지 View 재평가 트리거.
- View(UIKit/SwiftUI) import 금지. 이유: ViewModel 은 View 없는 순수 비즈니스 로직. `Foundation` + `Observation` 만 사용.
- SwiftData/CoreData 또는 디스크 persistence 금지. 이유: 크래시 복구는 다음 phase 의 별도 작업 — 이번엔 메모리 only.
- `errorMessage` 를 setter 외부에서 직접 수정하지 마라(`private(set)`). 이유: 상태 단일 출처 보장.
