# Step 2 — 결제수단 선택 UI

## 배경

Step 1 에서 `startPayment({plan, method})` 시그니처가 method 옵션을 받게 됐다. 사용자가 카카오페이/신용카드를 고를 수 있는 토글 UI를 결제 페이지에 추가한다. 안드로이드 앱(Capacitor) 환경에서는 Play Billing 만 사용해야 하므로 토글을 숨긴다 — Google Play 정책상 외부 PG 노출 금지.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/components/billing/CheckoutLanding.tsx` | Props 에 `method`, `onChangeMethod`, `showMethodToggle` 추가. 4개 카드 위(헤더 아래)에 토글 행 추가. `onPay` 시그니처는 그대로(method 는 CheckoutClient 가 startPayment 호출 시 같이 전달). |
| `frontend/src/app/checkout/CheckoutClient.tsx` | `method` state (`useState<PaymentMethod>("KAKAOPAY")`) 추가. Capacitor 앱이면 토글 숨김. `onPay` 에서 `startPayment({plan, method})` 호출. |

## CheckoutClient 변경

```tsx
import { isCapacitorApp } from "@/lib/platform";
import {
  // ...
  type PaymentMethod,
} from "@/lib/payment";

const [method, setMethod] = useState<PaymentMethod>("KAKAOPAY");
const showMethodToggle = !isCapacitorApp();

async function onPay(plan: SubscriptionPlan) {
  if (payingPlan) return;
  setPayingPlan(plan);
  try {
    const result = await startPayment({ plan, method });
    // ... 기존과 동일
  }
  // ...
}

return (
  <CheckoutLanding
    currentPlan={subscription?.active ? subscription.plan : null}
    previews={previews}
    payingPlan={payingPlan}
    subscription={subscription}
    method={method}
    onChangeMethod={setMethod}
    showMethodToggle={showMethodToggle}
    onPay={onPay}
  />
);
```

## CheckoutLanding 변경

Props 추가:

```tsx
type Props = {
  currentPlan: SubscriptionPlan | null;
  previews: Record<SubscriptionPlan, PreviewResponse | null>;
  payingPlan: SubscriptionPlan | null;
  subscription: ActiveSubscription | null;
  method: PaymentMethod;
  onChangeMethod: (m: PaymentMethod) => void;
  showMethodToggle: boolean;
  onPay: (plan: SubscriptionPlan) => void;
};
```

`subscription.active` 알림 블록과 4-카드 grid(`mt-14 grid ...`) 사이에 토글 행 추가:

```tsx
{showMethodToggle && (
  <div className="mt-10 flex justify-center">
    <div
      role="radiogroup"
      aria-label="결제 수단"
      className="inline-flex items-center gap-1 rounded-full border border-border bg-surface/40 p-1"
    >
      <MethodButton
        active={method === "KAKAOPAY"}
        onClick={() => onChangeMethod("KAKAOPAY")}
        label="카카오페이"
      />
      <MethodButton
        active={method === "CARD"}
        onClick={() => onChangeMethod("CARD")}
        label="신용카드"
      />
    </div>
  </div>
)}
```

`MethodButton` 내부 보조 컴포넌트는 같은 파일에 추가:

```tsx
function MethodButton({
  active,
  onClick,
  label,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={active}
      onClick={onClick}
      className={`rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
        active
          ? "bg-amber-500 text-amber-950 shadow-[0_0_18px_-4px_rgba(245,181,68,0.6)]"
          : "text-text-muted hover:text-text"
      }`}
    >
      {label}
    </button>
  );
}
```

기존 PlanCard / FAQ / 약관 안내는 그대로 둔다 — UI 추가만, 제거 없음.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

## (옵션) 수동 검증

`npm run dev` 후 `/checkout` 진입:

- [ ] 데스크탑 web: 헤더 아래 토글이 보이고 카카오페이/신용카드 클릭 시 활성 상태가 바뀐다.
- [ ] 신용카드 선택 → 결제 버튼 → KG이니시스 결제창이 뜬다 (테스트 카드 4111…).
- [ ] 카카오페이 선택 → 결제 버튼 → 카카오페이 결제창이 뜬다 (회귀 확인).
- [ ] Android 앱(Capacitor) 또는 `isCapacitorApp()` mock — 토글이 보이지 않고 Play Billing 으로 빠진다.

## Acceptance Criteria

1. `CheckoutClient` 가 `useState<PaymentMethod>("KAKAOPAY")` 로 method 를 관리한다.
2. `isCapacitorApp()` true 면 토글이 렌더되지 않는다.
3. `onPay` 가 `startPayment({plan, method})` 를 호출한다.
4. `CheckoutLanding` 토글이 `role="radiogroup"` + `aria-checked` 로 접근성 표기된다.
5. 기존 4-카드 UI / FAQ / 약관 안내는 시각적으로 변하지 않는다.
6. `npm run lint` 오류 0, `npm run build` 성공.

## 금지 사항

- PlanCard 안에 토글을 넣지 마라. 이유: 카드 4개 × 토글 2개 = 시각 노이즈. 페이지 상단 단일 토글이 단순.
- 토글을 `<select>` 로 만들지 마라. 이유: 라디오 토글이 시안성·터치 모두 우수, 옵션 2개라 select 의 펼침 동작은 불필요.
- 약관 안내 / FAQ 본문 텍스트를 바꾸지 마라. 이유: 본 step 범위 밖. 필요 시 별도 phase.
- Capacitor 분기 없이 토글을 항상 노출하지 마라. 이유: Google Play 정책 위반(외부 PG 노출) → 심사 거절 위험.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "결제수단 토글 UI 추가, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
