"use client";

import { useEffect } from "react";

const AD_SELECTORS = [
  "ins.adsbygoogle",
  ".google-auto-placed",
  'iframe[id^="google_ads_iframe"]',
  'iframe[id^="aswift_"]',
  'iframe[src*="googlesyndication"]',
  'iframe[src*="googleads"]',
  "div[data-google-query-id]",
  "div[data-ad-status]",
];

/**
 * 마운트되어 있는 동안 그 페이지의 모든 AdSense / Auto Ads 광고 DOM 을
 * 즉시 제거한다. /checkout 처럼 어떤 광고도 노출되면 안 되는 페이지에 둔다.
 *
 * AdSenseLoader 가 lib/adsExcludedPrefixes 경로에서 스크립트를 마운트하지
 * 않더라도, 다른 페이지에서 이미 로드된 adsbygoogle.js 와 window.adsbygoogle
 * 이 메모리에 남아 SPA 라우팅 후에도 광고를 재삽입할 수 있다. MutationObserver
 * 로 그 잔존 삽입을 따라가며 제거한다.
 */
export default function NoAdsGuard() {
  useEffect(() => {
    const sweep = () => {
      AD_SELECTORS.forEach((sel) => {
        document.querySelectorAll(sel).forEach((el) => el.remove());
      });
    };
    sweep();
    const observer = new MutationObserver(sweep);
    observer.observe(document.body, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, []);
  return null;
}
