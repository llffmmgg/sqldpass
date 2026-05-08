# Step 3 — blog/page.tsx의 impure-function-during-render 1건

## 배경

step 1·2에서 `react-hooks/set-state-in-effect` 12건을 모두 처리했고, 남은 lint error는 별개 룰 1건:

```
frontend/src/app/blog/page.tsx
  78:50  error  Error: Cannot call impure function during render
```

이 룰은 RSC/일반 React 컴포넌트의 render 함수 본문에서 부수효과·non-deterministic 함수(예: `Date.now()`, `Math.random()`, 외부 mutable read)를 호출하는 것을 차단한다. blog 페이지는 SSR + ISR로 동작하며, render 시 호출되는 impure 함수는 hydration mismatch나 inconsistent ISR 결과를 만든다.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

- `frontend/src/app/blog/page.tsx`

## 분석 요점

해당 파일 78:50을 열어 어떤 함수가 호출되는지 확인한다. 흔한 케이스:

- `new Date()` / `Date.now()` — 현재 시각 → 매 render마다 다른 값 → ISR 캐시 갱신 시점·hydration mismatch 위험.
- `Math.random()` — 비결정성 → 위와 동일.
- 모듈 스코프 mutable 변수 read — page.tsx에서 다른 모듈의 가변 상태를 직접 읽음.

`d50f491` commit이 "최근 3일 이내 작성된 글에 NEW 배지"를 추가했으므로, 78줄 부근에 `Date.now()` 또는 `new Date()` 비교 로직이 있을 가능성이 높다.

## 표준 리팩터링 패턴

### 패턴 NEW-A — server component면 `unstable_cache` 또는 RSC `cache()`로 감싸기

```tsx
import { cache } from "react";
const getNow = cache(() => new Date());
// ... rendering
const cutoff = getNow();
```

같은 render 트리 안에서 한 번만 평가되도록 한다.

### 패턴 NEW-B — page 진입 시 단 한 번 평가될 server prop으로 끌어올리기

`generateMetadata`나 page 함수 시작부에서 `const now = new Date()` 한 번 평가하고 props로 내려보낸다. 룰은 *render 본문* 호출을 잡으니 변수에 캐시되면 통과.

### 패턴 NEW-C — 결정성 cutoff로 대체

NEW 배지 기준을 "마지막 빌드 시점 기준 N일 이내"로 바꾼다 — `process.env.NEXT_PUBLIC_BUILD_TIME` 또는 `BUILD_ID` 등 빌드 타임 결정 값. ISR cache 안에서 일관됨.

→ blog 페이지의 의도는 "사용자에게 *현재 시점* 기준 NEW 배지"이므로 **패턴 NEW-A 또는 NEW-B** 우선. NEW-C는 "이 빌드 이후 N일 한정"이 되어 의도가 살짝 달라진다.

## Acceptance Criteria

1. `frontend/src/app/blog/page.tsx`의 line 78 부근에서 impure 호출이 render 함수 본문에서 사라진다 — 변수에 캐시되거나 `cache()`로 감싸지거나, server prop으로 끌어올려진다.
2. NEW 배지 동작이 동일하다 — 최근 3일 이내 글에 NEW가 보이고, 3일 이상 된 글에는 안 보인다.
3. `npm run lint`에서 `Cannot call impure function during render` 에러 0건.
4. `npm run build` 통과.
5. ISR 행동 검증 (가능한 범위에서) — 같은 페이지를 여러 번 새로고침해도 NEW 배지 결과가 안정적.

## 금지 사항

- NEW 배지 로직을 client component로 옮기지 마라. 이유: 첫 페인트에 배지가 깜박이게 된다 (hydration 후 등장). SSR 결과가 결정적이도록 *server에서* 시각을 한 번 픽스하는 게 정공.
- NEW 배지 기준 일수(3일)를 바꾸지 마라. 이유: 기능 변경. lint 정리 step에서는 동작 동일성 유지.
- `Date.now()` 호출을 단순히 변수에 할당해도 *render 함수 안*에 두지 마라. 이유: 같은 룰에 다시 잡힐 수 있다. `cache()` 또는 page 함수 *진입부*에서 한 번만.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

성공 조건:
- `react-hooks/set-state-in-effect` 0건 (step 1·2 결과 누적)
- `impure-function-during-render` 0건
- 다른 룰 새 위반 없음
- build 통과

수동 확인:
- `npm run dev` → `/blog` 진입 → 최근 작성 글에 NEW 배지 노출 확인.

## Status 규칙

- 성공: step 3 status를 `completed`로, summary에 "blog/page.tsx 78:50 impure call → {적용한 패턴} 으로 정리, NEW 배지 동작 동일" 기록.
- 실패: 3회 시도 후에도 lint·동작 회귀 시 `error`.
- blocked: 빌드타임 vs 런타임 시점 결정이 사용자 합의가 필요해 보이면 `blocked` + 사유.
