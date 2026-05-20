# Step 3 — 프론트엔드: SubscriptionStatsPanel 채널별 매출 분리

## 배경

Step 2 에서 신설한 `/api/admin/stats/revenue/by-provider` 와 `/revenue/by-provider/by-plan` 을 활용해 어드민 대시보드 `SubscriptionStatsPanel` 에 채널 segmented control 추가. 전체 선택 시 기존 통합 통계, 특정 provider 선택 시 그 채널만 차트.

기존 통합 차트는 그대로 유지하면서 위쪽에 segmented control 한 줄만 추가하는 minimal 변경.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/lib/adminApi.ts` | `RevenueByProviderPoint` / `RevenueByProviderPlan` 타입 + `fetchRevenueByProvider(days)` / `fetchRevenueByProviderAndPlan(days)` 함수 |
| `src/components/admin/SubscriptionStatsPanel.tsx` | 상단에 채널 segmented control (전체 / PortOne / Play / App Store) + 선택 시 fetch 분기. "전체" 는 기존 `/revenue` 그대로, 특정 채널은 `/revenue/by-provider` 결과를 해당 provider 만 필터링. |

## 타입 추가 (adminApi.ts)

```ts
export type RevenueByProviderPoint = {
  date: string;       // YYYY-MM-DD
  provider: string;   // "PORTONE" | "PLAY_BILLING" | "APP_STORE"
  revenue: number;
  refundAmount: number;
  count: number;
};

export type RevenueByProviderPlan = {
  provider: string;
  plan: string;
  count: number;
  revenue: number;
};

export async function fetchRevenueByProvider(days: number): Promise<RevenueByProviderPoint[]> {
  return adminFetch<RevenueByProviderPoint[]>(`/api/admin/stats/revenue/by-provider?days=${days}`);
}

export async function fetchRevenueByProviderAndPlan(days: number): Promise<RevenueByProviderPlan[]> {
  return adminFetch<RevenueByProviderPlan[]>(`/api/admin/stats/revenue/by-provider/by-plan?days=${days}`);
}
```

`adminFetch` 헬퍼는 기존 `fetchRevenueStats` 와 동일 패턴을 따른다.

## 채널 segmented control

`SubscriptionStatsPanel.tsx` 상단의 기간 토글 (7/30/90) 옆 또는 그 아래에:

```tsx
const CHANNEL_OPTIONS = [
  { id: "ALL", label: "전체" },
  { id: "PORTONE", label: "PortOne" },
  { id: "PLAY_BILLING", label: "Play" },
  { id: "APP_STORE", label: "App Store" },
] as const;
type ChannelId = typeof CHANNEL_OPTIONS[number]["id"];

const [channel, setChannel] = useState<ChannelId>("ALL");
```

UI: 기존 기간 segmented control 과 같은 톤. `border-border bg-surface/40 p-1` rounded-full 안에 옵션 4개.

## 데이터 fetch 분기

```tsx
useEffect(() => {
  let cancelled = false;
  setLoading(true);
  (async () => {
    try {
      if (channel === "ALL") {
        const [points, byPlan] = await Promise.all([
          fetchRevenueStats(days),
          fetchRevenueByPlan(days),
        ]);
        if (!cancelled) {
          setPoints(points);
          setByPlan(byPlan);
        }
      } else {
        const [byProv, byProvPlan] = await Promise.all([
          fetchRevenueByProvider(days),
          fetchRevenueByProviderAndPlan(days),
        ]);
        if (!cancelled) {
          // 선택 채널만 필터 + 같은 날짜끼리 합치기
          const filtered = byProv.filter((p) => p.provider === channel);
          setPoints(
            filtered.map((p) => ({
              date: p.date,
              revenue: p.revenue,
              refundAmount: p.refundAmount,
              count: p.count,
            }))
          );
          setByPlan(
            byProvPlan
              .filter((p) => p.provider === channel)
              .map((p) => ({ plan: p.plan, count: p.count, revenue: p.revenue }))
          );
        }
      }
    } catch (e) { ... }
    if (!cancelled) setLoading(false);
  })();
  return () => { cancelled = true; };
}, [days, channel]);
```

기존 차트 컴포넌트 (`StatCard` × 3 + 라인 차트 + 막대 차트) 는 그대로 사용 — points/byPlan state 갱신만 하면 됨.

## 빈 데이터 처리

선택 채널이 해당 기간에 결제 0건이면 `points = []` → 기존 라인 차트가 빈 데이터 안내 표시 (이미 구현됨). 별도 변경 불필요.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동 검증:

1. `/admin/subscriptions` 진입 → 패널 상단에 채널 segmented control 보임.
2. "전체" 선택 시 기존 차트 그대로 (회귀 0).
3. "App Store" 선택 시 iOS 결제만 매출/환불/건수.
4. 기간(7/30/90) 변경 시 채널 선택 유지하면서 데이터만 갱신.

## Acceptance Criteria

1. `adminApi.ts` 에 신규 타입 2종 + 함수 2종 추가.
2. `SubscriptionStatsPanel.tsx` 에 채널 segmented control 노출.
3. "전체" 선택 시 기존 endpoint 호출, 그 외는 by-provider 호출.
4. `npm run lint` 통과 (0 error / 0 warning 신규).
5. `npm run build` 통과.

## 금지 사항

- 기존 `fetchRevenueStats` / `fetchRevenueByPlan` 의 시그니처를 바꾸지 마라. **이유**: 다른 admin 화면에서 import 가능성.
- 차트 컴포넌트 (라인/막대) 의 시각 톤(`bg-primary`, `bg-rose`)을 바꾸지 마라. **이유**: 디자인 시스템 일관성 + 단단한 톤 정책 (animate-pulse / drop-shadow glow 금지).
- "전체" 케이스에서 `/revenue/by-provider` 호출 후 합산하지 마라. **이유**: 기존 `/revenue` 가 더 효율적 (DB 한 row 단위) + 호환성.
- "Other" 채널 옵션 추가하지 마라. **이유**: PaymentProvider enum 이 3가지(PORTONE/PLAY_BILLING/APP_STORE) 로 닫혀 있어 그 외 케이스 없음.

## Status 규칙

- 성공: `completed` + summary "adminApi RevenueByProvider 타입/함수 + SubscriptionStatsPanel 채널 segmented control + provider 필터 fetch 분기, lint/build OK".
- 실패: 3회 재시도 후 `error`.
