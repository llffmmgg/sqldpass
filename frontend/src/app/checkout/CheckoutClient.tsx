"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { Badge } from "@/components/ui";
import Spinner from "@/components/Spinner";
import { useToast } from "@/components/Toast";
import { isLoggedIn } from "@/lib/auth";
import {
  getActiveSubscription,
  getCheckoutEligibility,
  startPayment,
  type ActiveSubscription,
  type SubscriptionPlan,
} from "@/lib/payment";

type AccessState = "loading" | "anonymous" | "denied" | "allowed";

type TierKey = "FREE" | SubscriptionPlan;

type Tier = {
  key: TierKey;
  name: string;
  tagline: string;
  price: number; // 0 = 무료
  priceUnit: string;
  features: string[];
  notIncluded?: string[];
  cta: string;
  highlight?: boolean;
};

const TIERS: Tier[] = [
  {
    key: "FREE",
    name: "Free",
    tagline: "기본 기능을 무료로",
    price: 0,
    priceUnit: "영구",
    features: [
      "쉬움/보통 모의고사 풀이",
      "오답 노트·대시보드",
      "회차별 실력 추적",
    ],
    notIncluded: ["프리미엄 모의고사", "광고 제거", "PDF 다운로드"],
    cta: "현재 플랜",
  },
  {
    key: "THREE_DAY",
    name: "3일권",
    tagline: "시험 직전 단기 점검",
    price: 3900,
    priceUnit: "3일 동안",
    features: [
      "모든 무료 기능",
      "프리미엄(어려움/매우 어려움) 모의고사",
      "결제 후 72시간 풀이",
    ],
    notIncluded: ["광고 제거", "PDF 다운로드"],
    cta: "3일권 시작",
  },
  {
    key: "ONE_MONTH",
    name: "한달권",
    tagline: "한 달 집중 합격 코스",
    price: 9900,
    priceUnit: "30일 동안",
    features: [
      "모든 무료 기능",
      "프리미엄 모의고사 무제한",
      "광고 제거",
      "결제 후 30일 풀이",
    ],
    notIncluded: ["PDF 다운로드"],
    cta: "한달권 시작",
    highlight: true,
  },
  {
    key: "UNLIMITED",
    name: "무제한",
    tagline: "한 번 결제로 평생",
    price: 29900,
    priceUnit: "평생",
    features: [
      "모든 무료 기능",
      "프리미엄 모의고사 무제한",
      "광고 제거",
      "시험지 PDF 다운로드",
      "기간 제한 없음",
    ],
    cta: "평생 잠금 해제",
  },
];

export default function CheckoutClient() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[40vh] items-center justify-center">
          <Spinner message="확인 중..." />
        </div>
      }
    >
      <CheckoutContent />
    </Suspense>
  );
}

