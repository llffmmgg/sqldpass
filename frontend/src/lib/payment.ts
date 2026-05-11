/**
 * PortOne V2 구독 결제 흐름.
 *
 * 1. POST /api/payment/prepare {plan} → paymentId·amount·productName 사전 등록
 * 2. PortOne 브라우저 SDK 의 requestPayment 로 결제창 호출
 *    - method=KAKAOPAY → 카카오페이 채널 + payMethod=EASY_PAY
 *    - method=CARD     → KG이니시스(INIpay) 채널 + payMethod=CARD
 * 3. POST /api/payment/verify {paymentId} → backend 가 PortOne REST 재검증 + Subscription 발급
 * 4. GET /api/payment/subscription → 활성 구독 정보
 *
 * 환경변수:
 *   NEXT_PUBLIC_PORTONE_STORE_ID                 — store-{uuid}
 *   NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAOPAY     — 카카오페이 채널 키
 *   NEXT_PUBLIC_PORTONE_CHANNEL_KEY_INICIS       — KG이니시스(INIpay) 채널 키 (테스트: INIpayTest)
 *   NEXT_PUBLIC_PORTONE_CHANNEL_KEY              — (deprecated) 단일 키 — KAKAOPAY 누락 시 fallback
 */

import { getToken } from "@/lib/auth";
import { isCapacitorApp } from "@/lib/platform";

const STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID ?? "";
const CHANNEL_KEY_KAKAOPAY =
  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAOPAY ??
  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY ??
  "";
const CHANNEL_KEY_INICIS = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_INICIS ?? "";

export type SubscriptionPlan = "THREE_DAY" | "ONE_MONTH" | "UNLIMITED";

export type PaymentMethod = "KAKAOPAY" | "CARD";

/**
 * SubscriptionPlan ↔ Play Console 일회성 상품 SKU 매핑.
 * 백엔드 application.yaml 의 sqldpass.play-billing.product-id-mapping 과 일치해야 한다.
 * 환경변수 NEXT_PUBLIC_PLAY_BILLING_SKU_* 로 빌드 시 오버라이드 가능.
 */
const PLAY_BILLING_SKU: Record<SubscriptionPlan, string> = {
  THREE_DAY: process.env.NEXT_PUBLIC_PLAY_BILLING_SKU_THREE_DAY ?? "iap_three_day",
  ONE_MONTH: process.env.NEXT_PUBLIC_PLAY_BILLING_SKU_ONE_MONTH ?? "iap_one_month",
  UNLIMITED: process.env.NEXT_PUBLIC_PLAY_BILLING_SKU_UNLIMITED ?? "iap_unlimited",
};

export type CheckoutEligibility = { eligible: boolean };

export type PrepareResponse = {
  paymentId: string;
  amount: number;
  productName: string;
  plan: SubscriptionPlan;
  storeId: string;
};

export type VerifyResponse = {
  paymentId: string;
  amount: number;
  productName: string;
  plan: SubscriptionPlan;
  expiresAt: string | null;
};

export type ActiveSubscription = {
  active: boolean;
  plan: SubscriptionPlan | null;
  expiresAt: string | null;
  removesAds: boolean;
  allowsPdf: boolean;
};

export type PreviewResponse = {
  plan: SubscriptionPlan;
  baseAmount: number;
  prorateDiscount: number;
  finalAmount: number;
  allowed: boolean;
  reason: string | null;
};

