"use client";

import Link from "next/link";

import {
  type ActiveSubscription,
  type PaymentMethod,
  type PreviewResponse,
  type SubscriptionPlan,
} from "@/lib/payment";

type TierKey = "FREE" | SubscriptionPlan;

type Tier = {
  key: TierKey;
  name: string;
  tagline: string;
  price: number;
  unit?: string;
  features: string[];
  cta: string;
  highlight?: boolean;
};

const TIERS: Tier[] = [
  {
    key: "FREE",
    name: "Free",
    tagline: "기본 문제 풀이 제공",
    price: 0,
    features: ["쉬움/보통 회차", "오답 노트", "대시보드"],
    cta: "현재 플랜",
  },
  {
    key: "THREE_DAY",
    name: "Starter",
    tagline: "시험 직전 단기 점검",
    price: 3900,
    unit: "3일",
    features: ["PASS+ 회차 모두 풀이", "72시간 풀 액세스", "오답 노트 자동 저장"],
    cta: "Starter 시작",
  },
  {
    key: "ONE_MONTH",
    name: "Pro",
    tagline: "한 달 집중 합격 코스",
    price: 9900,
    unit: "30일",
    features: ["PASS+ 회차 무제한", "광고 제거", "30일 풀 액세스"],
    cta: "Pro 시작",
    highlight: true,
  },
  {
    key: "UNLIMITED",
    name: "Lifetime",
    tagline: "한 번 결제로 계속 이용",
    price: 29900,
    unit: "평생",
    features: ["PASS+ 회차 무제한", "광고 제거", "PDF 다운로드", "기간 제한 없음"],
    cta: "Lifetime 시작",
  },
];

type Props = {
  currentPlan: SubscriptionPlan | null;
  previews: Record<SubscriptionPlan, PreviewResponse | null>;
  payingPlan: SubscriptionPlan | null;
  subscription: ActiveSubscription | null;
  method: PaymentMethod;
  onChangeMethod: (m: PaymentMethod) => void;
  showMethodToggle: boolean;
  onPay: (plan: SubscriptionPlan) => void;
};

