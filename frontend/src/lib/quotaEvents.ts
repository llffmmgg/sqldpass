// 무료 일일 한도 초과(HTTP 402) 이벤트 — 백엔드 단일 가드 응답을 전역 모달로 라우팅.
//
// 흐름:
//   1. fetchApi 가 402 를 받으면 body 를 파싱해 dispatchQuotaExceeded() 호출
//   2. QuotaPaywallModal 이 window 의 "quota-exceeded" 커스텀 이벤트를 구독
//   3. 호출자는 QuotaExceededError 를 catch 해서 후속 UI 분기(스피너 닫기 등)에 활용
//
// 카운팅·잔량 추정은 일절 클라가 만지지 않는다. 화면에 띄우는 used/limit/resetAt 은 서버 응답을 그대로 사용.

export type QuotaErrorCode = "DAILY_QUESTION_LIMIT" | "DAILY_MOCK_LIMIT";

export interface QuotaExceededPayload {
  error: QuotaErrorCode;
  used: number;
  limit: number;
  /** KST naive ISO (예: "2026-05-22T00:00:00") — 표시 시 +09:00 가정. */
  resetAt: string;
}

export const QUOTA_EXCEEDED_EVENT = "quota-exceeded";

export class QuotaExceededError extends Error {
  readonly code: QuotaErrorCode;
  readonly payload: QuotaExceededPayload;

  constructor(payload: QuotaExceededPayload) {
    super(payload.error === "DAILY_MOCK_LIMIT" ? "오늘의 모의고사 한도에 도달했어요." : "오늘의 무료 풀이 한도에 도달했어요.");
    this.name = "QuotaExceededError";
    this.code = payload.error;
    this.payload = payload;
  }
}

export function isQuotaErrorCode(value: unknown): value is QuotaErrorCode {
  return value === "DAILY_QUESTION_LIMIT" || value === "DAILY_MOCK_LIMIT";
}

export function dispatchQuotaExceeded(payload: QuotaExceededPayload) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(
    new CustomEvent<QuotaExceededPayload>(QUOTA_EXCEEDED_EVENT, { detail: payload }),
  );
}
