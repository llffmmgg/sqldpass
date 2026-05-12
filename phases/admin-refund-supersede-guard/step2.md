# Step 2 — 프론트엔드: supersede 행 disable + 시각 배지

## 배경

Step 1 의 백엔드가 AdminPaymentRow 에 `supersededByNewerPayment: boolean` 을 채워 반환. 프론트가 이 필드로 환불 버튼 disable + 사유 툴팁 + (선택) 작은 회색 배지로 운영자에게 명확히 안내.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/lib/adminApi.ts` | `AdminPaymentRow` interface 에 `supersededByNewerPayment: boolean` 추가 |
| `src/app/admin/refunds/page.tsx` | `PaymentRow` 의 `refundable` 조건 확장 + `getDisabledReason` 분기 + 행에 "업그레이드 대체" 배지 |

## adminApi.ts 변경

```diff
  export interface AdminPaymentRow {
    id: number;
    ...
    paidAt: string | null;
    createdAt: string;
+   supersededByNewerPayment: boolean;
  }
```

## page.tsx PaymentRow 변경

refundable 조건:

```diff
  function PaymentRow({ row, onRefund }: { row: AdminPaymentRow; onRefund: () => void }) {
    const cancelled = row.status === "CANCELLED";
-   const refundable = row.status === "PAID" && row.provider === "PORTONE";
+   const refundable =
+     row.status === "PAID" &&
+     row.provider === "PORTONE" &&
+     !row.supersededByNewerPayment;
    const disabledReason = !refundable ? getDisabledReason(row) : null;
```

getDisabledReason 분기 추가 (PLAY_BILLING 분기 다음):

```diff
  function getDisabledReason(row: AdminPaymentRow): string {
    if (row.status === "CANCELLED") return "이미 환불됨";
    if (row.status === "FAILED") return "결제 실패 — 환불 대상 아님";
    if (row.status === "PENDING") return "결제 미완료 — 환불 대상 아님";
    if (row.provider === "PLAY_BILLING") {
      return "Play Billing 은 Google RTDN 으로 자동 환불 처리 — 어드민 수동 환불 금지";
    }
+   if (row.supersededByNewerPayment) {
+     return "이 결제는 이후 업그레이드 결제로 대체됨 — 현재 활성 결제부터 환불해주세요";
+   }
    return "환불 불가";
  }
```

## superseded 배지 추가 (상태 컬럼 옆)

상태 셀에 배지 옆에 작은 회색 "업그레이드 대체" 추가:

```diff
  <DataTable.Cell align="right">
    <span
      className={`inline-flex rounded-full border px-2 py-0.5 text-[11px] font-medium ${STATUS_CHIP[row.status]}`}
    >
      {STATUS_LABEL[row.status]}
    </span>
+   {row.supersededByNewerPayment && (
+     <span className="ml-1.5 inline-flex rounded-full border border-zinc-500/40 bg-zinc-500/10 px-2 py-0.5 text-[10px] text-zinc-400">
+       업그레이드 대체
+     </span>
+   )}
  </DataTable.Cell>
```

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동:
- /admin/refunds → 업그레이드 회원 검색 → 옛 결제 행: 환불 버튼 disable + 툴팁 "업그레이드로 대체됨" + "업그레이드 대체" 배지
- 같은 회원 최신 결제 행: 활성 + 배지 없음
- 최신 환불 → 옛 결제는 이제 supersededByNewerPayment=false → 활성

## Acceptance Criteria

1. `AdminPaymentRow` 타입에 `supersededByNewerPayment: boolean` 추가.
2. PaymentRow refundable 조건이 PAID + PORTONE + !supersededByNewerPayment.
3. getDisabledReason 의 superseded 분기 PLAY_BILLING 다음, return "환불 불가" 전 위치.
4. superseded 행 상태 셀에 작은 회색 "업그레이드 대체" 배지 표시.
5. lint 0 errors + build 성공.

## 금지 사항

- 환불 버튼 disable 만 두고 사유 툴팁/배지 생략하지 마라. **이유**: 운영자가 추측하면 PortOne 콘솔 직접 취소 사고 유발.
- 프론트에서 supersededByNewerPayment 를 자체 계산하지 마라. **이유**: 백엔드 single source of truth. 페이지네이션상 일부 row 만 보유.
- supersededByNewerPayment 행을 목록에서 숨기지 마라. **이유**: 운영자가 전체 결제 흐름 + 환불 이력 파악해야 함.
- 배지 텍스트를 "환불 불가" 같이 모호하게 두지 마라. **이유**: "업그레이드 대체" 가 정확한 상태 묘사 + 운영자가 어떤 결제를 환불해야 할지 즉시 파악.

## Status 규칙

- 성공: step 2 `completed`, summary "AdminPaymentRow supersededByNewerPayment + PaymentRow refundable 조건 + getDisabledReason 분기 + 업그레이드 대체 배지, lint/build OK".
- 실패: 3회 재시도 후 `error`.
