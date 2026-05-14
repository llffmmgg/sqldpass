# Step 2 — 프론트엔드: `ActiveSubscription` 타입 동기화

## 배경

Step 1 이 백엔드 `ActiveSubscription` record 에 `allowsPremium: boolean` 필드를 추가한다. 프론트의 동일 이름 타입(`frontend/src/lib/payment.ts`)도 정합 맞춰야 컴파일 안전.

자물쇠 UI(`PassPlusLockNotice`)는 현재 백엔드 403(MOCK_EXAM_LOCKED) 응답 기반이라 본 step 에선 UI 동작 변경 없음 — 타입만 동기화.

## 의존성

- Step 1 (`backend-allows-premium`) 완료 필수.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/lib/payment.ts` (또는 `ActiveSubscription` 정의가 있는 곳) | `ActiveSubscription` 인터페이스에 `allowsPremium: boolean` 필드 추가. 위치는 `hasLibraryAccess` 옆. |

## 코드

`ActiveSubscription` 정의 위치는 `getActiveSubscription` 클라이언트 함수와 가까이 있을 것. grep:
```
ActiveSubscription
```

추가 후:
```ts
export interface ActiveSubscription {
  plan: SubscriptionPlan | null;
  expiresAt: string | null;
  removesAds: boolean;
  allowsPdf: boolean;
  hasLibraryAccess: boolean;
  allowsPremium: boolean; // ← Step 1 백엔드 record 필드와 매칭
  active: boolean;
}
```

> 실제 필드 이름/순서는 기존 정의 그대로 따르고 `allowsPremium` 한 줄만 추가. 다른 곳에서 이 타입을 destructure 하는 코드는 `allowsPremium` 을 안 쓰면 무영향(추가 필드).

## 사용처 점검(읽기만 — 본 step 에서 변경 안 함)

- `useSubscription` 훅이 `ActiveSubscription` 을 그대로 노출하는지 확인. 다음 phase 에서 자물쇠 UI 사전 표시에 쓰일 수 있도록 노출만 해두기.
- 모의고사 목록 카드 `MockExamsClient.tsx` 의 PASS+ 라벨/자물쇠 분기는 본 step 에서 건드리지 않음.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 검증은 Step 1 의 백엔드 변경과 함께 — Focus 회원이 PASS+ 회차 진입 시 403 + `PassPlusLockNotice` 노출되는지 확인.

## Acceptance Criteria

1. `ActiveSubscription` 타입에 `allowsPremium: boolean` 추가.
2. `npm run lint` 0 errors.
3. `npm run build` 성공.
4. 기존 컴포넌트에 컴파일 에러 없음 — `allowsPremium` 미사용 컴포넌트는 무영향.

## 금지 사항

- 자물쇠 UI 동작을 본 step 에서 같이 바꾸지 마라. **이유**: 백엔드 403 기반 기존 흐름이 이미 정확. 사전 표시 개선은 별도 phase.
- `getActiveSubscription()` API 호출 경로/엔드포인트 변경하지 마라. **이유**: Step 1 은 record 필드만 추가 — endpoint URL/형태 동일.
- `useSubscription` 훅 시그니처를 바꾸지 마라. **이유**: 같은 이유 — 본 phase 는 타입 동기화 only.

## Status 규칙

- 성공: step 2 `completed`, summary "ActiveSubscription 타입에 allowsPremium 추가, lint/build OK".
- 실패: 3회 재시도 후 `error`.
