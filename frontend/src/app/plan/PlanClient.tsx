"use client";

import { useRouter } from "next/navigation";

import NoAdsGuard from "@/components/NoAdsGuard";
import CheckoutLanding from "@/components/billing/CheckoutLanding";

// /plan 은 가격 카탈로그 — 실제 결제 흐름(eligibility, prorate, PG SDK)은 /checkout 이 소유.
// 동일 카드 UI(CheckoutLanding)를 재사용해 디자인 분기 부담을 피하고,
// CTA 클릭 시 /checkout 으로 보내 베타 게이트/로그인 분기를 일원화한다.
export default function PlanClient() {
  const router = useRouter();

  return (
    <>
      <NoAdsGuard />
      <CheckoutLanding
        currentPlan={null}
        previews={{ THREE_DAY: null, FOCUS: null, ONE_MONTH: null, UNLIMITED: null }}
        payingPlan={null}
        subscription={null}
        method="CARD"
        onChangeMethod={() => {}}
        showMethodToggle={false}
        onPay={() => router.push("/checkout")}
      />
    </>
  );
}
