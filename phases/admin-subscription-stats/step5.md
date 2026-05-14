# Step 5 — 프론트엔드: 구독 통계 그래프 패널

## 배경

Step 2 의 `/api/admin/stats/revenue`, `/api/admin/stats/revenue/by-plan` 을 사용해 `/admin/subscriptions` 상단에 매출/환불 추이 라인 차트 + 플랜별 분포 막대 차트 + KPI 카드 3장을 표시. 외부 차트 라이브러리는 사용하지 않고 기존 인하우스 SVG 패턴(`src/components/admin/TrendChart.tsx`) 을 복제·확장.

archived 구독은 백엔드에서 이미 제외되므로 프론트는 받아온 데이터만 그대로 시각화.

## 의존성

- Step 2 (`backend-revenue-stats-api`) 완료 필수.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/lib/plan-tokens.ts` (신규) | SubscriptionPlan 별 색·라벨 토큰. cert-tokens 패턴 차용. |
| `src/lib/adminApi.ts` | `RevenuePoint`, `RevenueByPlan` 타입 + `fetchRevenueStats(days)`, `fetchRevenueByPlan(days)` 함수. |
| `src/components/admin/SubscriptionStatsPanel.tsx` (신규) | 기간 토글 + StatCard 3개 + 라인 차트 + 막대 차트. |
| `src/app/admin/subscriptions/page.tsx` | `<SubscriptionStatsPanel />` 를 `PageHeader` 직후·검색 input 직전에 삽입. |

## plan-tokens.ts

```ts
// src/lib/plan-tokens.ts
// 색 계열은 cert-tokens 패턴과 일관 — 단단한 Tailwind 토큰만. opacity 변형은 사용처에서.

export type SubscriptionPlanKey = "THREE_DAY" | "FOCUS" | "ONE_MONTH" | "UNLIMITED";

export const PLAN_TOKENS: Record<SubscriptionPlanKey, {
  label: string;
  short: string;
  bar: string;        // 막대 채움
  dot: string;        // 도트/배지
  text: string;       // 텍스트 강조
}> = {
  THREE_DAY: { label: "Thunder",  short: "3일",  bar: "bg-amber-500",   dot: "bg-amber-500",   text: "text-amber-300" },
  FOCUS:     { label: "Focus",    short: "30일", bar: "bg-sky-500",     dot: "bg-sky-500",     text: "text-sky-300" },
  ONE_MONTH: { label: "Pro",      short: "30일", bar: "bg-violet-500",  dot: "bg-violet-500",  text: "text-violet-300" },
  UNLIMITED: { label: "All Pass", short: "무기한", bar: "bg-emerald-500", dot: "bg-emerald-500", text: "text-emerald-300" },
};
```

> admin/subscriptions/page.tsx 의 `PLAN_LABEL`/`PLAN_CHIP` 와 색이 동일하도록 정렬: THREE_DAY=amber, FOCUS=sky, ONE_MONTH=violet, UNLIMITED=emerald.

## adminApi.ts 추가

```ts
export interface RevenuePoint {
  date: string;          // "YYYY-MM-DD"
  revenue: number;
  refundAmount: number;
  count: number;
}

export interface RevenueByPlan {
  plan: SubscriptionPlanKey;
  count: number;
  revenue: number;
}

export function fetchRevenueStats(days: number) {
  return adminFetch<RevenuePoint[]>(`/stats/revenue?days=${days}`);
}

export function fetchRevenueByPlan(days: number) {
  return adminFetch<RevenueByPlan[]>(`/stats/revenue/by-plan?days=${days}`);
}
```

## SubscriptionStatsPanel.tsx (신규)

핵심 구조:

```tsx
"use client";

import { useEffect, useState } from "react";
import { fetchRevenueStats, fetchRevenueByPlan, type RevenuePoint, type RevenueByPlan } from "@/lib/adminApi";
import { PLAN_TOKENS, type SubscriptionPlanKey } from "@/lib/plan-tokens";

const RANGES = [7, 30, 90] as const;
type Range = typeof RANGES[number];

