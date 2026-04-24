"use client";

import { useEffect, useRef } from "react";

/**
 * 반응형 배너 — 가로는 본문 폭 100% 로 자동 확장, 세로는 고정값.
 *
 * AdSense 반응형(auto) 포맷을 사용하되 height 를 CSS 로 고정해
 * 레이아웃 시프트(CLS) 를 방지하고, 구글은 그 높이에 맞는 광고 포맷
 * (728×90, 336×280, 300×250 등) 을 자동으로 고른다.
 *
 * 데스크톱/모바일 분기 불필요 — 하나의 <ins> 가 전 뷰포트를 커버.
 */
const ADSENSE_CLIENT = "ca-pub-6512792395955186";

interface AdResponsiveProps {
  /** AdSense 에서 발급받은 반응형 슬롯 ID */
  adSlot: string;
  /** 고정 세로 높이 (px). 90(리더보드 느낌) or 280(라지 렉탱글 느낌) 권장 */
  height: number;
  className?: string;
}

export default function AdResponsive({ adSlot, height, className }: AdResponsiveProps) {
  const pushed = useRef(false);

  useEffect(() => {
    if (pushed.current) return;
    pushed.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
    } catch {
      // AdSense 스크립트 미로드 — 무시
    }
  }, []);

  return (
    <aside
      aria-label="광고"
      className={`my-6 mx-auto w-full ${className ?? ""}`}
    >
      <ins
        className="adsbygoogle"
        style={{ display: "block", width: "100%", height }}
        data-ad-client={ADSENSE_CLIENT}
        data-ad-slot={adSlot}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </aside>
  );
}
