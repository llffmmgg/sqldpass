/**
 * AdSidebar(명시적 사이드 광고) 와 AdSenseLoader(adsbygoogle.js 자체) 모두
 * 같은 차단 경로 정책을 공유한다. AdSenseLoader 까지 막아야 AdSense Auto Ads
 * 가 끼어들지 않는다 — AdSidebar 만 막아도 Google 이 임의 위치에 자동 광고를
 * 삽입하기 때문.
 */
export const EXCLUDED_PREFIXES = [
  "/admin",
  "/profile",
  "/mypage/feedback",
  "/auth/callback",
  // 결제 결정 흐름 — 광고로 시선 분산 방지
  "/checkout",
];

export function isAdExcludedPath(pathname: string | null | undefined): boolean {
  if (!pathname) return false;
  return EXCLUDED_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}
