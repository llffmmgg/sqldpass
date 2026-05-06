"use client";

import { useEffect, useRef } from "react";

import { useSubscription } from "@/hooks/useSubscription";

const ADSENSE_CLIENT = "ca-pub-6512792395955186";

interface AdInfeedProps {
  adSlot: string;
  adLayoutKey: string;
  className?: string;
}

export default function AdInfeed({ adSlot, adLayoutKey, className }: AdInfeedProps) {
  const pushed = useRef(false);
  const { subscription } = useSubscription();

  useEffect(() => {
    if (pushed.current) return;
    if (subscription.removesAds) return;
    pushed.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
    } catch {
      // AdSense가 아직 로드되지 않은 경우 등 — 무시
    }
  }, [subscription.removesAds]);

  if (subscription.removesAds) return null;

  return (
    <ins
      className={`adsbygoogle ${className ?? ""}`}
      style={{ display: "block" }}
      data-ad-format="fluid"
      data-ad-layout-key={adLayoutKey}
      data-ad-client={ADSENSE_CLIENT}
      data-ad-slot={adSlot}
    />
  );
}
