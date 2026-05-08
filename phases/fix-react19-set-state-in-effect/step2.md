# Step 2 — app/ 페이지 3건 + hooks/ 1건 정리

## 배경

step 1에서 `components/` 8건을 처리했고, 남은 `react-hooks/set-state-in-effect` 위반은 페이지·hook 영역 4건.

| 파일 | 라인 | 비고 |
|------|------|------|
| `frontend/src/app/admin/members/page.tsx` | 49 | 관리자 회원 목록 — 데이터 fetch + filter sync |
| `frontend/src/app/auth/callback/google/page.tsx` | 43 | OAuth 콜백 — token 처리 후 redirect |
| `frontend/src/hooks/useSubscription.ts` | 38 | 구독 상태 hook — 비로그인 분기에서 즉시 setState |

**선결 조건**: step 1이 `completed`여야 한다. step 1이 표준 패턴 A–D를 정착시킨 상태에서 같은 패턴을 페이지·hook에 일관 적용한다.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

위 표의 3개 파일 (3개 위반).

## 적용 패턴

step 1의 표준 패턴 A–D를 그대로 사용한다. 파일별 권고:

### `app/admin/members/page.tsx:49`

관리자 페이지는 SSR/CSR 어느 쪽인지 우선 확인 (`"use client"` 헤더 여부). client면 데이터 fetch 패턴 B (alive 가드 + 사유 주석) 또는 SWR/외부 store가 이미 있으면 패턴 C가 적합.

### `app/auth/callback/google/page.tsx:43`

OAuth 콜백은 한 번 받은 code로 token 교환을 하는 흐름이라 `useEffect`에서 fetch + setState가 자연스럽다. **패턴 B**(alive 가드 + 사유 주석)이 정공. 인증 실패 시 redirect도 effect 안에서 처리.

### `hooks/useSubscription.ts:38`

비로그인 분기에서 `setSubscription(INACTIVE)` + `setLoading(false)`를 effect 본문에서 동기 호출. 비로그인이라는 사실이 동기로 결정되므로 **패턴 A** (lazy init) 또는 **패턴 D** (effect 자체 제거)가 가능. 단 후속 logout/login 이벤트로 상태가 바뀌어야 한다면 패턴 C가 적합 — `useSyncExternalStore`로 auth 상태를 구독.

## Acceptance Criteria

1. 위 3개 파일의 `react-hooks/set-state-in-effect` 위반이 모두 제거된다.
2. 각 변경에 사유가 있다 — 자기설명 또는 `eslint-disable-next-line ... -- {사유}` 주석.
3. `npm run lint` 결과에서 `set-state-in-effect` 카운트가 step 2 범위인 3건 추가 감소한다 (step 1 + step 2 = 12건 모두 제거).
4. `npm run build` 통과.
5. 동작 검증:
   - `/admin/members`: 데이터 로딩 → 첫 렌더 → filter 변경 시 정상 동작
   - `/auth/callback/google?code=...`: 콜백 진입 → token 처리 → 리다이렉트 동일
   - 로그인/비로그인 토글 시 `useSubscription`이 `INACTIVE` ↔ active 전환 정상

## 금지 사항

- step 1과 다른 패턴 사용을 임의로 도입하지 마라. 이유: 코드베이스의 useEffect 처리가 4가지로 갈라지면 후속 유지보수가 어려워진다.
- OAuth 콜백 라우팅 흐름을 바꾸지 마라. 이유: SSR/CSR 분기, 토큰 저장 위치(`localStorage[user_token]`), redirect 타이밍이 백엔드와 합의된 계약이다.
- `useSubscription`의 반환 시그니처(`{ subscription, loading, ... }`)를 바꾸지 마라. 이유: 호출부 영향 큰 broad 변경.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

성공 조건:
- `set-state-in-effect` 위반 0건.
- impure-function-during-render는 step 3 영역이라 그대로.
- build 통과.

수동 동작 확인 (가능한 범위):
- dev 서버 (`npm run dev`)에서 `/admin/members`(관리자 토큰 필요) 진입·필터·refresh 1회.
- 로그아웃 → `/auth/callback/google?code=test`은 실제 호출이 어렵다 — token 교환 로직만 코드 리딩으로 검증.
- 로그인/로그아웃 토글로 `useSubscription` 변화 확인.

## Status 규칙

- 성공: `phases/fix-react19-set-state-in-effect/index.json`의 step 2 status를 `completed`로, summary에 "app 3건 + hook 1건 정리, 패턴 분포 + 누적 12/12 처리" 기록.
- 실패: 3회 후 lint 회귀 또는 build 깨짐 시 `error` + `error_message`.
- blocked: OAuth 흐름 / SubscriptionService 계약 변화가 필요해 보이면 `blocked` + 사유.
