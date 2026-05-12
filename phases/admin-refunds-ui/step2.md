# Step 2 — 프론트엔드: /admin/refunds 페이지 + 모달 + sidebar

## 배경

Step 1 의 `GET /api/admin/payments` 와 기존 `POST /api/admin/payments/{id}/refund` 를 활용해 어드민 환불 화면을 만든다. UX/정합성/예외 갭 8건 반영.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/lib/adminApi.ts` | `AdminPaymentRow` 타입 + `AdminPaymentPage` + `listAdminPayments` + `refundAdminPayment(id, reason, signal)` |
| `src/app/admin/refunds/page.tsx` (신규) | 결제 목록 페이지 — 필터(검색, status, provider) + DataTable + 페이지네이션 + 행별 환불 버튼 + 모달 트리거 |
| `src/app/admin/refunds/RefundReasonModal.tsx` (신규) | 사유 입력 모달 — 5~200자 검증 + busy 가드 + 30s AbortController timeout + 에러 안내 |
| `src/app/admin/layout.tsx` | sidebar "결제" 섹션에 "환불 관리" 메뉴 추가 |

## adminApi 추가

```ts
export type AdminPaymentStatus = "PENDING" | "PAID" | "FAILED" | "CANCELLED";
export type AdminPaymentProvider = "PORTONE" | "PLAY_BILLING";

