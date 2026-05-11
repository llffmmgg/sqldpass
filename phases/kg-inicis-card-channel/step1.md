# Step 1 — PortOne 채널 분리 + 결제수단 분기

## 배경

현재 `frontend/src/lib/payment.ts` 는 PortOne V2 단일 채널(카카오페이)만 호출한다. KG이니시스 일반결제(신용카드, INIpayTest MID) 채널을 추가해 카드결제를 받을 수 있게 한다.

PortOne 콘솔에서 KG이니시스 채널을 추가하면 `channelKey` 가 별도 발급된다. 같은 store 안에서 두 채널을 병행 운영 — `channelKey` + `payMethod` 만 호출 시 분기하면 된다. 백엔드 검증(`/api/payment/verify`)은 PortOne REST 응답 구조가 채널에 무관하게 동일해서 변경 없음.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `frontend/.env.example` | PortOne 채널 키를 KAKAOPAY/INICIS 두 변수로 분리 |
| `frontend/src/lib/payment.ts` (line 17-18, 165-210) | `CHANNEL_KEY_KAKAOPAY`, `CHANNEL_KEY_INICIS` 상수 + `PaymentMethod` 타입 + `startPayment({plan, method})` 분기 + `startPaymentPortOne` 내부 분기 |

## 환경변수 변경

```diff
- NEXT_PUBLIC_PORTONE_CHANNEL_KEY=
+ NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAOPAY=     # 기존 카카오페이 채널 키
+ NEXT_PUBLIC_PORTONE_CHANNEL_KEY_INICIS=       # KG이니시스(INIpayTest) 채널 키
```

`payment.ts` 상단:

```ts
const STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID ?? "";
const CHANNEL_KEY_KAKAOPAY =
  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAOPAY ??
  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY ?? // 한 PR 한정 fallback
  "";
const CHANNEL_KEY_INICIS = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_INICIS ?? "";
```

## API 변경

```ts
// 신규 export
export type PaymentMethod = "KAKAOPAY" | "CARD";

// 기존 startPayment 시그니처 확장 (method 옵션, 기본 KAKAOPAY)
export async function startPayment(opts: {
  plan: SubscriptionPlan;
  method?: PaymentMethod;
}): Promise<VerifyResponse>;
```

기존 호출부 `startPayment({ plan })` 는 method 미지정 → KAKAOPAY 로 동작. **하위 호환 유지.**

## startPaymentPortOne 분기

```ts
async function startPaymentPortOne(
  plan: SubscriptionPlan,
  method: PaymentMethod,
): Promise<VerifyResponse> {
  if (!STORE_ID) {
    throw new Error("결제 설정이 비어있습니다 (NEXT_PUBLIC_PORTONE_STORE_ID).");
  }
  const channelKey = method === "CARD" ? CHANNEL_KEY_INICIS : CHANNEL_KEY_KAKAOPAY;
  if (!channelKey) {
    throw new Error(
      method === "CARD"
        ? "신용카드 결제 설정이 비어있습니다 (NEXT_PUBLIC_PORTONE_CHANNEL_KEY_INICIS)."
        : "카카오페이 결제 설정이 비어있습니다 (NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAOPAY).",
    );
  }
  const prepared = await authFetch<PrepareResponse>("/api/payment/prepare", {
    method: "POST",
    body: JSON.stringify({ plan }),
  });

  const PortOne = (await import("@portone/browser-sdk/v2")).default;
  const baseArgs = {
    storeId: STORE_ID,
    channelKey,
    paymentId: prepared.paymentId,
    orderName: prepared.productName,
    totalAmount: prepared.amount,
    currency: "CURRENCY_KRW" as const,
  };
  const requestArg =
    method === "CARD"
      ? { ...baseArgs, payMethod: "CARD" as const }
      : {
          ...baseArgs,
          payMethod: "EASY_PAY" as const,
          easyPay: { easyPayProvider: "KAKAOPAY" as const },
        };

  const response = await PortOne.requestPayment(
    requestArg as unknown as Parameters<typeof PortOne.requestPayment>[0],
  );
  if (response && "code" in response && response.code !== undefined) {
    throw new Error(response.message ?? "결제가 취소되었거나 실패했습니다.");
  }
  return authFetch<VerifyResponse>("/api/payment/verify", {
    method: "POST",
    body: JSON.stringify({ paymentId: prepared.paymentId }),
  });
}
```

`startPayment` 본문:

```ts
export async function startPayment(opts: {
  plan: SubscriptionPlan;
  method?: PaymentMethod;
}): Promise<VerifyResponse> {
  if (isCapacitorApp()) {
    return startPaymentPlayBilling(opts.plan);
  }
  return startPaymentPortOne(opts.plan, opts.method ?? "KAKAOPAY");
}
```

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

## Acceptance Criteria

1. `.env.example` 에 `NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAOPAY`, `NEXT_PUBLIC_PORTONE_CHANNEL_KEY_INICIS` 두 변수가 존재한다.
2. `payment.ts` 에 `PaymentMethod` 타입이 export 되어 있다.
3. `startPayment` 시그니처는 `{plan, method?}` 이며 method 기본값은 KAKAOPAY.
4. `startPaymentPortOne` 내부에서 method 에 따라 channelKey 와 payMethod 가 분기된다.
5. Capacitor 앱은 그대로 `startPaymentPlayBilling` 로 진입한다 (변경 없음).
6. `npm run lint` 오류 0, `npm run build` 성공.

## 금지 사항

- 백엔드(`backend/`)를 수정하지 마라. 이유: PortOne 검증 API 는 채널에 무관하게 동일 응답이라 백엔드 변경 불필요. PaymentProvider enum 도 PORTONE 그대로.
- `PaymentEntity.payMethod` 같은 컬럼/마이그레이션을 추가하지 마라. 이유: 결제수단별 통계는 본 phase 범위 밖. 필요하면 별도 phase.
- 카카오페이 채널의 `easyPay.easyPayProvider` 값(KAKAOPAY)을 바꾸지 마라. 이유: 기존 결제 회귀 방지.
- 환경변수 fallback 제거하지 마라. 이유: 배포 환경의 `NEXT_PUBLIC_PORTONE_CHANNEL_KEY` 변수가 아직 살아있을 수 있어 즉시 끊으면 카카오페이 결제가 깨진다. 한 PR 한정으로 두고 후속 PR에서 정리.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "payment.ts 채널/결제수단 분기 추가, lint/build OK".
- 실패: 3회 재시도 후 실패면 `error`.
