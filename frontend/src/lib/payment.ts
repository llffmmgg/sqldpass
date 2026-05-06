/**
 * PortOne V2 4티어 구독 결제 흐름.
 *
 * 1. POST /api/payment/prepare {plan} → paymentId·amount·productName 사전 등록
 * 2. PortOne 브라우저 SDK 의 requestPayment 로 결제창 호출
 * 3. POST /api/payment/verify {paymentId} → backend 가 PortOne REST 재검증 + Subscription 발급
 * 4. GET /api/payment/subscription → 활성 구독 정보
 *
 * 환경변수:
 *   NEXT_PUBLIC_PORTONE_STORE_ID    — store-{uuid}
 *   NEXT_PUBLIC_PORTONE_CHANNEL_KEY — 채널 키 (PG 사 한 곳)
 */

import { getToken } from "@/lib/auth";

const STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID ?? "";
const CHANNEL_KEY = process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY ?? "";

export type SubscriptionPlan = "THREE_DAY" | "ONE_MONTH" | "UNLIMITED";

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
 * PDF 다운로드 가능 여부 — Phase 4 에서 subscription.allowsPdf 로 교체될 예정.
 * 현재는 백엔드의 옛 화이트리스트 가드를 그대로 호출.
 */
export async function getPdfEligibility(): Promise<{ eligible: boolean }> {
  return authFetch<{ eligible: boolean }>("/api/mock-exams/pdf/eligibility");
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
    const text = await res.text().catch(() => "");
    throw new Error(text || `PDF 다운로드 실패 (${res.status})`);
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
 * 결제 한 번 — backend 사전 등록 → PortOne SDK 결제창 → backend 검증.
 * 성공 시 verify 응답을 반환. 사용자 취소·실패 시 throw.
 */
export async function startPayment(opts: { plan: SubscriptionPlan }): Promise<VerifyResponse> {
  if (!STORE_ID || !CHANNEL_KEY) {
    throw new Error("결제 설정이 비어있습니다 (NEXT_PUBLIC_PORTONE_STORE_ID / NEXT_PUBLIC_PORTONE_CHANNEL_KEY).");
  }
  const prepared = await authFetch<PrepareResponse>("/api/payment/prepare", {
    method: "POST",
    body: JSON.stringify({ plan: opts.plan }),
  });

  const PortOne = (await import("@portone/browser-sdk/v2")).default;
  const response = await PortOne.requestPayment({
    storeId: STORE_ID,
    channelKey: CHANNEL_KEY,
    paymentId: prepared.paymentId,
    orderName: prepared.productName,
    totalAmount: prepared.amount,
    currency: "CURRENCY_KRW",
    payMethod: "CARD",
  } as unknown as Parameters<typeof PortOne.requestPayment>[0]);

  if (response && "code" in response && response.code !== undefined) {
    throw new Error(response.message ?? "결제가 취소되었거나 실패했습니다.");
  }

  return authFetch<VerifyResponse>("/api/payment/verify", {
    method: "POST",
    body: JSON.stringify({ paymentId: prepared.paymentId }),
  });
}
