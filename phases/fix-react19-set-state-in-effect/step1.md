# Step 1 — components/ react-hooks/set-state-in-effect 8건 정리

## 배경

React 19/Next.js 16에서 도입된 ESLint 룰 `react-hooks/set-state-in-effect`가 `useEffect` 본문 안에서 동기적으로 `setState`를 호출하는 패턴을 차단한다. 이유: cascading render를 유발해 성능을 떨어뜨린다 (https://react.dev/learn/you-might-not-need-an-effect).

`components/` 영역에서 발견된 위반 8건 (8개 파일):

| 파일 | 라인 | 비고 |
|------|------|------|
| `frontend/src/components/AuthGuard.tsx` | 12 | 로그인 가드 — auth 상태 sync |
| `frontend/src/components/CertTerminal.tsx` | 36, 43 | 자격증 인터랙션 — 한 파일 2건 |
| `frontend/src/components/ExamCountdownStrip.tsx` | 10 | 시험 카운트다운 |
| `frontend/src/components/FeedbackRail.tsx` | 89 | 플로팅 피드백 버튼 |
| `frontend/src/components/HeroCta.tsx` | 14 | 랜딩 CTA |
| `frontend/src/components/RankingSection.tsx` | 22 | 공개 랭킹 — 데이터 fetch |
| `frontend/src/components/admin/TrendChart.tsx` | 21 | 관리자 추세 차트 |
| `frontend/src/app/mock-exams/MockExamsClient.tsx` | 55 | mock-exams 클라이언트(예외적으로 app/ 하위지만 client component이므로 동일 step에서 처리) |

## 작업 디렉터리

```
frontend/
```

## 변경 대상

위 표의 8개 파일.

## 표준 리팩터링 패턴

각 위반 위치를 다음 4 패턴 중 하나로 변환한다 — 코드 의도를 가장 잘 보존하는 패턴을 선택.

### 패턴 A — 초기값 계산이면 `useState` lazy initializer

`useEffect`가 단지 첫 렌더 후 한 번 동기 setState하는 거라면 `useState(() => initial)`로 옮긴다.

```tsx
// before
const [v, setV] = useState(0);
useEffect(() => { setV(compute()); }, []);

// after
const [v, setV] = useState(() => compute());
```

### 패턴 B — 외부 데이터 fetch면 condition 분기 + setState 유지

진짜 외부 sync(API fetch, localStorage)면 룰의 취지대로 두되, setState를 항상 부르지 않고 abort/unmount 가드 + 변경 시에만 부르도록 한다.

```tsx
useEffect(() => {
  let alive = true;
  fetchX().then((next) => {
    if (!alive) return;
    setX((prev) => (prev === next ? prev : next));
  });
  return () => { alive = false; };
}, []);
```

이 패턴은 cascading render를 막지만 룰이 `setX`를 잡을 수 있다. 그 경우 *해당 라인 1줄*에만 `// eslint-disable-next-line react-hooks/set-state-in-effect -- 외부 fetch 결과 sync` 주석으로 면제하고 *반드시 사유를 적어라*.

### 패턴 C — 단순 sync면 `useSyncExternalStore`

window/localStorage 등 외부 store를 구독하는 거라면 `useSyncExternalStore`로 옮긴다 (React 18+ 표준).

```tsx
const value = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
```

### 패턴 D — 진짜 불필요면 제거

setState 결과가 effect 외부에서 읽히지 않으면 effect 자체를 제거.

## 변경 절차

1. 각 파일 위반 라인을 열어 의도를 파악한다.
2. 패턴 A–D 중 가장 적합한 것을 선택한다 (lazy init 우선, fetch는 패턴 B + 사유 주석).
3. 해당 컴포넌트의 *수동 동작*을 코드 구조로 검증한다 — props 변화, 마운트/언마운트, navigation 시 cleanup.
4. 8개 파일을 모두 수정한 뒤 `npm run lint`로 errors가 줄었는지 확인.

## Acceptance Criteria

1. 위 8개 파일의 `react-hooks/set-state-in-effect` 위반이 모두 제거된다 (총 9건 — `CertTerminal.tsx` 2건 포함).
2. 각 변경에 사유가 있다 — 코드의 자기설명 또는 `eslint-disable-next-line ... -- {사유}` 주석.
3. `npm run lint` 결과에서 `set-state-in-effect` 카운트가 9건 감소한다.
4. `npm run build` 통과 (회귀 없음).
5. 변경한 컴포넌트의 시각적 동작이 동일하다 — 로딩 상태, 첫 렌더 깜빡임, navigation 후 상태가 같음. (최소 dev 서버에서 해당 페이지 진입·재진입 1회씩 확인)

## 금지 사항

- 모든 위반을 일괄 `eslint-disable`로 끄지 마라. 이유: 룰이 잡으려는 cascading render 위험은 실재한다. 패턴 A–D 중 하나로 *구조적 변경*이 우선.
- `useEffect` 안의 setState를 `setTimeout(0)`이나 `Promise.resolve().then()`으로 미루지 마라. 이유: 룰을 우회할 뿐 같은 cascading render 비용 발생.
- 컴포넌트 props 시그니처를 바꾸지 마라. 이유: 호출부 영향이 step 범위를 넘는다.
- API 호출 위치/타이밍을 바꾸지 마라. 이유: 네트워크 호출량/순서가 달라지면 회귀 위험.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

성공 조건:
- lint output에서 `react-hooks/set-state-in-effect` 9건이 사라진다 (남은 위반은 step 2 범위).
- impure-function-during-render는 step 3 영역이라 그대로 남아 있어도 OK.
- build 통과.

## Status 규칙

- 성공: `phases/fix-react19-set-state-in-effect/index.json`의 step 1 status를 `completed`로, summary에 "components 8개 파일 9건 정리, 사용 패턴 분포(A:n, B:n, C:n, D:n)" 기록.
- 실패: 3회 시도 후 lint 회귀(다른 룰 새 위반) 또는 build 깨짐 시 `error` + `error_message`.
- blocked: 컴포넌트 의도 파악이 props/상위 호출부 분석을 요구하면 `blocked` + `blocked_reason: "{파일명} 의도가 호출부 컨텍스트 필요 — 사용자 확인 필요"`.
