"use client";

import { useEffect, useRef } from "react";
import { usePathname } from "next/navigation";

import { useSubscription } from "@/hooks/useSubscription";

const ADSENSE_CLIENT = "ca-pub-6512792395955186";

const EXCLUDED_PREFIXES = [
  "/admin",
  "/profile",
  "/mypage/feedback",
  "/auth/callback",
  // 결제 결정 흐름 — 광고로 시선 분산 방지
  "/checkout",
];

interface AdSidebarProps {
  adSlot: string;
}

export default function AdSidebar({ adSlot }: AdSidebarProps) {
  const pathname = usePathname();
  const pushed = useRef(false);
  const { subscription, loading } = useSubscription();

  useEffect(() => {
    // subscription 응답 받기 전엔 push 보류 — 구독 회원에게 광고가 잠깐이라도 노출되는 걸 방지
    if (loading) return;
    if (pushed.current) return;
    if (subscription.removesAds) return;
    pushed.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
    } catch {
      // AdSense 스크립트가 아직 로드되지 않았을 수 있음 — 무시
    }
  }, [loading, subscription.removesAds]);

  if (!adSlot) return null;
  if (loading) return null;
  // 한달권/무제한권 회원은 광고 제거
  if (subscription.removesAds) return null;
  if (EXCLUDED_PREFIXES.some((prefix) => pathname?.startsWith(prefix))) {
    return null;
  }

  return (
    <aside
      aria-label="광고"
      className="fixed right-4 top-24 z-30 hidden w-[300px] min-[1400px]:block"
    >
      <ins
        className="adsbygoogle"
        style={{ display: "inline-block", width: 300, height: 600 }}
        data-ad-client={ADSENSE_CLIENT}
        data-ad-slot={adSlot}
      />
    </aside>
  );
}
