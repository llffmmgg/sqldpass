"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { Badge, Button, Card } from "@/components/ui";
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
  price: string;
  priceNote: string;
  duration: string;
  highlights: { label: string; included: boolean }[];
  cta: string;
  highlight?: boolean;
};

const TIERS: Tier[] = [
  {
    key: "FREE",
    name: "무료",
    price: "₩0",
    priceNote: "영구",
    duration: "기본 학습 도구",
    highlights: [
      { label: "쉬움/보통 모의고사 풀이", included: true },
      { label: "오답 노트·대시보드", included: true },
      { label: "프리미엄(어려움/매우 어려움) 모의고사", included: false },
      { label: "광고 제거", included: false },
      { label: "PDF 다운로드", included: false },
    ],
    cta: "현재 플랜",
  },
  {
    key: "THREE_DAY",
    name: "3일권",
    price: "₩3,900",
    priceNote: "3일",
    duration: "구매 후 72시간",
    highlights: [
      { label: "모든 무료 기능", included: true },
      { label: "프리미엄 모의고사 풀이", included: true },
      { label: "광고 제거", included: false },
      { label: "PDF 다운로드", included: false },
    ],
    cta: "3일권 결제",
  },
  {
    key: "ONE_MONTH",
    name: "한달권",
    price: "₩9,900",
    priceNote: "30일",
    duration: "구매 후 30일",
    highlights: [
      { label: "모든 무료 기능", included: true },
      { label: "프리미엄 모의고사 풀이", included: true },
      { label: "광고 제거", included: true },
      { label: "PDF 다운로드", included: false },
    ],
    cta: "한달권 결제",
    highlight: true,
  },
  {
    key: "UNLIMITED",
    name: "무제한권",
    price: "₩29,900",
    priceNote: "평생",
    duration: "계정 종료까지",
    highlights: [
      { label: "모든 무료 기능", included: true },
      { label: "프리미엄 모의고사 풀이", included: true },
      { label: "광고 제거", included: true },
      { label: "시험지 PDF 다운로드", included: true },
    ],
    cta: "무제한권 결제",
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
      // 0.8초 후 모의고사 목록으로 이동
      setTimeout(() => router.push("/mock-exams"), 800);
    } catch (e) {
      toast.show(
        e instanceof Error ? e.message : "결제 처리 중 오류가 발생했습니다.",
        "error",
      );
    } finally {
      setPayingPlan(null);
    }
  }

  return (
    <div>
      <div className="mb-8 text-center">
        <Badge variant="soft" tone="info" size="sm">
          요금제
        </Badge>
        <h1 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">
          내 합격 속도에 맞게 골라보세요
        </h1>
        <p className="mt-3 text-sm text-text-muted">
          모든 결제는 PortOne(코리아포트원) 을 통해 안전하게 처리됩니다.
        </p>
        {subscription?.active && (
          <p className="mt-4 inline-flex items-center gap-2 rounded-full border border-success/40 bg-success/10 px-3 py-1 text-xs text-success">
            현재 구독: {planLabel(subscription.plan!)}
            {subscription.expiresAt && (
              <span className="text-success/80">
                · 만료 {new Date(subscription.expiresAt).toLocaleDateString("ko-KR")}
              </span>
            )}
            {subscription.expiresAt === null && <span className="text-success/80">· 평생</span>}
          </p>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
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

      <p className="mt-8 text-center text-[11px] text-text-subtle">
        결제 진행 시{" "}
        <Link href="/terms" className="underline">
          이용약관
        </Link>{" "}
        및{" "}
        <Link href="/refund" className="underline">
          환불 정책
        </Link>
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
    <Card
      padding="lg"
      className={
        tier.highlight
          ? "relative border-primary/40 bg-gradient-to-b from-primary/[0.06] to-transparent"
          : "relative"
      }
    >
      {tier.highlight && (
        <span className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-primary px-3 py-1 text-[10px] font-bold text-primary-fg">
          가장 인기
        </span>
      )}
      <h2 className="text-lg font-bold tracking-tight">{tier.name}</h2>
      <p className="mt-1 text-xs text-text-muted">{tier.duration}</p>
      <p className="mt-4 text-3xl font-bold tabular-nums">{tier.price}</p>
      <p className="text-[11px] text-text-subtle">{tier.priceNote}</p>

      <ul className="mt-5 space-y-2">
        {tier.highlights.map((h, i) => (
          <li key={i} className={`flex items-start gap-2 text-xs ${h.included ? "text-text" : "text-text-subtle line-through"}`}>
            <span className={h.included ? "text-success" : "text-text-subtle"}>{h.included ? "✓" : "—"}</span>
            <span>{h.label}</span>
          </li>
        ))}
      </ul>

      <div className="mt-6">
        {isFree ? (
          <Button variant="ghost" size="md" className="w-full" disabled>
            {tier.cta}
          </Button>
        ) : isCurrent ? (
          <Button variant="ghost" size="md" className="w-full" disabled>
            이용 중
          </Button>
        ) : (
          <Button
            variant={tier.highlight ? "primary" : "outline"}
            size="md"
            className="w-full"
            onClick={() => onPay(tier.key as SubscriptionPlan)}
            disabled={disabled}
          >
            {isPaying ? "결제 진행 중..." : tier.cta}
          </Button>
        )}
      </div>
    </Card>
  );
}

function planLabel(plan: SubscriptionPlan): string {
  switch (plan) {
    case "THREE_DAY":
      return "3일권";
    case "ONE_MONTH":
      return "한달권";
    case "UNLIMITED":
      return "무제한권";
  }
}

function ComingSoonView({ showLogin }: { showLogin: boolean }) {
  return (
    <Card padding="lg" className="mx-auto max-w-md text-center">
      <Badge variant="soft" tone="info" size="sm">
        준비 중
      </Badge>
      <h1 className="mt-3 text-2xl font-bold tracking-tight">
        결제 페이지는 곧 오픈됩니다
      </h1>
      <p className="mt-3 text-sm leading-relaxed text-text-muted">
        지금은 카드사 심사 단계라 일부 운영자 계정에서만 결제가 가능합니다.
        <br />
        정식 오픈 안내는 사이트 공지사항으로 알려드릴게요.
      </p>
      <div className="mt-6 flex flex-col items-center gap-2 text-sm">
        {showLogin && (
          <p className="text-xs text-text-subtle">로그인이 필요할 수도 있습니다.</p>
        )}
        <Link
          href="/mock-exams"
          className="rounded-lg border border-border bg-surface px-4 py-2 text-text transition-colors hover:border-primary/40"
        >
          모의고사로 →
        </Link>
      </div>
    </Card>
  );
}
