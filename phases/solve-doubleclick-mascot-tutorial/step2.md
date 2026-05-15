# Step 2 — 더블클릭 단축 + 모달 마운트 (4개 풀이 화면)

## Background

Step 1에서 만든 `SolveTutorialModal` + `hasSeenSolveTutorial` / `markSolveTutorialSeen` 헬퍼를 네 개 풀이 화면에 연결한다.
옵션 더블클릭은 화면 종류에 따라 동작이 다르다.

- 단일 채점형 (즉시 정/오답 표시): 더블클릭 = `handleSelect` + `handleSubmit`. 자동 다음 X.
- 일괄 채점형 (마지막에 한 번에 채점): 더블클릭 = `select` + `goNext` (다음 문제로 즉시 이동).

## Workdir

```powershell
frontend/
```

## Scope

| File | Change |
| --- | --- |
| `frontend/src/app/solve/SolveClient.tsx` | `handleSubmit(forcedOption?: number)` 시그니처 확장, 옵션 `<button>`(라인 820 부근)에 `onDoubleClick={() => { if (revealed) return; handleSelect(num); handleSubmit(num); }}` + `select-none touch-manipulation` 추가. 첫 진입(`phase === "solve"`) 시 `SolveTutorialModal` 노출. |
| `frontend/src/app/solve/bookmarks/BookmarksSolveClient.tsx` | 동일 패턴. `handleSubmit(forcedOption?: number)` 시그니처 확장, 옵션 `<button>`(라인 579 부근). 풀이 phase 진입 시 모달 노출. |
| `frontend/src/components/past-exams/PastExamRunnerClient.tsx` | `MCQOptions`에 `onAdvance?: () => void` prop 추가, 옵션 `<button>`에 `onDoubleClick={() => { onSelect(num); onAdvance?.(); }}` + 클래스. 호출부에서 `onAdvance={goNext}`. 컴포넌트 마운트 시 모달 노출(타이머 시작 전이라 일시정지 불필요 — 사용자가 "시작" 버튼 누르기 전 진입). |
| `frontend/src/app/mock-exams/[id]/page.tsx` | MCQ 옵션 렌더링부에 `onDoubleClick={() => { selectOption(num); goNext(); }}` + 클래스. 타이머가 이미 도는 화면이므로 모달 열려있는 동안 `setTimerRunning(false)`, 닫으면 `setTimerRunning(true)`. |

## Implementation

### A. `handleSubmit(forcedOption?: number)` 패턴

두 단일 채점형 화면에서 동일하게 적용.

```ts
async function handleSubmit(forcedOption?: number) {
  const effectiveOption = forcedOption ?? selectedOption;
  if (!current || revealed) return;
  // 기존 hasAnswer() 가드를 effectiveOption 기반으로 교체
  const hasAns =
    current.questionType === "MCQ"
      ? effectiveOption !== null
      : answerText.trim().length > 0;
  if (!hasAns) return;
  // ... 이후 채점에서는 effectiveOption / answerText 사용
}
```

- MCQ 정답 비교가 내부에 있다면 `selectedOption` → `effectiveOption` 으로 교체.
- 기존 인자 없는 호출부(엔터키, "확인" 버튼)는 그대로 작동(`forcedOption` undefined → `selectedOption` fallback).
- 더블클릭 호출: `handleSelect(num); handleSubmit(num);` — `handleSelect` 는 UI 상태(`setSelectedOption`)만 업데이트하므로 같은 틱에 호출 가능.

### B. 옵션 버튼 클래스 보강

네 화면 옵션 `<button>` 모두에 다음 두 클래스를 className에 추가:

- `select-none` — 더블클릭으로 텍스트 선택 방지.
- `touch-manipulation` — 모바일 더블탭 줌인 방지.

### C. 일괄 채점형 — `MCQOptions` 시그니처 확장

`PastExamRunnerClient.tsx:731-772`:

