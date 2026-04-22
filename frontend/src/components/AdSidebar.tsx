"use client";

import { useEffect, useRef } from "react";
import { usePathname } from "next/navigation";

const ADSENSE_CLIENT = "ca-pub-6512792395955186";

const EXCLUDED_PREFIXES = [
  "/admin",
  "/profile",
  "/mypage/feedback",
  "/auth/callback",
];

interface AdSidebarProps {
  adSlot: string;
}

export default function AdSidebar({ adSlot }: AdSidebarProps) {
  const pathname = usePathname();
  const pushed = useRef(false);

  useEffect(() => {
    if (pushed.current) return;
    pushed.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
    } catch {
      // AdSense 스크립트가 아직 로드되지 않았을 수 있음 — 무시
    }
  }, []);

  if (!adSlot) return null;
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