function CheckoutContent() {
  const router = useRouter();
  const toast = useToast();
  const [access, setAccess] = useState<AccessState>("loading");
  const [subscription, setSubscription] = useState<ActiveSubscription | null>(null);
  const [payingPlan, setPayingPlan] = useState<SubscriptionPlan | null>(null);

  useEffect(() => {
    if (!isLoggedIn()) {
      setAccess("anonymous");
      return;
    }
    Promise.all([getCheckoutEligibility(), getActiveSubscription()])
      .then(([elig, sub]) => {
        setAccess(elig.eligible ? "allowed" : "denied");
        setSubscription(sub);
      })
      .catch(() => setAccess("denied"));
  }, []);

  if (access === "loading") {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <Spinner message="확인 중..." />
      </div>
    );
  }

  if (access === "anonymous") {
    return <ComingSoonView showLogin />;
  }
  if (access === "denied") {
    return <ComingSoonView showLogin={false} />;
  }

  async function onPay(plan: SubscriptionPlan) {
    if (payingPlan) return;
    setPayingPlan(plan);
    try {
      const result = await startPayment({ plan });
      toast.show(`${planLabel(result.plan)} 결제 완료`, "success");
      setTimeout(() => router.push("/mock-exams"), 800);
    } catch (e) {
      const message = e instanceof Error ? e.message : "";
      // PortOne SDK 의 사용자 취소는 message 안에 "취소" / "cancel" 키워드 포함 — info 톤
      const cancelled = /취소|cancel/i.test(message);
      if (cancelled) {
        toast.show("결제를 취소하셨습니다.", "info");
      } else {
        // 그 외는 백엔드 검증 실패 / 네트워크 오류 — 일반 사용자 친화 메시지로
        toast.show(
          message || "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
          "error",
        );
      }
      // 디버깅용 — 콘솔에는 원문 남김
      if (e instanceof Error) console.error("[checkout]", e);
    } finally {
      setPayingPlan(null);
    }
  }

  return (
    <div className="relative">
      {/* 배경 그라데이션 — 보라 hero 톤 */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 -top-24 -z-10 h-[420px] bg-gradient-to-b from-primary/[0.08] via-primary/[0.02] to-transparent"
      />

      <header className="text-center">
        <span className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/[0.08] px-3 py-1 text-xs font-medium text-primary">
          <span className="relative flex h-1.5 w-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-60" />
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-primary" />
          </span>
          요금제
        </span>
        <h1 className="mx-auto mt-5 max-w-2xl text-balance text-4xl font-bold tracking-tight sm:text-5xl">
          내 합격 속도에 맞춘
          <br />
          <span className="bg-gradient-to-r from-primary to-[#5ee0a5] bg-clip-text text-transparent">
            단순한 요금제
          </span>
        </h1>
        <p className="mx-auto mt-4 max-w-lg text-sm leading-relaxed text-text-muted sm:text-base">
          숨겨진 비용 없이, 한 번 결제로 끝.
          {" "}모든 결제는 PortOne(코리아포트원) 을 통해 안전하게 처리됩니다.
        </p>

        {subscription?.active && (
          <div className="mt-6 inline-flex items-center gap-2 rounded-full border border-success/40 bg-success/[0.08] px-4 py-1.5 text-xs font-medium text-success">
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
            <span>
              현재 {planLabel(subscription.plan!)} 이용 중
              {subscription.expiresAt && (
                <span className="ml-1 text-success/80">
                  · {new Date(subscription.expiresAt).toLocaleDateString("ko-KR")} 만료
                </span>
              )}
              {subscription.expiresAt === null && (
                <span className="ml-1 text-success/80">· 평생</span>
              )}
            </span>
          </div>
        )}
      </header>

      <div className="mt-14 grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {TIERS.map((tier) => (
          <TierCard
            key={tier.key}
            tier={tier}
            onPay={onPay}
            disabled={payingPlan !== null}
            currentPlan={subscription?.active ? subscription.plan : null}
            payingPlan={payingPlan}
          />
        ))}
      </div>

      {/* 신뢰 요소 */}
      <div className="mt-14 flex flex-wrap items-center justify-center gap-x-8 gap-y-3 text-xs text-text-subtle">
        <span className="inline-flex items-center gap-1.5">
          <svg className="h-3.5 w-3.5 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          PortOne 안전 결제
        </span>
        <span className="inline-flex items-center gap-1.5">
          <svg className="h-3.5 w-3.5 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
          </svg>
          신용·체크카드
        </span>
        <span className="inline-flex items-center gap-1.5">
          <svg className="h-3.5 w-3.5 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M16 15v-1a4 4 0 00-4-4H8m0 0l3 3m-3-3l3-3m9 14V5a2 2 0 00-2-2H6a2 2 0 00-2 2v16l4-2 4 2 4-2 4 2z" />
          </svg>
          7일 환불 보장
        </span>
      </div>

      <p className="mt-8 text-center text-[11px] text-text-subtle">
        결제 진행 시{" "}
        <Link href="/terms" className="underline-offset-2 hover:underline">이용약관</Link>
        {" "}및{" "}
        <Link href="/refund" className="underline-offset-2 hover:underline">환불 정책</Link>
        에 동의하는 것으로 간주됩니다.
      </p>
    </div>
  );
}

