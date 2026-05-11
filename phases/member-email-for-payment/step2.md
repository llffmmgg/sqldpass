# Step 2 — 프론트엔드: OAuth scope + customer + 재로그인 유도

## 배경

Step 1 에서 백엔드가 `PreparePaymentResult.customerEmail` 을 채워준다. 프론트엔드는:
1. OAuth scope 에 `email` 추가해서 다음 로그인부터 백엔드가 verified email 을 받음
2. PortOne SDK 호출 시 `customer.email` 을 전달 (있을 때만)
3. CARD 결제인데 customerEmail 이 null 이면 사용자에게 "재로그인 필요" 안내 + 자동 로그아웃

KAKAOPAY 는 차단하지 않음 — 기존 사용자 경험 보호.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/src/lib/oauth.ts` (L9) | scope `"openid profile"` → `"openid profile email"` |
| `frontend/src/lib/payment.ts` | `PrepareResponse` 타입에 `customerEmail` 추가, `startPaymentPortOne` 가 CARD + customerEmail null 시 REAUTH_REQUIRED throw, baseArgs.customer 전달 |
| `frontend/src/app/checkout/CheckoutClient.tsx` | onPay catch 에서 REAUTH_REQUIRED 분기 — 토스트 + clearAuth + 로그인 페이지 라우팅 |

## payment.ts 변경

`PrepareResponse` 타입:
```ts
export type PrepareResponse = {
  paymentId: string;
  amount: number;
  productName: string;
  plan: SubscriptionPlan;
  storeId: string;
  customerEmail: string | null;
};
```

`startPaymentPortOne` 안에서 prepare 응답 받은 직후:
```ts
const prepared = await authFetch<PrepareResponse>("/api/payment/prepare", { ... });

// CARD 는 KG이니시스 customer.email 필수 — null 이면 재로그인 유도
if (method === "CARD" && !prepared.customerEmail) {
  throw new Error("REAUTH_REQUIRED:신용카드 결제를 위해 이메일 정보가 필요합니다. Google 동의 화면에서 이메일 주소 권한을 허용하고 다시 로그인해주세요.");
}
```

baseArgs 에 customer 추가 (있을 때만):
```ts
const baseArgs: Record<string, unknown> = {
  storeId: STORE_ID,
  channelKey,
  paymentId: prepared.paymentId,
  orderName: prepared.productName,
  totalAmount: prepared.amount,
  currency: "CURRENCY_KRW",
  redirectUrl,
};
if (prepared.customerEmail) {
  baseArgs.customer = { email: prepared.customerEmail };
}
```

기존 `baseArgs` 의 `const` 선언 + 분기는 그대로 두되, customer 만 조건부 mutation 으로 추가. requestArg 의 spread 도 그대로 작동.

## CheckoutClient.tsx 변경

`onPay` catch 분기에 추가:
```ts
} catch (e) {
  const message = e instanceof Error ? e.message : "";
  
  // 재로그인 유도 — KG이니시스 결제용 email 누락
  if (message.startsWith("REAUTH_REQUIRED:")) {
    const userMsg = message.substring("REAUTH_REQUIRED:".length);
    toast.show(userMsg, "info");
    setTimeout(() => {
      clearAuth();
      router.push("/");  // 홈 또는 로그인 진입점
    }, 2000);
    return;
  }
  
  const cancelled = /취소|cancel/i.test(message);
  // ... 기존 분기
}
```

`clearAuth` import 추가 — `@/lib/auth` 에 이미 존재 여부 grep 으로 확인 후 사용.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

### (옵션) 수동 검증
1. 로그아웃 후 Google 로그인 → 동의 화면에 "이메일 주소" 권한 표시 확인.
2. DB `SELECT email FROM member WHERE id=?` 로 채워졌는지 확인.
3. `/checkout` → 신용카드 → PortOne 결제창 customer email 자동 입력 확인.
4. 직접 `UPDATE member SET email=NULL WHERE id=?` → 신용카드 결제 시도 → 토스트 + 2초 후 홈 이동 + 로그아웃 확인.
5. 카카오페이는 email NULL 이어도 정상 결제 (회귀 없음).

## Acceptance Criteria

1. `oauth.ts:9` scope 가 `"openid profile email"` 이다.
2. `PrepareResponse` 타입에 `customerEmail: string | null` 이 추가됨.
3. `startPaymentPortOne` 가 CARD + customerEmail null 시 `REAUTH_REQUIRED:...` 메시지로 throw.
4. customerEmail 있으면 `baseArgs.customer = { email }` 으로 SDK 에 전달.
5. CheckoutClient onPay catch 에서 REAUTH_REQUIRED 분기 동작 — 토스트 + clearAuth + 홈 라우팅.
6. KAKAOPAY 결제는 customerEmail null 이어도 차단되지 않는다.
7. `npm run lint` 0 errors, `npm run build` 성공.

## 금지 사항

- KAKAOPAY 도 customerEmail null 시 차단하지 마라. 이유: 사용자 결정. 기존 사용자 경험 보호, KG이니시스가 필요로 하는 유일 경로.
- redirectUrl 같은 다른 결제 인자를 함께 수정하지 마라. 이유: 본 step 은 customer/scope/재로그인만. 변경 범위 최소화.
- REAUTH_REQUIRED 에러를 백엔드 에러 코드로 던지지 마라. 이유: 프론트 결제수단 분기는 클라이언트가 가장 잘 안다. 백엔드는 customerEmail null 반환만, 차단 정책은 프론트 책임.
- `clearAuth` 후 즉시 `router.push` 하지 마라. setTimeout 2초 유지. 이유: 토스트가 사용자에게 보이는 시간 확보.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "oauth scope email 추가, payment.ts customer + REAUTH_REQUIRED, CheckoutClient 분기, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
