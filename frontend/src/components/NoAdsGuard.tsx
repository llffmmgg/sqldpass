"use client";

import { useEffect } from "react";

/**
 * 마운트 동안 페이지에서 광고를 CSS 로 숨긴다 (DOM 은 보존).
 * /checkout, /plan 처럼 결제 결정 흐름에서 광고로 시선 분산을 차단.
 *
 * 이전엔 MutationObserver 로 광고 DOM 을 직접 remove() 했으나,
 * 사용자가 다른 페이지로 이동해도 destroy 된 placeholder 가 복구되지 않고
 * window.adsbygoogle 의 슬롯 상태가 깨져 Auto Ads 가 영구 비활성되는
 * 버그가 있었음 (visit /checkout → /home 에서도 광고 안 뜸).
 * body 클래스 + CSS display:none 으로 변경하여 마운트/언마운트만 토글하면
 * 다른 페이지에선 광고가 자동 복원됨.
 */
export default function NoAdsGuard() {
  useEffect(() => {
    document.body.classList.add("no-ads");
    return () => {
      document.body.classList.remove("no-ads");
    };
  }, []);
  return null;
}
