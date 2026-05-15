"use client";

import Script from "next/script";
import { usePathname } from "next/navigation";

import { useSubscription } from "@/hooks/useSubscription";
import { isAdExcludedPath } from "@/lib/adsExcludedPrefixes";

const ADSENSE_CLIENT = "ca-pub-6512792395955186";

/**
 * AdSense adsbygoogle.js 스크립트 로더.
 *
 * 자동 광고(Auto Ads) 까지 포함해 모든 AdSense 광고를 막으려면 스크립트 자체를
 * 로드하지 말아야 한다. 명시적 광고 컴포넌트(AdSidebar 등) 만 컨트롤해도
 * AdSense 콘솔에서 자동 광고가 켜져 있으면 Google 이 페이지에 임의로 광고를 삽입.
 *
 * 동작:
 *  - useSubscription 응답 대기 중 → 로드 보류
 *  - removesAds=true (한달권/무제한 회원) → 스크립트 안 로드 = 자동 광고도 안 뜸
 *  - EXCLUDED_PREFIXES(/checkout 등) 경로 → 스크립트 안 로드 = Auto Ads 도 안 뜸
 *  - 그 외 → 평소대로 lazyOnload 스크립트 로드
 *
 * 비로그인은 useSubscription 이 isLoggedIn() 검사로 즉시 INACTIVE 라 깜빡임 없음.
 */
export default function AdSenseLoader() {
  const pathname = usePathname();
  const { subscription, loading } = useSubscription();

  if (loading) return null;
  if (subscription.removesAds) return null;
  if (isAdExcludedPath(pathname)) return null;

  return (
    <Script
      id="adsbygoogle-loader"
      src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_CLIENT}`}
      crossOrigin="anonymous"
      strategy="lazyOnload"
    />
  );
}
