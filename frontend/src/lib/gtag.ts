/**
 * GA4 이벤트 전송 헬퍼.
 * window.gtag 이 로드되기 전(SSR/초기 페인트) 호출에 안전.
 *
 * 주의 — 개인정보:
 * - 닉네임/이메일/원본 memberId 등 PII 는 절대 보내지 않는다 (GA 약관 위반).
 * - user_id (GA 표준 필드) 는 setUserId 로 명시 호출 시에만. 기본은 미설정.
 */

declare global {
  interface Window {
    gtag?: (...args: unknown[]) => void;
    dataLayer?: unknown[];
  }
}

type EventParams = Record<string, string | number | boolean | undefined | null>;

function safeGtag(...args: unknown[]) {
  if (typeof window === "undefined") return;
  if (typeof window.gtag !== "function") return;
  try {
    window.gtag(...args);
  } catch {
    /* 무시 — 분석 코드 실패가 사용자 흐름을 막아선 안 됨 */
  }
}

/** 커스텀 이벤트 전송 */
export function trackEvent(name: string, params: EventParams = {}): void {
  // null/undefined 값은 빼서 깔끔하게
  const cleaned: EventParams = {};
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null) cleaned[k] = v;
  }
  safeGtag("event", name, cleaned);
}

/** 페이지뷰 수동 전송 (App Router SPA 라우트 변경 추적용) */
export function trackPageview(path: string): void {
  safeGtag("event", "page_view", {
    page_path: path,
    page_location: typeof window !== "undefined" ? window.location.href : path,
  });
}

/**
 * GA user_id 설정 — 로그인-비로그인 행동을 묶어 분석할 때만 호출.
 * 현재 사용처 없음 (안전 기본). 필요하면 OAuth 콜백 등에서 호출.
 * 절대 평문 PII (이메일/닉네임/원본 memberId) 가 아닌 hash 값을 권장.
 */
export function setUserId(userId: string): void {
  safeGtag("config", "G-MPQ2F9201M", { user_id: userId });
}

/** 사용자 속성 (세그먼트 분석용 — 예: plan_type=free) */
export function setUserProperties(props: EventParams): void {
  safeGtag("set", "user_properties", props);
}
