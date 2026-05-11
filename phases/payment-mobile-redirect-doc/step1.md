# Step 1 — 모바일 redirectUrl + 복귀 처리

## 배경

PortOne 통합 점검(10대 항목)에서 발견된 갭 #1.

데스크탑 브라우저는 PortOne 결제창이 같은 탭 iframe/팝업으로 열려 `requestPayment()` Promise 가 그대로 resolve → JS 가 곧바로 `/api/payment/verify` 호출. `redirectUrl` 불필요.

모바일 브라우저는 카카오페이 → 카카오톡 앱으로 점프, KG이니시스 카드 → ISP/안전결제 앱으로 점프하는 흐름이 잦다. 외부 앱에서 sqldpass 브라우저로 돌아올 때 PortOne 은 `redirectUrl` 로 이동시키는데, 현재 `frontend/src/lib/payment.ts:213-220` 의 `baseArgs` 에 `redirectUrl` 필드가 없다 → 복귀 흐름 깨져 사용자가 결제했는데도 verify 가 호출 안 됨 → 권한 미부여 (RTDN/웹훅으로 결국 복구되긴 하나 사용자 경험 깨짐).

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/lib/payment.ts` (L213-220, +export) | `baseArgs.redirectUrl` 추가, `verifyPaymentById(paymentId)` export |
| `frontend/src/app/checkout/CheckoutClient.tsx` | `useSearchParams` 로 `?paymentId=...` 감지 → verify → 토스트 → `router.replace("/checkout")` 로 쿼리 제거 후 `/mock-exams` 이동 |

## payment.ts 변경

`baseArgs` 객체에 redirectUrl 한 줄:

```ts
const baseArgs = {
  storeId: STORE_ID,
  channelKey,
  paymentId: prepared.paymentId,
  orderName: prepared.productName,
  totalAmount: prepared.amount,
  currency: "CURRENCY_KRW" as const,
  redirectUrl:
    typeof window !== "undefined"
      ? `${window.location.origin}/checkout?paymentId=${encodeURIComponent(prepared.paymentId)}`
      : undefined,
};
```

KAKAOPAY 와 CARD 양쪽 모두 적용 (분기 위쪽 `baseArgs` 에 추가하므로 자동).

`verifyPaymentById` export 신설 — 기존 `authFetch` 는 파일 내부 함수라 CheckoutClient 에서 직접 못 쓰니, 의도 명확한 헬퍼로 노출:

```ts
export async function verifyPaymentById(paymentId: string): Promise<VerifyResponse> {
  return authFetch<VerifyResponse>("/api/payment/verify", {
    method: "POST",
    body: JSON.stringify({ paymentId }),
  });
}
```

## CheckoutClient.tsx 변경

`useSearchParams` import (next/navigation), `verifyPaymentById` import (payment.ts), `planLabel` 은 이미 import 됨.

```tsx
import { useSearchParams } from "next/navigation";
import { verifyPaymentById, /* 기타 기존 */ } from "@/lib/payment";

// CheckoutContent 안:
const searchParams = useSearchParams();
const returnedPaymentId = searchParams.get("paymentId");

useEffect(() => {
  if (!returnedPaymentId) return;
  verifyPaymentById(returnedPaymentId)
    .then((result) => {
      toast.show(`${planLabel(result.plan)} 결제 완료`, "success");
      router.replace("/checkout");
      setTimeout(() => router.push("/mock-exams"), 800);
    })
    .catch((e) => {
      const message = e instanceof Error ? e.message : "";
      const cancelled = /취소|cancel/i.test(message);
      toast.show(
        cancelled ? "결제를 취소하셨습니다." : (message || "결제 검증 실패"),
        cancelled ? "info" : "error",
      );
      router.replace("/checkout");
    });
}, [returnedPaymentId]);
```

`router.replace("/checkout")` 로 쿼리 제거 → 새로고침 시 verify 가 다시 안 돈다. (다만 verify 멱등 보증이라 돌더라도 안전.)

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

### (옵션) 수동 검증

- 데스크탑 크롬: `/checkout` → 카카오페이 결제 → 기존 흐름 변경 0. (Promise resolve → verify 호출, redirectUrl 미사용.)
- 모바일 시뮬레이션(DevTools iPhone) 또는 실 모바일: 카카오페이/카드 결제 → 외부 앱 전환 후 복귀 → `/checkout?paymentId=...` 진입 → 토스트 "결제 완료" + `/mock-exams` 이동 → 주소창 쿼리 사라짐 확인.

## Acceptance Criteria

1. `payment.ts` `baseArgs` 에 `redirectUrl` 필드 추가됨 (KAKAOPAY / CARD 양쪽).
2. `verifyPaymentById(paymentId)` 가 export 된다.
3. `CheckoutClient` 가 `useSearchParams().get("paymentId")` 로 복귀 케이스를 감지한다.
4. 복귀 verify 성공 시 success 토스트 + `/mock-exams` 라우팅, 실패 시 error/info 토스트.
5. verify 후 `router.replace("/checkout")` 로 쿼리가 제거된다.
6. 기존 `onPay` 플로우 (Promise resolve → verify) 시그니처 변경 없음.
7. `npm run lint` 0 errors, `npm run build` 성공.

## 금지 사항

- 백엔드(`backend/`)를 수정하지 마라. 이유: verify endpoint 와 멱등성은 payment-test-coverage step3 에서 이미 보증됨. 모바일 복귀는 같은 endpoint 재호출이라 백엔드 변경 불필요.
- `authFetch` 를 export 하지 마라. 이유: 너무 광범위한 헬퍼라 의도가 흐려진다. `verifyPaymentById` 같은 도메인 함수로만 노출.
- redirectUrl 에 `prepared.amount` 또는 `plan` 같은 추가 쿼리를 넣지 마라. 이유: 클라이언트 변조 가능 값을 URL 에 넣으면 서버 재검증 의미가 흐려진다. paymentId 만으로 백엔드 DB 에서 모든 정보 조회 가능.
- 복귀 후 `router.replace` 전에 verify 가 끝나기를 기다리지 않고 즉시 replace 하지 마라. 이유: verify 실패 시 사용자에게 에러 토스트를 보여줘야 한다.
- Capacitor 앱 분기(`isCapacitorApp()`) 를 건드리지 마라. 이유: Play Billing 은 redirectUrl 무관. 본 변경은 웹 전용.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "payment.ts redirectUrl + verifyPaymentById export, CheckoutClient 복귀 처리, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