export default function CheckoutLanding({
  currentPlan,
  previews,
  payingPlan,
  subscription,
  method,
  onChangeMethod,
  showMethodToggle,
  onPay,
}: Props) {
  return (
    <div className="relative">
      {/* 배경 amber radial glow — PASS+ 액센트 */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 -top-24 -z-10 h-[420px] bg-[radial-gradient(ellipse_60%_50%_at_50%_0%,rgba(245,181,68,0.10),transparent_70%)]"
      />

      <header className="text-center">
        <span className="inline-flex items-center gap-2 rounded-full border border-amber-500/30 bg-amber-500/[0.08] px-3 py-1 text-xs font-medium text-amber-700 dark:text-amber-300">
          <span className="relative flex h-1.5 w-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-400 opacity-60" />
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-amber-500" />
          </span>
          요금제
        </span>
        <h1 className="mx-auto mt-5 max-w-2xl text-balance text-4xl font-bold tracking-tight text-text sm:text-5xl">
          내 시험 일정에 맞는
          <br />
          <span className="bg-gradient-to-r from-amber-500 to-amber-700 bg-clip-text text-transparent dark:from-amber-300 dark:to-amber-500">
            플랜을 선택하세요
          </span>
        </h1>
        <p className="mx-auto mt-4 max-w-lg text-sm leading-relaxed text-text-muted sm:text-base">
          필요한 기간만큼 이용하고, 고난이도 모의고사까지 바로 풀어보세요.
        </p>

        {subscription?.active && (
          <div className="mt-6 inline-flex items-center gap-2 rounded-full border border-success/40 bg-success/[0.08] px-4 py-1.5 text-xs font-medium text-success">
            <CheckSvg className="h-3.5 w-3.5" />
            <span>
              현재 {planLabel(subscription.plan!)} 이용 중
              {subscription.expiresAt && (
                <span className="ml-1 opacity-80">
                  · {new Date(subscription.expiresAt).toLocaleDateString("ko-KR")} 만료
                </span>
              )}
              {subscription.expiresAt === null && (
                <span className="ml-1 opacity-80">· 평생</span>
              )}
            </span>
          </div>
        )}
      </header>

      {showMethodToggle && (
        <div className="mt-10 flex flex-col items-center gap-2">
          <span className="text-[11px] font-medium uppercase tracking-[1.4px] text-text-subtle">
            결제 수단
          </span>
          <div
            role="radiogroup"
            aria-label="결제 수단 선택"
            className="inline-flex items-center gap-1 rounded-full border border-border bg-surface/40 p-1"
          >
            <MethodButton
              active={method === "KAKAOPAY"}
              onClick={() => onChangeMethod("KAKAOPAY")}
              label="카카오페이"
            />
            <MethodButton
              active={method === "CARD"}
              onClick={() => onChangeMethod("CARD")}
              label="신용카드"
            />
          </div>
        </div>
      )}

      <div className="mt-10 grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
        {TIERS.map((t) => (
          <PlanCard
            key={t.key}
            tier={t}
            currentPlan={currentPlan}
            preview={t.key === "FREE" ? null : previews[t.key]}
            payingPlan={payingPlan}
            onPay={onPay}
          />
        ))}
      </div>

      {/* FAQ */}
      <div className="mt-16 grid grid-cols-1 gap-6 rounded-2xl border border-border bg-surface/40 p-6 sm:grid-cols-3 sm:gap-8 sm:p-8">
        {[
          ["자동결제 되나요?", "아니요. 한 번 결제 = 한 번만 청구돼요."],
          ["환불은 언제까지?", "결제 후 7일 이내, 사용 이력이 없으면 100% 환불돼요."],
          ["Starter 만료되면?", "다시 결제하거나 Pro 로 이어갈 수 있어요."],
        ].map(([q, a], i) => (
          <div
            key={q}
            className={i === 0 ? "" : "sm:border-l sm:border-border sm:pl-6"}
          >
            <div className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-amber-600 dark:text-amber-400">
              FAQ · {String(i + 1).padStart(2, "0")}
            </div>
            <div className="mt-2 text-[13.5px] font-bold tracking-[-0.01em] text-text">
              {q}
            </div>
            <div className="mt-1.5 text-[12.5px] leading-[1.65] text-text-muted">
              {a}
            </div>
          </div>
        ))}
      </div>

      <p className="mt-6 text-center text-[11px] text-text-subtle">
        결제 시{" "}
        <Link
          href="/terms"
          target="_blank"
          rel="noopener noreferrer"
          className="underline transition-colors hover:text-text"
        >
          이용약관
        </Link>
        {" "}및{" "}
        <Link
          href="/refund"
          target="_blank"
          rel="noopener noreferrer"
          className="underline transition-colors hover:text-text"
        >
          환불 정책
        </Link>
        에 동의하는 것으로 간주됩니다.
      </p>
    </div>
  );
}

function PlanCard({
  tier,
  currentPlan,
  preview,
  payingPlan,
  onPay,
}: {
  tier: Tier;
  currentPlan: SubscriptionPlan | null;
  preview: PreviewResponse | null;
  payingPlan: SubscriptionPlan | null;
  onPay: (plan: SubscriptionPlan) => void;
}) {
  const { highlight } = tier;
  const isFree = tier.key === "FREE";
  const isCurrent = !isFree && currentPlan === tier.key;
  const isPaying = !isFree && payingPlan === tier.key;
  const isBlocked = preview ? !preview.allowed : false;
  const hasProrate = preview ? preview.allowed && preview.prorateDiscount > 0 : false;
  const disabled = payingPlan !== null;

  return (
    <div
      className={`relative flex flex-col rounded-2xl p-7 transition-all duration-300 ${
        highlight
          ? "border-2 border-amber-500/60 bg-gradient-to-b from-amber-500/[0.10] via-surface to-surface/80 shadow-[0_0_40px_-10px_rgba(245,181,68,0.45)] hover:-translate-y-1 hover:shadow-[0_0_60px_-10px_rgba(245,181,68,0.6)] xl:-translate-y-2"
          : "border border-border bg-surface/40 hover:-translate-y-0.5 hover:border-border-strong hover:bg-surface/70 hover:shadow-[var(--shadow-md)]"
      }`}
    >
      {highlight && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2">
          <span className="inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-amber-400 to-amber-600 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-amber-950 shadow-lg shadow-amber-500/40">
            <StarSvg className="h-3 w-3" />
            가장 인기
          </span>
        </div>
      )}

      <span
        className={`font-mono text-[10.5px] font-medium uppercase tracking-[1.4px] ${
          highlight ? "text-amber-600 dark:text-amber-400" : "text-text-subtle"
        }`}
      >
        {tier.unit ?? "FREE"}
      </span>

      <h3 className="mt-2 text-lg font-bold tracking-tight text-text">{tier.name}</h3>
      <p className="mt-1 min-h-[36px] text-xs leading-snug text-text-muted">
        {tier.tagline}
      </p>

      {/* 가격 — prorate 분기 */}
      <div className="mt-5">
        {hasProrate && preview ? (
          <>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold tabular-nums tracking-tight text-text">
                ₩{preview.finalAmount.toLocaleString()}
              </span>
              <span className="text-sm tabular-nums text-text-subtle line-through">
                ₩{preview.baseAmount.toLocaleString()}
              </span>
            </div>
            <p className="mt-1 text-xs text-success">
              현재 구독 잔여 ₩{preview.prorateDiscount.toLocaleString()} 차감
            </p>
          </>
        ) : (
          <div className="flex items-baseline gap-1">
            <span className="text-3xl font-bold tabular-nums tracking-tight text-text">
              {tier.price === 0 ? "무료" : `₩${tier.price.toLocaleString()}`}
            </span>
            {tier.price > 0 && tier.unit && (
              <span className="text-xs text-text-subtle">/ {tier.unit}</span>
            )}
          </div>
        )}
      </div>

      <div
        className={`mt-5 h-px ${
          highlight
            ? "bg-[linear-gradient(90deg,transparent,rgba(245,181,68,0.35)_50%,transparent)]"
            : "bg-[linear-gradient(90deg,transparent,var(--border)_50%,transparent)]"
        }`}
      />

      <ul className="mt-5 flex flex-1 flex-col gap-2.5">
        {tier.features.map((f, i) => (
          <li
            key={i}
            className="flex items-start gap-2.5 text-sm leading-snug text-text"
          >
            <CheckSvg
              className={`mt-0.5 h-4 w-4 flex-shrink-0 ${
                highlight ? "text-amber-500 dark:text-amber-300" : "text-primary"
              }`}
            />
            <span>{f}</span>
          </li>
        ))}
      </ul>

      {/* 버튼 — 5가지 상태 분기 */}
      <div className="mt-7">
        {isFree ? (
          <button
            type="button"
            disabled
            className="w-full cursor-default rounded-lg border border-dashed border-border bg-transparent px-4 py-2.5 text-sm font-medium text-text-muted"
          >
            현재 플랜
          </button>
        ) : isCurrent ? (
          <button
            type="button"
            disabled
            className="w-full cursor-default rounded-lg border border-success/40 bg-success/[0.08] px-4 py-2.5 text-sm font-semibold text-success"
          >
            ✓ 이용 중
          </button>
        ) : isBlocked ? (
          <div>
            <button
              type="button"
              disabled
              className="w-full cursor-default rounded-lg border border-border bg-transparent px-4 py-2.5 text-sm font-medium text-text-subtle"
            >
              현재 구독 종료 후 가능
            </button>
            {preview?.reason && (
              <p className="mt-2 text-[11px] leading-tight text-text-subtle">
                {preview.reason}
              </p>
            )}
          </div>
        ) : (
          <button
            type="button"
            onClick={() => onPay(tier.key as SubscriptionPlan)}
            disabled={disabled}
            className={`w-full rounded-lg px-4 py-2.5 text-sm font-semibold transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-60 ${
              highlight
                ? "bg-gradient-to-r from-amber-400 to-amber-500 text-amber-950 shadow-lg shadow-amber-500/30 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-amber-500/45 hover:brightness-105"
                : "border border-border bg-surface text-text hover:-translate-y-0.5 hover:border-amber-500/40 hover:bg-surface-hover"
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
              hasProrate ? "업그레이드" : tier.cta
            )}
          </button>
        )}
      </div>
    </div>
  );
}

function MethodButton({
  active,
  onClick,
  label,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={active}
      onClick={onClick}
      className={`rounded-full px-4 py-1.5 text-xs font-semibold transition-all ${
        active
          ? "bg-amber-500 text-amber-950 shadow-[0_0_18px_-4px_rgba(245,181,68,0.6)]"
          : "text-text-muted hover:text-text"
      }`}
    >
      {label}
    </button>
  );
}

function CheckSvg({ className = "" }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M5 13l4 4L19 7" />
    </svg>
  );
}

function StarSvg({ className = "" }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

export function planLabel(plan: SubscriptionPlan): string {
  switch (plan) {
    case "THREE_DAY":
      return "Starter";
    case "ONE_MONTH":
      return "Pro";
    case "UNLIMITED":
      return "Lifetime";
  }
}
