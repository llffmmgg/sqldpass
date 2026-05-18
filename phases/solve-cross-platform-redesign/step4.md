# Step 4 — iOS 단일 채점 풀이 화면 신규 (ios-solo-solve-screen)

## 배경

iOS `ios/Sqldpass/Features/Solve/SolveView.swift` 는 **모의고사 응시 모드 전용**(여러 문항 → 마지막 일괄 제출). 웹 `/solve` 의 **단일 채점 모드(1문제씩 즉시 채점)** 는 없다.

본 step 은 step 3 의 Android `SoloSolveScreen` 과 동등한 UX 의 iOS 화면을 신규 구현한다. 본문 렌더는 step 2 의 `QuestionContentView` 사용. `docs/SOLVE_SCREEN_SPEC.md` 의 정보 위계 / 상태 다이어그램 / 햅틱 / 인터랙션을 그대로 따른다.

기존 `SolveView` / `SolveResultView` 는 모의고사 모드로 남기고 본 step 에서 건드리지 않는다.

## 작업 디렉터리

```bash
cd ios
```

macOS 셸에서만. Windows 에서 빌드 검증을 시도하지 말 것.

## 변경 대상

| 파일 | 변경 |
|---|---|
| `ios/Sqldpass/Features/Solo/SoloSolveView.swift` | 신규 — 단일 채점 풀이 화면 메인 |
| `ios/Sqldpass/Features/Solo/SoloSolveViewModel.swift` | 신규 — `@Observable`. 상태 머신 (loading / idle / submitting / revealed / sessionComplete / error) + 즉시 채점 + 다음 문제 fetch |
| `ios/Sqldpass/Features/Solo/Components/SolveOptionRow.swift` | 신규 — 옵션 1개의 5가지 시각 상태 (미답/선택/정답공개 정답/정답공개 오답/정답공개 무선택). `withAnimation(.easeOut(duration: 0.2))`, shake offset, bounce scale |
| `ios/Sqldpass/Features/Solo/Components/SoloProgressHeader.swift` | 신규 — 종료/진행도/북마크/신고 + 진행 바 (`ProgressView` linear, primary tint) |
| `ios/Sqldpass/Features/Solo/Components/SoloExplanationCard.swift` | 신규 — 모범답안/키워드/해설 묶음 (정답 공개 시) |
| `ios/Sqldpass/Features/Solo/Components/SoloBottomActionBar.swift` | 신규 — 하단 [이전][정답 확인 / 다음 문제]. `.safeAreaInset(edge: .bottom)` |
| `ios/Sqldpass/Features/Solo/Components/AnonQuotaChip.swift` | 신규 — 비회원 일일 한도 칩 |
| `ios/Sqldpass/App/MainTabView.swift` | Home 탭의 "10문제 풀기" CTA 가 `NavigationLink` 로 `SoloSolveView` push 하도록 연결. 자격증 컨텍스트는 ProfileViewModel 또는 별도 store 에서 마지막 선택 자격증 사용 |
| `ios/Sqldpass/Services/SolveService.swift` | 기존 `submit(_:)` 확장 — `SubmitRequest.Single(subjectId, answer)` convenience 추가(단일 채점 호환). 단 응답 형식은 기존 `Solve` 그대로 |
| `ios/project.yml` | Features/Solo 폴더 등록 (xcodegen sources path 추가) |

## 상태 머신 (SoloSolveViewModel)

```swift
@Observable
final class SoloSolveViewModel {
    enum State: Equatable {
        case loading
        case idle(Question, solved: Int, correct: Int, selectedOption: Int?, answerText: String)
        case submitting(prev: State)
        case revealed(Question, QuestionDetail, isCorrect: Bool, solved: Int, correct: Int, selectedOption: Int?, answerText: String)
        case sessionComplete(solved: Int, correct: Int, subjectName: String)
        case error(message: String, retry: (() -> Void)?)
    }

    private(set) var state: State = .loading
    let subjectId: Int64
    let subjectName: String
    private let setSize = 10

    func start() async { ... }
    func selectOption(_ num: Int) { ... }      // 햅틱 light, idle/idle 만 처리
    func setAnswerText(_ text: String) { ... } // SHORT/DESCRIPTIVE
    func submit() async { ... }                // idle → submitting → revealed. 즉시 채점 + 백그라운드 POST /api/solves
    func goNext() async { ... }                // revealed → 다음 Question fetch. solved==setSize 면 sessionComplete
    func goPrevious() { ... }                  // optional, undo 1 step
    func exit() { ... }                        // 미답 + 답안 있음이면 confirm
}
```

## 인터랙션 디테일

