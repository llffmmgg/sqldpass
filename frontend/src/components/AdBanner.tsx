"use client";

import { useEffect, useRef } from "react";

/**
 * 고정 크기 배너 광고 — 데스크톱/모바일 분기.
 *
 * AdSense 반응형(auto) 포맷과 달리 고정 규격으로 렌더해 "광고 자리"가
 * 레이아웃에 명확히 잡히도록 함. 데스크톱/모바일 슬롯 ID 를 각각 받아
 * 성과(CTR/RPM) 를 위치별로 분리 집계할 수 있다.
 *
 * 슬롯이 빈 문자열이면 해당 플랫폼은 렌더하지 않으므로, AdSense 에서
 * 슬롯 발급 전에도 배포 안전.
 */
const ADSENSE_CLIENT = "ca-pub-6512792395955186";

interface AdBannerProps {
  /** 데스크톱 슬롯 ID (예: 728×90 or 336×280) */
  desktopSlot?: string;
  desktopWidth: number;
  desktopHeight: number;
  /** 모바일 슬롯 ID (320×100 공통) */
  mobileSlot?: string;
  className?: string;
}

export default function AdBanner({
  desktopSlot,
  desktopWidth,
  desktopHeight,
  mobileSlot,
  className,
}: AdBannerProps) {
  const desktopPushed = useRef(false);
  const mobilePushed = useRef(false);

  useEffect(() => {
    if (desktopSlot && !desktopPushed.current) {
      desktopPushed.current = true;
      try {
        (window.adsbygoogle = window.adsbygoogle ?? []).push({});
      } catch {
        // AdSense 스크립트 미로드 — 무시
      }
    }
    if (mobileSlot && !mobilePushed.current) {
      mobilePushed.current = true;
      try {
        (window.adsbygoogle = window.adsbygoogle ?? []).push({});
      } catch {
        // AdSense 스크립트 미로드 — 무시
      }
    }
  }, [desktopSlot, mobileSlot]);

  if (!desktopSlot && !mobileSlot) return null;

  return (
    <aside
      aria-label="광고"
      className={`my-6 flex justify-center ${className ?? ""}`}
    >
      {desktopSlot && (
        <ins
          className="adsbygoogle hidden md:inline-block"
          style={{ width: desktopWidth, height: desktopHeight }}
          data-ad-client={ADSENSE_CLIENT}
          data-ad-slot={desktopSlot}
        />
      )}
      {mobileSlot && (
        <ins
          className="adsbygoogle md:hidden inline-block"
          style={{ width: 320, height: 100 }}
          data-ad-client={ADSENSE_CLIENT}
          data-ad-slot={mobileSlot}
        />
      )}
    </aside>
  );
}