```tsx
function MCQOptions({
  options, selected, onSelect, onAdvance, cert,
}: {
  options: string[];
  selected: number | null;
  onSelect: (value: number) => void;
  onAdvance?: () => void;
  cert: CertKey;
}) {
  // ...
  <button
    onClick={() => onSelect(num)}
    onDoubleClick={() => { onSelect(num); onAdvance?.(); }}
    className={`... select-none touch-manipulation ${...}`}
  >
}
```

호출부: `<MCQOptions ... onAdvance={goNext} />`. `goNext`는 `currentIdx < total - 1` 가드가 이미 있어 마지막 문제에서는 안전한 no-op.

### D. 모달 마운트 — 네 화면 공통 패턴

```tsx
import { hasSeenSolveTutorial } from "@/lib/tutorialStorage";
import SolveTutorialModal from "@/components/SolveTutorialModal";

const [showTutorial, setShowTutorial] = useState(false);

useEffect(() => {
  if (!hasSeenSolveTutorial()) setShowTutorial(true);
}, []);

// 렌더 트리 최상단/최하단:
<SolveTutorialModal open={showTutorial} onClose={() => setShowTutorial(false)} />
```

**화면별 노출 시점:**

- `SolveClient.tsx`: 의존성 `[phase]`로 두고 `phase === "solve"` 일 때만 노출. 주제 선택 단계에서는 띄우지 않는다.
- `BookmarksSolveClient.tsx`: 풀이 시작 후 `current` 가 첫 세팅된 시점. 의존성 `[current?.questionId]` 로 두고 첫 1회만 (가드: `useRef<boolean>(false)`).
- `PastExamRunnerClient.tsx`: 마운트 즉시.
- `mock-exams/[id]/page.tsx` (`MockExamDetailContent`): 마운트 즉시 + 모달 열린 동안 `setTimerRunning(false)`, 닫으면 사용자 명시적 시작 흐름을 깨지 않게 — 모달 닫힐 때 현재 시험 상태가 이미 시작된 상태였다면 `setTimerRunning(true)`. 시험 시작 전이면 그대로 둔다. 안전한 방법: `useRef<boolean>(wasRunningBeforeModal)` 로 모달 열기 직전 상태를 저장하고, 닫을 때 그대로 복원.

## 금지 사항

- 자동 "다음 문제"를 단일 채점형(`SolveClient`, `BookmarksSolveClient`)에 적용하지 말 것. 이유: 정/오답 채점 결과와 해설을 사용자가 확인할 시간을 빼앗는다. 본 phase에서 명시적으로 결정됨.
- `MCQOptions`의 새 prop을 필수(required)로 만들지 말 것. 이유: 다른 곳에서 이 컴포넌트를 재사용 중일 수 있으므로 `onAdvance?` 로 옵셔널 유지.
- 모달 노출 가드용 localStorage 키를 화면마다 별도로 만들지 말 것. 이유: phase 결정 — 단일 키 `seen_solve_tutorial_v1`로 통합 관리.

## Validation

- step 3에서 lint/build 일괄.
- 수동 검증(`npm run dev`):
  - localStorage 키 제거 후 4개 화면에 각각 진입 → 첫 진입 시 모달 1회 노출, 닫고 다른 화면 진입 시 재노출 없음.
  - 단일 채점형: 옵션 더블클릭 → 즉시 채점 결과 표시, 자동 다음 X.
  - 일괄 채점형: 옵션 더블클릭 → 답 저장 + 다음 문제로 이동, 마지막 문제에서는 머묾.
  - 모바일(개발자 도구 모바일 모드) 더블탭 줌인 없음.

## Status Rules

- Success: step 2 `completed`로 표시, summary에 변경된 4개 화면과 더블클릭 동작 요약.
- Failure: 동일 시도 3회 실패 시 `error`.
- Blocked: `MCQOptions` 외부 호출처가 발견되어 시그니처 변경이 안전하지 않으면 `blocked`로 표시하고 호출부 목록 기록.
