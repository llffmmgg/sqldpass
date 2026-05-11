# Step 2 — 프론트엔드: BuyerInfoModal + buyer 전달 + 정리

## 배경

Step 1 에서 백엔드 `PaymentController.PrepareRequest` 가 buyer 정보를 받도록 확장됐다. 프론트는 결제 시점에 모달로 이름/이메일/휴대폰을 수집하고, PortOne SDK `customer` 객체로 전달한다.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

### 이전 phase 정리
| 파일 | 변경 |
|------|------|
| `frontend/src/lib/oauth.ts` | scope 원복 `"openid profile"` |
| `frontend/src/lib/payment.ts` | REAUTH_REQUIRED throw 제거, PrepareResponse.customerEmail 제거, baseArgs.customer 의 email-only 분기 제거 |
| `frontend/src/app/checkout/CheckoutClient.tsx` | REAUTH catch 분기 + clearAuth import 제거 |

### 신규
| 파일 | 변경 |
|------|------|
| `frontend/src/lib/buyerStorage.ts` (신규) | localStorage wrapper — get/set email + phone |
| `frontend/src/components/billing/BuyerInfoModal.tsx` (신규) | 결제 정보 입력 모달 |

### 흐름 변경
| 파일 | 변경 |
|------|------|
| `frontend/src/lib/payment.ts` | startPayment 시그니처에 buyer 필수 추가, prepare body 에 buyer 같이 전달, PortOne SDK customer 객체 (fullName/email/phoneNumber 전부) |
| `frontend/src/app/checkout/CheckoutClient.tsx` | pendingPlan state — onPay 가 모달 열고, BuyerInfoModal onSubmit 콜백에서 실제 startPayment 호출 |

## BuyerInfoModal 컴포넌트

### Props
```ts
type Props = {
  open: boolean;
  plan: SubscriptionPlan | null;
  onClose: () => void;
  onSubmit: (buyer: { name: string; email: string; phoneNumber: string }) => void;
};
```

### 안내 박스 (헤더 아래)

회원 정보 미저장 정책을 사용자에게 명확히 전달. 3단계 구조:

```
문어 CBT 는 회원가입 시 닉네임 외에는 어떤 개인정보도 저장하지 않습니다.

결제 영수증 발송·결제 내역 확인·결제 오류 대응(환불, CS 식별) 을 위해
신용카드 PG(KG이니시스) 가 customer 정보를 요구하므로, 결제 시점에만 아래
정보를 입력받습니다. 입력하신 정보는 결제 기록과 함께 저장되며 회원 정보와는
분리됩니다.
```

스타일: `bg-amber-500/[0.08]` border `border-amber-500/30` 약한 강조 박스 (PASS+ 액센트). "회원가입 시 닉네임 외" font-semibold.

### 입력 필드 + 보조 텍스트

각 라벨 아래에 짧은 사용처 안내:
- 이름: "결제 영수증과 환불·CS 식별에 사용"
- 이메일: "결제 영수증·결제 알림 발송용. 다음 결제 시 자동 채움"
- 휴대폰: "결제 오류 대응 시 연락용. 다음 결제 시 자동 채움"

### 검증

```ts
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^01[0-9][-\s]?\d{3,4}[-\s]?\d{4}$/;

function validateName(v: string): string | null {
  const t = v.trim();
  if (!t) return "이름을 입력해주세요.";
  if (t.length > 50) return "이름은 50자 이내로 입력해주세요.";
  return null;
}
function validateEmail(v: string): string | null {
  if (!v.trim()) return "이메일을 입력해주세요.";
  if (!EMAIL_RE.test(v.trim())) return "이메일 형식이 올바르지 않습니다.";
  return null;
}
function validatePhone(v: string): string | null {
  if (!v.trim()) return "휴대폰 번호를 입력해주세요.";
  if (!PHONE_RE.test(v.trim())) return "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";
  return null;
}
```

### 동작
- 마운트 시 `getStoredBuyerInfo()` → email/phone 자동 채움. name 은 항상 빈 상태.
- 모든 필드 유효해야 "결제 진행" 버튼 enabled.
- "결제 진행" 클릭 → `setStoredBuyerInfo(email, phone)` 호출 → `onSubmit({name, email, phoneNumber})`.
- ESC 키 / overlay 클릭 / X 버튼 → `onClose()`.
- 모달 열린 동안 body scroll lock (`document.body.style.overflow = "hidden"`).

### 패턴 참고

`frontend/src/components/FeedbackModal.tsx` 의 overlay + ESC + scroll lock 구조 재사용.

## buyerStorage.ts

```ts
const EMAIL_KEY = "checkout_buyer_email";
const PHONE_KEY = "checkout_buyer_phone";

export function getStoredBuyerInfo(): { email: string; phoneNumber: string } {
  if (typeof window === "undefined") return { email: "", phoneNumber: "" };
  return {
    email: localStorage.getItem(EMAIL_KEY) ?? "",
    phoneNumber: localStorage.getItem(PHONE_KEY) ?? "",
  };
}

export function setStoredBuyerInfo(email: string, phoneNumber: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(EMAIL_KEY, email);
  localStorage.setItem(PHONE_KEY, phoneNumber);
}
```

## payment.ts 변경