async function authFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    let message = `요청 실패 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      // ignore
    }
    throw new Error(message);
  }
  return (await res.json()) as T;
}

export async function getCheckoutEligibility(): Promise<CheckoutEligibility> {
  return authFetch<CheckoutEligibility>("/api/payment/eligibility");
}

/** 활성 구독 정보 조회. 없으면 active=false 반환. */
export async function getActiveSubscription(): Promise<ActiveSubscription> {
  return authFetch<ActiveSubscription>("/api/payment/subscription");
}

/**
 * 모바일 redirectUrl 복귀 흐름에서 호출 — paymentId 만으로 서버 verify.
 * verify 는 PaymentService 에서 멱등(PAID 캐시 시 PortOne 호출 0회) 보증되므로
 * SDK Promise resolve 경로와 동시에 들어와도 안전.
 */
export async function verifyPaymentById(paymentId: string): Promise<VerifyResponse> {
  return authFetch<VerifyResponse>("/api/payment/verify", {
    method: "POST",
    body: JSON.stringify({ paymentId }),
  });
}

/** plan 별 결제 미리보기 — 활성 구독의 prorate 차감 적용된 실 결제 금액 반환. */
export async function previewPayment(plan: SubscriptionPlan): Promise<PreviewResponse> {
  return authFetch<PreviewResponse>(`/api/payment/preview?plan=${encodeURIComponent(plan)}`);
}

/**
 * PDF 다운로드 버튼 노출 여부 (가시성 전용).
 * 베타 기간엔 화이트리스트 닉네임 회원에게 true (미결제여도 노출 → 클릭 시 결제 유도).
 * 정식 오픈 시 UNLIMITED 결제 회원에게만 true.
 * 실 다운로드 권한은 별도 — downloadMockExamPdfAsUser 가 PDF_REQUIRES_SUBSCRIPTION 으로 거절.
 */
export async function getPdfEligibility(): Promise<{ eligible: boolean }> {
  return authFetch<{ eligible: boolean }>("/api/mock-exams/pdf/eligibility");
}

/** PDF 다운로드 거절 시 throw 하는 에러 — code 로 분기 가능. */
export class PdfDownloadError extends Error {
  readonly code: string;
  constructor(code: string, message: string) {
    super(message);
    this.code = code;
    this.name = "PdfDownloadError";
  }
}

/**
 * 사용자용 PDF 다운로드 — 백엔드 프록시로 받아 즉시 다운로드 트리거.
 * R2 public URL 노출 X. 한글 파일명(예: SQLD_모의고사_18회.pdf) 자동 적용.
 */
export async function downloadMockExamPdfAsUser(id: number): Promise<void> {
  const token = getToken();
  const res = await fetch(`/api/mock-exams/${id}/pdf/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    let code = `HTTP_${res.status}`;
    let message = `PDF 다운로드 실패 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.code) code = body.code;
      if (body?.message) message = body.message;
    } catch {
      // body 파싱 실패 시 기본 메시지 사용
    }
    throw new PdfDownloadError(code, message);
  }
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  const asciiMatch = /filename="?([^";]+)"?/i.exec(disposition);
  const filename = utf8Match
    ? decodeURIComponent(utf8Match[1])
    : asciiMatch?.[1] ?? `mock-exam-${id}.pdf`;

  const blob = await res.blob();
  const objectUrl = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = objectUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(objectUrl), 100);
}

/**
 * 결제 시작 — 실행 환경에 따라 PortOne(웹) 또는 Google Play Billing(안드로이드 앱) 으로 분기.
 *
 * 안드로이드 앱에서 PortOne 결제창을 띄우면 Play 정책 위반이라 즉시 등록 거절. 반대로 웹에서
 * Play Billing 호출은 의미 없음. 그래서 isCapacitorApp() 으로 명확히 갈라준다.
 *
 * 성공 시 양쪽 모두 VerifyResponse 형태로 동일 — 결제 채널 이후 흐름은 통일.
 */
export async function startPayment(opts: {
  plan: SubscriptionPlan;
  method?: PaymentMethod;
}): Promise<VerifyResponse> {
  if (isCapacitorApp()) {
    return startPaymentPlayBilling(opts.plan);
  }
  return startPaymentPortOne(opts.plan, opts.method ?? "KAKAOPAY");
}

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
  // 모바일 브라우저는 카카오페이/카드 ISP 가 외부 앱으로 전환된 뒤 복귀해야 한다.
  // redirectUrl 미설정 시 PortOne 기본 결과 페이지에 멈출 수 있다 — 우리 /checkout 으로
  // 돌려 받고, paymentId 쿼리로 복귀 verify 를 호출한다 (verify 는 멱등).
  const redirectUrl =
    typeof window !== "undefined"
      ? `${window.location.origin}/checkout?paymentId=${encodeURIComponent(prepared.paymentId)}`
      : undefined;
  const baseArgs = {
    storeId: STORE_ID,
    channelKey,
    paymentId: prepared.paymentId,
    orderName: prepared.productName,
    totalAmount: prepared.amount,
    currency: "CURRENCY_KRW" as const,
    redirectUrl,
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

async function startPaymentPlayBilling(plan: SubscriptionPlan): Promise<VerifyResponse> {
  const billing = (typeof window !== "undefined" ? window.Capacitor?.Plugins?.Billing : undefined);
  if (!billing) {
    throw new Error(
      "앱 결제 플러그인이 설정되지 않았어요. 잠시 후 다시 시도해주세요.\n" +
      "(개발: mobile/ 워크스페이스에 Play Billing Capacitor 플러그인 설치/등록 필요)",
    );
  }
  const productId = PLAY_BILLING_SKU[plan];
  if (!productId) {
    throw new Error(`알 수 없는 상품 plan: ${plan}`);
  }
  const result = await billing.purchase({ productId });
  if (!result.success || !result.purchaseToken) {
    throw new Error(result.errorMessage ?? "결제가 취소되었거나 실패했습니다.");
  }
  return authFetch<VerifyResponse>("/api/payment/play-billing/verify", {
    method: "POST",
    body: JSON.stringify({ productId, purchaseToken: result.purchaseToken }),
  });
}