function TierCard({
  tier,
  onPay,
  disabled,
  currentPlan,
  payingPlan,
}: {
  tier: Tier;
  onPay: (plan: SubscriptionPlan) => void;
  disabled: boolean;
  currentPlan: SubscriptionPlan | null;
  payingPlan: SubscriptionPlan | null;
}) {
  const isFree = tier.key === "FREE";
  const isCurrent = !isFree && currentPlan === tier.key;
  const isPaying = payingPlan === tier.key;

  return (
    <div
      className={`relative flex flex-col rounded-2xl p-7 transition-all duration-300 ${
        tier.highlight
          ? "border-2 border-primary/60 bg-gradient-to-b from-primary/[0.08] via-bg to-bg shadow-[0_0_40px_-10px_rgba(124,92,196,0.45)] xl:-translate-y-2"
          : "border border-border bg-surface/40 hover:border-border-strong hover:bg-surface/70"
      }`}
    >
      {tier.highlight && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2">
          <span className="inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-primary to-[#5ee0a5] px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-white shadow-lg">
            <svg className="h-3 w-3" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
            </svg>
            가장 인기
          </span>
        </div>
      )}

      <div>
        <h3 className="text-lg font-bold tracking-tight">{tier.name}</h3>
        <p className="mt-1 text-xs text-text-muted">{tier.tagline}</p>
      </div>

      <div className="mt-6 flex items-baseline gap-1">
        <span className="text-4xl font-bold tabular-nums tracking-tight">
          {tier.price === 0 ? "무료" : `₩${tier.price.toLocaleString()}`}
        </span>
      </div>
      <p className="mt-1 text-xs text-text-subtle">{tier.priceUnit}</p>

      <ul className="mt-7 flex-1 space-y-2.5">
        {tier.features.map((f, i) => (
          <li key={i} className="flex items-start gap-2.5 text-sm text-text">
            <CheckIcon />
            <span className="leading-snug">{f}</span>
          </li>
        ))}
        {tier.notIncluded?.map((f, i) => (
          <li key={`x-${i}`} className="flex items-start gap-2.5 text-sm text-text-subtle">
            <CrossIcon />
            <span className="leading-snug line-through">{f}</span>
          </li>
        ))}
      </ul>

      <div className="mt-8">
        {isFree ? (
          <button
            disabled
            className="w-full rounded-lg border border-border bg-transparent px-4 py-2.5 text-sm font-medium text-text-muted"
          >
            {tier.cta}
          </button>
        ) : isCurrent ? (
          <button
            disabled
            className="w-full rounded-lg border border-success/40 bg-success/[0.08] px-4 py-2.5 text-sm font-semibold text-success"
          >
            ✓ 이용 중
          </button>
        ) : (
          <button
            onClick={() => onPay(tier.key as SubscriptionPlan)}
            disabled={disabled}
            className={`w-full rounded-lg px-4 py-2.5 text-sm font-semibold transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-60 ${
              tier.highlight
                ? "bg-gradient-to-r from-primary to-[#5ee0a5] text-white shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/40 hover:-translate-y-0.5"
                : "border border-border bg-surface text-text hover:border-primary/40 hover:bg-surface-hover"
            }`}
          >
            {isPaying ? (
              <span className="inline-flex items-center justify-center gap-2">
                <svg className="h-3.5 w-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth={3} className="opacity-25" />
                  <path fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" className="opacity-75" />
                </svg>
                결제 진행 중...
              </span>
            ) : (
              tier.cta
            )}
          </button>
        )}
      </div>
    </div>
  );
}

function CheckIcon() {
  return (
    <svg className="mt-0.5 h-4 w-4 shrink-0 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
    </svg>
  );
}

function CrossIcon() {
  return (
    <svg className="mt-0.5 h-4 w-4 shrink-0 text-text-subtle" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
    </svg>
  );
}

function planLabel(plan: SubscriptionPlan): string {
  switch (plan) {
    case "THREE_DAY":
      return "3일권";
    case "ONE_MONTH":
      return "한달권";
    case "UNLIMITED":
      return "무제한";
  }
}

function ComingSoonView({ showLogin }: { showLogin: boolean }) {
  return (
    <div className="mx-auto max-w-md text-center">
      <div className="rounded-2xl border border-border bg-surface/40 p-10">
        <Badge variant="soft" tone="info" size="sm">
          준비 중
        </Badge>
        <h1 className="mt-4 text-2xl font-bold tracking-tight">
          결제 페이지는 곧 오픈됩니다
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-text-muted">
          지금은 카드사 심사 단계라 일부 운영자 계정에서만 결제가 가능합니다.
          <br />
          정식 오픈 안내는 사이트 공지사항으로 알려드릴게요.
        </p>
        <div className="mt-6 flex flex-col items-center gap-3 text-sm">
          {showLogin && (
            <p className="text-xs text-text-subtle">로그인이 필요할 수도 있습니다.</p>
          )}
          <Link
            href="/mock-exams"
            className="rounded-lg border border-border bg-surface px-5 py-2.5 text-text transition-colors hover:border-primary/40"
          >
            모의고사로 →
          </Link>
        </div>
      </div>
    </div>
  );
}