- **옵션 1회 탭**: 선택. `UIImpactFeedbackGenerator(style: .light).impactOccurred()`.
- **옵션 더블탭** (revealed 전): `.onTapGesture(count: 2) { select + submit }`. 단일 탭과 충돌 방지 위해 `.onTapGesture(count: 1)` 은 `simultaneousGesture` 로 처리하거나 0.25s 디바운스. 햅틱 mediumImpact.
- **"정답 확인"**: `submit()`. 선택 없으면 비활성.
- **정답 공개 시**:
  - 정답 옵션 = `Color.semanticSuccess` border 2pt + `checkmark.circle.fill` icon
  - 선택 오답 = `Color.semanticDanger` border 2pt + `xmark.circle.fill` icon + offset shake animation (±4pt, 3 cycles, 0.3s 총)
  - 무선택 옵션 = opacity 0.5
  - 햅틱: 정답 `.notificationOccurred(.success)`, 오답 `.notificationOccurred(.warning)`
- **"다음 문제"**: `withAnimation(.easeOut(duration: 0.25)) { state = .idle(...) }`. 새 카드는 `.transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading)))`.

## SolveOptionRow 시각 상태 표

step 3 의 Android 표와 동일한 의미. iOS native 색은 `Color.semanticSuccess` / `Color.semanticDanger` / `Color.brandPrimary` / `Color.appBorder` 사용. tint container 가 토큰에 없으면 `.opacity(0.12)` 로(이 정도는 의미 있는 강조라 허용).

## 햅틱 헬퍼 (재사용)

```swift
// ios/Sqldpass/Core/Haptics/Haptics.swift (신규)
import UIKit

enum Haptics {
    static func light() { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
    static func medium() { UIImpactFeedbackGenerator(style: .medium).impactOccurred() }
    static func success() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
    static func warning() { UINotificationFeedbackGenerator().notificationOccurred(.warning) }
    static func error() { UINotificationFeedbackGenerator().notificationOccurred(.error) }
}
```

`Core/Haptics/` 가 없으면 신설. 단 본 step 의 핵심은 SoloSolveView 이며 Haptics 헬퍼는 본 step 안에서 같이 만들면 충분 (별 step 분리 불필요).

## Acceptance Criteria

1. `xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass -destination 'platform=iOS Simulator,name=iPhone 15 Pro' -configuration Debug build` → `** BUILD SUCCEEDED **`
2. 신규 파일들이 project.yml sources 패턴에 매칭되어 빌드에 포함.
3. Home 탭 → "10문제 풀기" CTA 탭 → SoloSolveView push.
4. 옵션 탭 시 시각/햅틱, 더블탭 시 즉시 채점.
5. 정답 공개 후 SoloExplanationCard 등장 (모범답안/키워드는 SHORT/DESCRIPTIVE 만).
6. 10문제 완료 시 SessionComplete 카드 (점수/정답률/"같은 10문 다시"/"새 10문").
7. 매직 넘버 없음 — 모든 값은 `Spacing.*`, `Radius.*` 토큰 참조.
8. 시뮬레이터 스크린샷 4장: 미답/선택/정답공개 정답/정답공개 오답+해설. `/tmp/sqldpass-step4-*.png` 저장.

## 검증

```bash
cd ios
~/bin/xcodegen generate
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -10
```

스크린샷:

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 3
# 화면을 SoloSolveView 까지 이동 후 각 상태별 screenshot
xcrun simctl io booted screenshot /tmp/sqldpass-step4-idle.png
```

**Windows 환경에서 본 step 빌드 불가** — 코드 변경만 본 step 에서 수행하고, 빌드/스크린샷 검증은 macOS 에서 사용자가 실행. 결과를 step summary 에 기록.

## 금지 사항

- 기존 `SolveView.swift` / `SolveResultView.swift` 를 수정하지 마라. 이유: 모의고사 응시 모드 회귀 위험.
- `WKWebView` 또는 외부 markdown 라이브러리를 본 step 에서 추가하지 마라. 이유: step 2 의 `QuestionContentView` 가 이미 본문을 렌더한다.
- 옵션 시각에 `.background(.thinMaterial)` 또는 `.shadow(radius: ...glow...)` 효과를 쓰지 마라. 이유: MEMORY 의 blur/glow 금지. solid surface + 명확한 border 만.
- 풀이 제출(`POST /api/solves`) 응답을 await 하면서 옵션 비활성화하지 마라. 이유: 즉시 채점 UX 가 깨짐. 클라이언트 측 채점으로 즉시 표시하고 서버 호출은 `Task { ... }` 로 백그라운드 (실패 시 step 6 의 큐잉).
- `Color(red:..., green:..., blue:...)` 또는 `Color.green` 같은 시스템 색을 박지 마라. 이유: `ios/AGENTS.md` 의 디자인 토큰 규칙.
- `Haptics` enum 을 actor 또는 MainActor 격리로 만들지 마라. 이유: `UIImpactFeedbackGenerator` 는 main thread 에서만 호출되지만 enum 정적 메서드로 충분하며, 격리 어노테이션은 호출부 노이즈.

## Status 규칙

- 성공: index.json step 4 `completed`, summary 에 빌드 결과 + 스크린샷 경로 4장.
- 실패: 3회 시도 후 `error` + `error_message`.
- Windows 환경에서 코드만 작성: `blocked` + `blocked_reason: "iOS 빌드/스크린샷 검증을 위해 macOS 환경 필요"`.
