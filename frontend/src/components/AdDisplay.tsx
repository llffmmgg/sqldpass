"use client";

import { useEffect, useRef } from "react";

import { useSubscription } from "@/hooks/useSubscription";

const ADSENSE_CLIENT = "ca-pub-6512792395955186";

interface AdDisplayProps {
  adSlot: string;
  className?: string;
}

export default function AdDisplay({ adSlot, className }: AdDisplayProps) {
  const pushed = useRef(false);
  const { subscription, loading } = useSubscription();

  useEffect(() => {
    if (loading) return;
    if (pushed.current) return;
    if (subscription.removesAds) return;
    pushed.current = true;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
    } catch {
      // AdSense가 아직 로드되지 않은 경우 등 — 무시
    }
  }, [loading, subscription.removesAds]);

  if (loading) return null;
  if (subscription.removesAds) return null;

  return (
    <ins
      className={`adsbygoogle ${className ?? ""}`}
      style={{ display: "block" }}
      data-ad-client={ADSENSE_CLIENT}
      data-ad-slot={adSlot}
      data-ad-format="auto"
      data-ad-full-width-responsive="true"
    />
  );
}
