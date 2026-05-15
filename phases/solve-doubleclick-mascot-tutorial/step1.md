# Step 1 — Tutorial infra + mascot modal

## Background

풀이 첫 진입 시 노출할 문어 마스코트 튜토리얼의 공통 인프라를 만든다.
이후 step 2에서 네 풀이 화면이 같은 헬퍼와 모달을 재사용하므로 인프라를 먼저 분리한다.

신규 의존성 없음 — Tailwind keyframe과 기존 `MascotImage`(pose `guide`)만 사용.

## Workdir

```powershell
frontend/
```

## Scope

| File | Change |
| --- | --- |
| `frontend/src/lib/tutorialStorage.ts` | 신규: localStorage 단일 키(`seen_solve_tutorial_v1`) 헬퍼. SSR/예외 안전. |
| `frontend/src/components/SolveTutorialModal.tsx` | 신규: 마스코트 + 안내 + "알겠어요" 버튼 모달. open/onClose props. ESC + 배경 클릭 + body scroll lock. 닫을 때 `markSolveTutorialSeen()` 호출. |
| `frontend/src/app/globals.css` | 마스코트 keyframe 두 개(`mascot-bob-in`, `mascot-bounce-soft`) + 유틸 클래스 두 개(`animate-mascot-in`, `animate-mascot-bounce`) 추가. |

## Implementation

### `tutorialStorage.ts`

```ts
const KEY = "seen_solve_tutorial_v1";

export function hasSeenSolveTutorial(): boolean {
  if (typeof window === "undefined") return true;
  try { return localStorage.getItem(KEY) === "1"; } catch { return true; }
}
export function markSolveTutorialSeen(): void {
  if (typeof window === "undefined") return;
  try { localStorage.setItem(KEY, "1"); } catch {}
}
```

### `SolveTutorialModal.tsx`

- `"use client"` 선언.
- props: `{ open: boolean; onClose: () => void }`.
- 배경: `fixed inset-0 z-[60] bg-black/60` — `backdrop-blur` 금지(`docs/UI_GUIDE.md` AI 슬롭 안티패턴).
- 컨테이너: `rounded-2xl border border-border bg-surface p-6 shadow-xl`, 모바일 `max-w-sm`, sm 이상 `max-w-md`.
- 내부 레이아웃 (수직):
  - 상단: `<MascotImage pose="guide" size={112} className="animate-mascot-in animate-mascot-bounce" priority />`
  - 본문 제목: `안녕! 빠르게 푸는 팁 하나 알려줄게.`
  - 본문 설명 두 줄:
    - `보기를 더블클릭하면 한 번에 진행할 수 있어.`
    - `키보드 1~4번, Enter로도 빨라져요.`
  - 단단한 primary 버튼: `<Button variant="primary" size="md" onClick={handleClose} autoFocus>알겠어요</Button>`
- 동작:
  - `useEffect`로 ESC 키 + body scroll lock — `FeedbackModal.tsx:51-62` 패턴 그대로.
  - 배경 클릭, ESC, 버튼 클릭 모두 `handleClose()` → `markSolveTutorialSeen()` + `onClose()`.
  - 컨테이너 click 은 `stopPropagation()`.
- 컬러는 토큰만 사용 (`bg-surface`, `text-text`, `text-text-muted`, `bg-primary`). 자격증 액센트 등 직접 색 변경 금지.

### `globals.css` 추가

```css
@keyframes mascot-bob-in {
  0%   { opacity: 0; transform: translateY(20px) scale(0.9); }
  60%  { opacity: 1; transform: translateY(-6px) scale(1.04); }
  100% { opacity: 1; transform: translateY(0)    scale(1);    }
}
@keyframes mascot-bounce-soft {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-4px); }
}
.animate-mascot-in     { animation: mascot-bob-in 520ms cubic-bezier(0.22, 1, 0.36, 1); }
.animate-mascot-bounce { animation: mascot-bounce-soft 2.4s ease-in-out infinite 520ms; }
```

`animate-mascot-bounce`는 진입 애니메이션 끝난 뒤 매끄럽게 이어지도록 `520ms` 지연을 시작값으로 둔다.

## 금지 사항

- `backdrop-blur-*`, `drop-shadow-2xl glow`, `opacity-*` 펄스 추가 금지. 이유: 사용자 룰 [[feedback_no_ai_blur_effects]] 및 `docs/UI_GUIDE.md` 안티패턴.
- 색 계열 변경 금지(예: amber-500 → 800). 이유: 사용자 룰 [[feedback_color_token_changes]]. 모달은 토큰만 사용.
- framer-motion / lottie 신규 의존성 추가 금지. 이유: 본 phase에서 명시적으로 keyframe-only 결정됨.

## Validation

- 파일 3개가 의도한 경로에 존재.
- 빌드/렌더는 step 3에서 일괄 검증.

## Status Rules

- Success: step 1 `completed`로 표시, summary에 신규 파일 경로 기록.
- Failure: 동일 변경 3회 실패 시 `error`.