export interface AdminPaymentRow {
  id: number;
  paymentId: string;
  memberId: number;
  nickname: string | null;
  plan: AdminSubscriptionPlan | null;
  amount: number;
  baseAmount: number;
  prorateDiscount: number;
  status: AdminPaymentStatus;
  provider: AdminPaymentProvider;
  buyerName: string | null;
  buyerEmail: string | null;
  buyerPhoneNumber: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface AdminPaymentPage {
  content: AdminPaymentRow[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export function listAdminPayments(opts: {
  page?: number; size?: number;
  status?: AdminPaymentStatus; provider?: AdminPaymentProvider;
  nickname?: string; paymentId?: string;
} = {}) { ... return adminFetch<AdminPaymentPage>(`/payments?${params}`); }

export function refundAdminPayment(id: number, reason: string, signal?: AbortSignal) {
  return adminFetch<void>(`/payments/${id}/refund`, {
    method: "POST",
    body: JSON.stringify({ reason }),
    signal,
  });
}
```

`adminFetch` 가 `signal` 을 fetch 에 그대로 전달하도록 `options?: RequestInit` 시그니처라 변경 불필요 (RequestInit 에 `signal` 포함).

## RefundReasonModal 사양

Props: `{ payment: AdminPaymentRow; onClose: () => void; onConfirmed: () => void }`

- 결제 요약 — 회원 닉네임 (`?? "(탈퇴 회원 #" + memberId + ")"`), plan(label), `₩amount.toLocaleString()`, paymentId
- textarea 사유 입력 — 최소 5자 / 최대 200자, 인라인 에러
- "환불 처리" 버튼 (5자 이상이면 활성, busy 시 disable + spinner)
- "취소" 버튼 (busy 시 disable)
- ESC / overlay / X 닫기 — **busy 중에는 차단**
- 30초 timeout (`AbortController` + `setTimeout`) → 타임아웃 시 토스트 "PG 응답 지연 — 잠시 후 결제 목록을 새로고침해주세요" + 모달 닫기
- 에러 catch 시 토스트 "환불에 실패했습니다. PortOne 콘솔에서 결제 상태를 확인하고 운영자에게 보고해주세요." + 모달 유지 (재시도 가능)

## /admin/refunds 페이지

state: `data`, `loading`, `page`, `search`, `statusFilter`, `providerFilter`, `refundTarget`.

`reload()`:
- `search` 가 `sqldpass-` 로 시작하면 `paymentId` 필터로, 아니면 `nickname` 으로 분기
- `listAdminPayments({ page, size:20, status, provider, paymentId|nickname })` 호출

DataTable 컬럼:
| 결제일 | 회원 | Plan | 금액 | 상태 | 결제수단 | 액션 |

- **회원**: nickname ?? "(탈퇴 회원 #" + memberId + ")"
- **금액**: `₩` + `amount.toLocaleString()`. `prorateDiscount > 0` 이면 "(원가 ₩baseAmount)" 작게
- **상태**: PaymentStatus 한국어 배지. CANCELLED 는 회색 + "환불됨"
- **결제수단**: PORTONE → "PortOne", PLAY_BILLING → "Play 결제"
- **buyer 정보**: 행 펼침은 없음. 모달에서 표시. 별도 컬럼은 안 둠 (테이블 폭 절약)
- **CANCELLED 행 시각**: `opacity-60` 클래스
- **액션**:
  - status=PAID + provider=PORTONE → "환불" 버튼 활성, 클릭 시 `setRefundTarget(row)` → 모달 열림
  - 그 외 → disable + 툴팁:
    - CANCELLED: "이미 환불됨"
    - FAILED/PENDING: "환불 불가 (결제 미완료)"
    - PLAY_BILLING: "Play Billing 은 RTDN 자동 환불"

페이지네이션: subscriptions/page.tsx 와 동일 패턴.

## sidebar 추가

`src/app/admin/layout.tsx` SIDEBAR_SECTIONS 의 "결제" 섹션:

```diff
  {
    label: "결제",
    items: [
      { href: "/admin/subscriptions", label: "구독 관리", icon: ICON.subscriptions },
+     { href: "/admin/refunds", label: "환불 관리", icon: ICON.subscriptions },
    ],
  },
```

(별도 아이콘 추가 없이 기존 subscriptions 아이콘 재사용.)

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동:
- /admin/login → 사이드바 "환불 관리" → 결제 목록 표시.
- 필터 — status/provider 변경 시 reload.
- "sqldpass-1234" 입력 → paymentId 필터로 동작.
- "닉네임" 입력 → nickname 필터.
- PAID+PORTONE 행 환불 버튼 클릭 → 모달 → 5자 이상 사유 입력 → 환불 처리.
- 모달 busy 중 ESC/overlay/X 클릭 무효.
- 사유 4자 → "5자 이상" 에러. 201자 → "200자 이내".
- CANCELLED 행 환불 버튼 disable + 툴팁.

## Acceptance Criteria

1. `/admin/refunds` 페이지가 sidebar 에서 접근 가능, 어드민 미인증 시 로그인 리다이렉트.
2. 결제 목록 + 페이지네이션 + 3종 필터 동작.
3. 검색 input 이 `sqldpass-` 접두사로 paymentId/nickname 자동 분기.
4. RefundReasonModal — 5~200자 검증 + busy 가드 + 30초 timeout + 에러 안내 강화.
5. PAID + PORTONE 만 환불 버튼 활성, 나머지 disable + 툴팁.
6. CANCELLED 행 opacity-60 + "환불됨" 라벨.
7. nickname null 인 결제는 "(탈퇴 회원 #N)" 표시. buyer null 은 모달에서 "–".
8. 환불 성공 시 토스트 + 목록 reload.
9. `npm run lint` 0 errors, `npm run build` 성공.

## 금지 사항

- 모달 안에서 직접 `/api/admin/payments/...refund` 호출하지 마라. **이유**: adminApi.refundAdminPayment 단일 진입점.
- busy 중 모달 닫기를 허용하지 마라. **이유**: setState on unmounted component 경고 + 응답 결과 안내 누락.
- Play Billing 행을 목록에서 숨기지 마라. **이유**: 전체 결제 흐름을 봐야 함. disable + 안내가 정답.
- buyer 정보 마스킹을 적용하지 마라. **이유**: 어드민 화면 + CS 원본 필요. 별도 phase.
- 페이지 size 를 100 초과로 요청하지 마라. **이유**: 백엔드 가드와 일관성.
- nickname null 에 빈 문자열 표시하지 마라. **이유**: 운영자가 row 식별 못 함 — "(탈퇴 회원 #N)" 명시.

## Status 규칙

- 성공: step 2 `completed`, summary "adminApi listAdminPayments/refundAdminPayment + RefundReasonModal + /admin/refunds 페이지 + sidebar 등록, lint/build OK".
- 실패: 3회 재시도 후 `error`.