export default function SubscriptionStatsPanel() {
  const [days, setDays] = useState<Range>(30);
  const [points, setPoints] = useState<RevenuePoint[]>([]);
  const [byPlan, setByPlan] = useState<RevenueByPlan[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([fetchRevenueStats(days), fetchRevenueByPlan(days)])
      .then(([rev, plan]) => { setPoints(rev); setByPlan(plan); })
      .catch(() => { setPoints([]); setByPlan([]); })
      .finally(() => setLoading(false));
  }, [days]);

  const totalRevenue = points.reduce((s, p) => s + p.revenue, 0);
  const totalRefund  = points.reduce((s, p) => s + p.refundAmount, 0);
  const totalCount   = points.reduce((s, p) => s + p.count, 0);

  return (
    <section className="mb-6 rounded-xl border border-border bg-surface p-5">
      {/* 기간 토글 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-sm font-semibold">매출 통계</h2>
        <div className="flex gap-1 rounded-md border border-border p-1">
          {RANGES.map((r) => (
            <button
              key={r}
              onClick={() => setDays(r)}
              className={`rounded px-2 py-1 text-xs font-medium ${
                days === r ? "bg-primary text-primary-fg" : "text-text-muted hover:text-text"
              }`}
            >
              {r}일
            </button>
          ))}
        </div>
      </div>

      {/* KPI 카드 3개 */}
      <div className="grid grid-cols-3 gap-3">
        <StatCard label="총 매출" value={`₩${totalRevenue.toLocaleString()}`} />
        <StatCard label="환불액" value={`₩${totalRefund.toLocaleString()}`} tone="danger" />
        <StatCard label="결제 건수" value={`${totalCount}건`} />
      </div>

      {/* 라인 차트 — 매출(primary) + 환불(danger) */}
      <div className="mt-5">
        <RevenueLineChart points={points} loading={loading} />
      </div>

      {/* 플랜별 막대 차트 */}
      <div className="mt-5">
        <PlanBarChart data={byPlan} loading={loading} />
      </div>
    </section>
  );
}
```

`RevenueLineChart`, `PlanBarChart`, `StatCard` 는 같은 파일 안에 함수 컴포넌트로 두거나 `components/admin/charts/` 하위로 분리(agent 선택). 단단한 톤 유지:
- 라인 차트: SVG path, primary 색 = 매출, red-400(또는 danger 토큰) = 환불. 영역 fill 없이 단순 stroke 만. **`animate-pulse` / `drop-shadow` 글로우 금지**.
- 막대 차트: 가로 막대 또는 세로 막대. 각 plan 의 색은 `PLAN_TOKENS[plan].bar`. 옆에 `count + ₩revenue` 라벨.
- StatCard: `border border-border bg-bg-elevated rounded-lg p-3`. tone="danger" 면 글자 색만 danger 토큰. 배경은 그대로 단단하게.

빈/로딩 상태:
- 로딩: `bg-surface-hover h-32 rounded` 스켈레톤. **animate-pulse 금지** — 정적 회색 박스.
- 빈: "최근 N일간 결제 없음" 한 줄.

## admin/subscriptions/page.tsx 삽입

```diff
  return (
    <div>
      <PageHeader ... />
+     <SubscriptionStatsPanel />
      <div className="mb-4 flex items-center gap-2">
        <input ... 닉네임 검색 />
```

import 한 줄 추가: `import SubscriptionStatsPanel from "@/components/admin/SubscriptionStatsPanel";`

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동(dev 서버):
1. `/admin/subscriptions` 상단에 새 통계 패널 노출.
2. 기간 토글 7/30/90 클릭 시 카드/차트 즉시 갱신.
3. 라인 차트에 매출(primary) 라인 + 환불(danger) 라인 2개.
4. 막대 차트에 plan 별 막대 4개(THREE_DAY/FOCUS/ONE_MONTH/UNLIMITED), 색이 page.tsx PLAN_CHIP 과 동일 계열.
5. Step 4 에서 archive 한 admin 본인 결제는 그래프에서 **빠진 채로** 보임.
6. 데이터 없으면 "결제 없음" 안내.

## Acceptance Criteria

1. `plan-tokens.ts` 신규, page.tsx PLAN_LABEL/PLAN_CHIP 와 색 일관.
2. `adminApi.ts` 에 `fetchRevenueStats`, `fetchRevenueByPlan` 추가 + 타입 export.
3. `SubscriptionStatsPanel` — 기간 토글, KPI 3장, 라인 차트(매출+환불), 막대 차트(plan 별).
4. `/admin/subscriptions` 페이지에 패널 삽입.
5. `animate-pulse`, `drop-shadow-[...]`, backdrop-blur 등 흐릿한 효과 미사용(메모리 규칙).
6. `npm run lint` 0 error, `npm run build` 성공.

## 금지 사항

- 외부 차트 라이브러리(Recharts, Chart.js, Tremor 등)를 npm install 하지 마라. **이유**: 번들 크기 + 기존 패턴(인하우스 SVG, TrendChart) 일관성.
- `animate-pulse`, `drop-shadow-[0_0_Xpx_...]`, `backdrop-blur-*` 사용하지 마라. **이유**: 메모리 [[feedback-no-ai-blur-effects]] 규칙 — AI 스러운 효과 금지.
- 옅은 옅은 색 배경(`bg-primary/5`, `bg-amber-300/10` 등 4~10% opacity)을 카드 채움으로 쓰지 마라. **이유**: 같은 규칙 — `bg-surface` / `bg-bg-elevated` 단단한 토큰 사용.
- 플랜 라벨/색을 page.tsx 와 다르게 정의하지 마라. **이유**: 같은 화면에서 두 가지 라벨이 보이면 운영자 혼선. `plan-tokens.ts` 를 단일 진실원으로.
- archived 필터를 프론트에서 추가하지 마라. **이유**: Step 2 백엔드가 이미 제외. 중복 필터는 혼선 + 향후 includeArchived 옵션 도입 시 회귀.

## Status 규칙

- 성공: step 5 `completed`, summary "plan-tokens + adminApi fetchRevenueStats/ByPlan + SubscriptionStatsPanel(라인+막대+KPI3) + /admin/subscriptions 패널 삽입, lint/build OK".
- 실패: 3회 재시도 후 `error`.