```ts
export type BuyerInfo = {
  name: string;
  email: string;
  phoneNumber: string;
};

export async function startPayment(opts: {
  plan: SubscriptionPlan;
  method?: PaymentMethod;
  buyer: BuyerInfo;
}): Promise<VerifyResponse> {
  if (isCapacitorApp()) return startPaymentPlayBilling(opts.plan);
  return startPaymentPortOne(opts.plan, opts.method ?? "KAKAOPAY", opts.buyer);
}

async function startPaymentPortOne(plan, method, buyer) {
  // ... storeId/channelKey 가드 (REAUTH 분기 제거)
  const prepared = await authFetch<PrepareResponse>("/api/payment/prepare", {
    method: "POST",
    body: JSON.stringify({
      plan,
      buyerName: buyer.name,
      buyerEmail: buyer.email,
      buyerPhoneNumber: buyer.phoneNumber,
    }),
  });

  const PortOne = (await import("@portone/browser-sdk/v2")).default;
  const baseArgs: Record<string, unknown> = {
    storeId: STORE_ID, channelKey,
    paymentId: prepared.paymentId,
    orderName: prepared.productName,
    totalAmount: prepared.amount,
    currency: "CURRENCY_KRW",
    redirectUrl,
    customer: {
      fullName: buyer.name,
      email: buyer.email,
      phoneNumber: buyer.phoneNumber.replace(/[-\s]/g, ""),
    },
  };
  // ... requestArg 분기 (KAKAOPAY/CARD) 동일
}
```

`PrepareResponse` 에서 `customerEmail` 필드 제거.

## CheckoutClient 변경

```tsx
import BuyerInfoModal from "@/components/billing/BuyerInfoModal";
import type { BuyerInfo } from "@/lib/payment";

const [pendingPlan, setPendingPlan] = useState<SubscriptionPlan | null>(null);

function onPay(plan: SubscriptionPlan) {
  if (payingPlan) return;
  setPendingPlan(plan);  // 모달 열림
}

async function onBuyerSubmit(buyer: BuyerInfo) {
  if (!pendingPlan) return;
  const plan = pendingPlan;
  setPendingPlan(null);
  setPayingPlan(plan);
  try {
    const result = await startPayment({ plan, method, buyer });
    toast.show(`${planLabel(result.plan)} 결제 완료`, "success");
    setTimeout(() => router.push("/mock-exams"), 800);
  } catch (e) {
    const message = e instanceof Error ? e.message : "";
    const cancelled = /취소|cancel/i.test(message);
    toast.show(
      cancelled ? "결제를 취소하셨습니다." : message || "결제 처리 중 오류가 발생했습니다.",
      cancelled ? "info" : "error",
    );
    if (e instanceof Error) console.error("[checkout]", e);
  } finally {
    setPayingPlan(null);
  }
}

// JSX 마지막에
<BuyerInfoModal
  open={pendingPlan !== null}
  plan={pendingPlan}
  onClose={() => setPendingPlan(null)}
  onSubmit={onBuyerSubmit}
/>
```

(`clearAuth` import 제거, REAUTH 분기 제거.)

복귀 verify 흐름(redirectUrl) 에서는 buyer 가 필요 없으니 기존 그대로 유지 — `verifyPaymentById(paymentId)` 만 호출.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

## Acceptance Criteria

1. `oauth.ts` scope = `"openid profile"`.
2. `buyerStorage.ts` 가 신규 생성되고 email/phone 만 저장.
3. `BuyerInfoModal` 컴포넌트가 신규 생성 — 안내 박스(3단계 카피) + 3 필드 + 검증 + 자동 채움 + ESC/overlay/X close + scroll lock.
4. `CheckoutClient.onPay` 가 모달을 열고, `onBuyerSubmit` 가 실제 startPayment 호출.
5. `startPayment` 시그니처에 `buyer: BuyerInfo` 가 필수로 추가됨. prepare body 에 buyerName/Email/PhoneNumber 전달.
6. PortOne SDK `customer.fullName/email/phoneNumber` 가 KAKAOPAY/CARD 양쪽에서 채워짐.
7. REAUTH_REQUIRED / customerEmail / clearAuth import 모두 제거.
8. `npm run lint` 0 errors, `npm run build` 성공.

## 금지 사항

- BuyerInfoModal 안에서 직접 `/api/payment/prepare` 를 호출하지 마라. **이유**: 모달은 입력 수집만, 결제 흐름은 payment.ts 단일 진입점.
- name 을 localStorage 에 저장하지 마라. **이유**: 사용자 결정. 실명 영수증용이라 매번 명시 입력.
- Capacitor 앱 흐름에 buyer 모달을 띄우지 마라. **이유**: Play Billing 자체 수집, 외부 PG 노출은 정책 위반. `isCapacitorApp()` 분기에서 모달 스킵.
- PortOne customer 객체에 빈 문자열을 전달하지 마라. **이유**: KG이니시스 거절 가능. 검증 통과 후만 SDK 호출.
- 안내 박스의 "회원정보 미저장" 카피를 단순 문구로 줄이지 마라. **이유**: 사용자 명시 요청 — 3단계 카피(평소 안 받음 → 왜 결제때 → 어디 쓰는지) 유지.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "oauth 원복, payment.ts buyer 전달, BuyerInfoModal 신규(안내 박스 + 검증 + 자동 채움), CheckoutClient 모달 통합, lint/build OK".
- 실패: 3회 재시도 후 `error`.
