# Step 2 — Quota 사전 표시 헤더

## 배경

`GET /api/quota` 응답을 받아 문제 탭과 모의고사 탭 진입 시 헤더 영역에 "오늘 18 / 30 문제" 형태로 출력. 사용자가 한도 도달 전에 미리 인지할 수 있게 함.

**원칙**: 클라이언트는 값을 계산하지 않는다. 서버가 준 `questionUsed`, `questionLimit`, `mockUsed`, `mockLimit` 을 그대로 출력. `*Limit` 이 null 이면 활성 구독자 → 표시 자체를 숨김.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

신규/수정 파일:

| 파일 | 변경 |
|------|------|
| 신규 `frontend/src/lib/quotaApi.ts` | GET /api/quota fetch + 타입 정의 |
| 신규 `frontend/src/components/QuotaBadge.tsx` | "오늘 18 / 30" 표시 컴포넌트 |
| 수정 `frontend/src/app/solve/page.tsx` 또는 동등 라우트 | 헤더에 QuotaBadge 마운트 |
| 수정 `frontend/src/app/mini-mock-exams/page.tsx`, `frontend/src/app/mock-exams/page.tsx` | 동일 |

기존 `frontend/src/components/BottomTabBar.tsx` 의 탭 정의는 변경하지 마라 (정책 변경 없음).

## quotaApi.ts 작성 가이드

```ts
export type Quota = {
  questionUsed: number;
  questionLimit: number | null;
  mockUsed: number;
  mockLimit: number | null;
  resetAt: string;  // KST naive ISO from backend
};

export async function fetchQuota(): Promise<Quota> {
  const res = await apiFetch("/api/quota");
  return res.json();
}
```

`resetAt` 은 백엔드가 KST naive 로 보냄(`2026-05-22T00:00:00`). 프론트에서 표시할 때 `+09:00` 부착 — 메모리 `project_kst_naive_serialization` 준수.

## QuotaBadge 작성 가이드

```tsx
"use client";
export function QuotaBadge({ kind }: { kind: "question" | "mock" }) {
  const [quota, setQuota] = useState<Quota | null>(null);
  useEffect(() => { fetchQuota().then(setQuota); }, []);
  if (!quota) return null;
  const limit = kind === "question" ? quota.questionLimit : quota.mockLimit;
  if (limit === null) return null;  // 활성 구독자 - 숨김
  const used = kind === "question" ? quota.questionUsed : quota.mockUsed;
  const label = kind === "question" ? "문제" : "모의고사";
  return <span className="...">오늘 {used} / {limit} {label}</span>;
}
```

스타일은 기존 토큰 유지. 메모리 `feedback_color_token_changes`, `feedback_no_ai_blur_effects` 준수.

탭 진입마다 새 quota fetch — 사용자가 풀이 완료 후 탭 복귀 시 카운트가 업데이트되어야 함. SWR 또는 단순 useEffect 모두 OK.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

dev server:
1. 무료 회원으로 /solve 진입 → "오늘 0 / 30 문제" 표시
2. 문제 풀이 후 다시 진입 → 카운트 증가 반영
3. /mock-exams 진입 → "오늘 0 / 1 모의고사" 표시
4. 활성 구독 시드로 로그인 → 두 위치 모두 표시 안 됨

## Acceptance Criteria

1. quotaApi.ts, QuotaBadge.tsx 신규.
2. /solve, /mock-exams, /mini-mock-exams 헤더에 QuotaBadge 마운트.
3. `*Limit` null 시 표시 숨김.
4. `npm run lint`, `npm run build` 통과.

## 금지 사항

- 클라이언트에서 자체로 카운트 계산·증가하지 마라. 이유: 백엔드 단일 진실 소스.
- 새 색 계열 도입 금지(feedback_color_token_changes).
- 기출복원 탭(/past-exams)에 QuotaBadge 마운트 금지. 이유: 기출은 무제한 정책.
- BottomTabBar 의 탭 구성을 변경하지 마라. 이유: 정책 변경 아님.

## Status 규칙

- 성공: step 2 `completed`. phase 전체 완료 표시.
- 실패: 3회 후 `error`.
