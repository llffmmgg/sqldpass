"use client";

import Link from "next/link";

import {
  type ActiveSubscription,
  type PaymentMethod,
  type PreviewResponse,
  type SubscriptionPlan,
} from "@/lib/payment";

type TierKey = "FREE" | SubscriptionPlan;

type Feature = { text: string; muted?: boolean };

type Tier = {
  key: TierKey;
  name: string;
  tagline: string;
  price: number;
  /** 정가 (할인 어필용) — 있으면 가격 위에 line-through 로 표시. 실제 결제 금액은 price 가 아닌 백엔드 baseAmount. */
  originalPrice?: number;
  unit?: string;
  features: Feature[];
  cta: string;
  highlight?: boolean;
};

const FREE_BASELINE: Feature[] = [
  { text: "쉬움/보통 회차", muted: true },
  { text: "오답 노트", muted: true },
  { text: "대시보드", muted: true },
];

const TIERS: Tier[] = [
  {
    key: "FREE",
    name: "Free",
    tagline: "기본 문제 풀이 제공",
    price: 0,
    features: [
      { text: "쉬움/보통 회차" },
      { text: "오답 노트" },
      { text: "대시보드" },
    ],
    cta: "현재 플랜",
  },
  {
    key: "THREE_DAY",
    name: "Starter",
    tagline: "시험 직전 단기 점검",
    price: 3900,
    unit: "3일",
    features: [
      { text: "PASS+ 회차 풀이" },
      { text: "72시간 풀 액세스" },
      ...FREE_BASELINE,
    ],
    cta: "Starter 시작",
  },
  {
    key: "ONE_MONTH",
    name: "Pro",
    tagline: "한 달 집중 합격 코스",
    price: 9900,
    originalPrice: 12900,
    unit: "30일",
    features: [
      { text: "PASS+ 회차 무제한" },
      { text: "30일 풀 액세스" },
      { text: "광고 제거" },
      ...FREE_BASELINE,
    ],
    cta: "Pro 시작",
    highlight: true,
  },
  {
    key: "UNLIMITED",
    name: "Lifetime",
    tagline: "한 번 결제로 계속 이용",
    price: 29900,
    unit: "평생",
    features: [
      { text: "PASS+ 회차 무제한" },
      { text: "앞으로 추가될 회차까지 무제한" },
      { text: "기간 제한 없음" },
      { text: "광고 제거" },
      { text: "PDF 다운로드" },
      ...FREE_BASELINE,
    ],
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
      {/* 배경 primary radial glow — Supabase 톤 */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-x-0 -top-24 -z-10 h-[420px] bg-[radial-gradient(ellipse_60%_50%_at_50%_0%,rgba(62,207,142,0.10),transparent_70%)]"
      />

      <header className="text-center">
        <span className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
          <span className="relative flex h-1.5 w-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-60" />
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-primary" />
          </span>
          요금제
        </span>
        <h1 className="mx-auto mt-5 max-w-2xl text-balance text-4xl font-bold tracking-tight text-text sm:text-5xl">
          내 시험 일정에 맞는
          <br />
          <span className="bg-gradient-to-r from-[var(--primary)] to-[var(--primary-hover)] bg-clip-text text-transparent">
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

      <p className="mt-6 text-center text-xs text-text-muted">
        PASS+ 모의고사는 주기적으로 새 회차가 추가되며, 유료 플랜 이용 기간 동안 새로 추가되는 회차도
        그대로 이용할 수 있어요.
      </p>

      {/* 구매 유도 — 시험 난이도 변화 (실제 합격률 데이터 기반) */}
      <div className="mx-auto mt-16 max-w-2xl text-center">
        <h2 className="text-2xl font-bold tracking-tight text-text sm:text-3xl">
          최근 자격증 시험이 점점 어려워지고 있어요
        </h2>
        <p className="mx-auto mt-3 max-w-xl text-sm leading-relaxed text-text-muted sm:text-base">
          컴퓨터활용능력 1급 실기는 2024년 개정 이후 합격률{" "}
          <span className="font-semibold text-text">6.9%</span>로 이전 평균 대비 절반 아래로
          떨어졌고, 정보처리기사 실기도 2025년 1회{" "}
          <span className="font-semibold text-text">15.1%</span>로 최근 회차 중 낮은 편이에요.
          기출만으로는 채우기 어려운 신유형, PASS+ 회차로 미리 풀어보세요.
        </p>
        <p className="mx-auto mt-3 text-[11px] text-text-subtle">
          자료: 큐넷·대한상공회의소 공개 자격 통계
        </p>
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
            <div className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-primary">
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

  const inlineBadge: { label: string; tone: "neutral" | "primary" } | null = isFree
    ? { label: "현재 플랜", tone: "neutral" }
    : highlight
    ? { label: "가장 인기", tone: "primary" }
    : null;

  return (
    <div
      className={`relative flex flex-col rounded-2xl p-7 transition-colors duration-200 ${
        highlight
          ? "border-2 border-primary/60 bg-gradient-to-b from-primary/[0.06] via-surface to-surface/80 shadow-[0_0_24px_-12px_rgba(62,207,142,0.30)]"
          : "border border-border bg-surface/40 hover:border-border-strong hover:bg-surface/70"
      }`}
    >
      {/* 헤더 — 플랜명 + 인라인 뱃지 */}
      <div className="flex items-center justify-between gap-3">
        <h3
          className={`text-2xl font-bold tracking-tight ${
            highlight ? "text-primary" : "text-text"
          }`}
        >
          {tier.name}
        </h3>
        {inlineBadge && (
          <span
            className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10.5px] font-semibold uppercase tracking-wider ${
              inlineBadge.tone === "primary"
                ? "bg-primary/15 text-primary"
                : "border border-border bg-bg-elevated text-text-muted"
            }`}
          >
            {inlineBadge.tone === "primary" && <StarSvg className="mr-1 h-3 w-3" />}
            {inlineBadge.label}
          </span>
        )}
      </div>

      {/* 가격 — prorate 분기 */}
      <div className="mt-5">
        {hasProrate && preview ? (
          <>
            <div className="flex items-baseline gap-1.5">
              <span className="text-3xl font-bold tabular-nums tracking-tight text-text">
                ₩{preview.finalAmount.toLocaleString()}
              </span>
              {tier.unit && (
                <span className="text-sm text-text-subtle">/ {tier.unit}</span>
              )}
            </div>
            <p className="mt-1 text-xs tabular-nums text-text-subtle line-through">
              ₩{preview.baseAmount.toLocaleString()}
            </p>
            <p className="mt-1 text-xs text-success">
              현재 구독 잔여 ₩{preview.prorateDiscount.toLocaleString()} 차감
            </p>
          </>
        ) : (
          <>
            {/*
             * line-through 정가를 ₩9,900 옆 + 위쪽 정렬(self-start) 로 '대각선 위' 효과.
             * Pro 만 표시, 다른 카드는 line-through 없이 가격 + unit 만 — 가격 줄이 1줄이라
             * 모든 카드 가격 영역 동일 height, CTA 동일 위치 유지.
             */}
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold tabular-nums tracking-tight text-text">
                {tier.price === 0 ? "무료" : `₩${tier.price.toLocaleString()}`}
              </span>
              {tier.originalPrice && tier.originalPrice > tier.price && (
                <span className="self-start text-xs tabular-nums text-text-subtle line-through">
                  ₩{tier.originalPrice.toLocaleString()}
                </span>
              )}
              {tier.price > 0 && tier.unit && (
                <span className="text-sm text-text-subtle">/ {tier.unit}</span>
              )}
            </div>
          </>
        )}
      </div>

      {/* CTA 버튼 — 5가지 상태 분기 (가격 바로 아래) */}
      <div className="mt-5">
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
            className={`w-full rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors duration-200 disabled:cursor-not-allowed disabled:opacity-60 ${
              highlight
                ? "bg-primary text-primary-fg shadow-sm hover:bg-primary-hover"
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
              hasProrate ? "업그레이드" : tier.cta
            )}
          </button>
        )}
      </div>

      {/* 구분선 */}
      <div
        className={`mt-6 h-px ${
          highlight
            ? "bg-[linear-gradient(90deg,transparent,rgba(62,207,142,0.35)_50%,transparent)]"
            : "bg-[linear-gradient(90deg,transparent,var(--border)_50%,transparent)]"
        }`}
      />

      {/* features */}
      <ul className="mt-5 flex flex-col gap-2.5">
        {tier.features.map((f, i) => (
          <li
            key={i}
            className={`flex items-start gap-2.5 text-sm leading-snug ${
              f.muted ? "text-text-muted" : "text-text"
            }`}
          >
            <CheckSvg
              className={`mt-0.5 h-4 w-4 flex-shrink-0 ${
                f.muted ? "text-text-subtle" : "text-primary"
              }`}
            />
            <span>{f.text}</span>
          </li>
        ))}
      </ul>
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
          ? "bg-primary text-primary-fg shadow-[0_0_18px_-4px_rgba(62,207,142,0.5)]"
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
