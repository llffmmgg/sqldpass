"use client";

import {
  type ActiveSubscription,
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
  onPay: (plan: SubscriptionPlan) => void;
};

export default function CheckoutLanding({
  currentPlan,
  previews,
  payingPlan,
  subscription,
  onPay,
}: Props) {
  return (
    <div
      className="
        mx-auto w-full max-w-[1200px] px-6 pt-[72px] pb-20 sm:px-10 lg:px-20
        bg-[radial-gradient(ellipse_80%_50%_at_50%_-10%,rgba(245,181,68,0.08),transparent_60%)]
        dark:bg-[radial-gradient(ellipse_80%_50%_at_50%_-10%,rgba(245,181,68,0.06),transparent_60%)]
      "
    >
      <h1 className="mx-auto max-w-[820px] text-center text-[34px] font-extrabold leading-[1.2] tracking-[-0.027em] text-neutral-900 dark:text-neutral-100 sm:text-[44px]">
        내 시험 일정에 맞는<br />
        <span className="bg-gradient-to-b from-amber-400 to-amber-600 dark:from-amber-300 dark:to-amber-600 bg-clip-text text-transparent">
          플랜을 선택하세요
        </span>
      </h1>

      <p className="mx-auto mt-4 max-w-[560px] text-center text-sm leading-[1.7] text-neutral-600 dark:text-neutral-400">
        필요한 기간만큼 이용하고, 프리미엄 모의고사까지 바로 풀어보세요.
      </p>

      {subscription?.active && (
        <div className="mt-6 flex justify-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-emerald-500/40 bg-emerald-500/[0.08] px-4 py-1.5 text-xs font-medium text-emerald-700 dark:text-emerald-300">
            <CheckIcon className="h-3 w-3" />
            현재 {planLabel(subscription.plan!)} 이용 중
            {subscription.expiresAt && (
              <span className="opacity-80">
                · {new Date(subscription.expiresAt).toLocaleDateString("ko-KR")} 만료
              </span>
            )}
            {subscription.expiresAt === null && <span className="opacity-80">· 평생</span>}
          </span>
        </div>
      )}

      {/* secure rail */}
      <div className="mt-[22px] flex flex-wrap justify-center gap-x-[18px] gap-y-2">
        {["PortOne 안전 결제", "7일 환불 보장", "자동결제 없음"].map((txt) => (
          <span
            key={txt}
            className="inline-flex items-center gap-1.5 font-mono text-[11px] tracking-wide text-neutral-500 dark:text-neutral-400"
          >
            <CheckIcon className="h-2.5 w-2.5 text-amber-500" />
            {txt}
          </span>
        ))}
      </div>

      {/* 티어 4개 — 모바일 세로, 데스크톱 가로 */}
      <div className="mt-14 flex flex-col items-stretch gap-4 lg:flex-row">
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
      <div
        className="
          mt-14 grid grid-cols-1 gap-8 sm:grid-cols-3
          rounded-[14px] px-7 py-[26px]
          border border-neutral-200 dark:border-neutral-800
          bg-gradient-to-b from-white to-neutral-50
          dark:from-neutral-900 dark:to-neutral-950
        "
      >
        {[
          ["자동결제 되나요?", "아니요. 한 번 결제 = 한 번만 청구돼요."],
          ["환불은 언제까지?", "결제 후 7일 이내, 사용 이력이 없으면 100% 환불돼요."],
          ["3일권 만료되면?", "다시 결제하거나 한달권으로 이어갈 수 있어요."],
        ].map(([q, a], i) => (
          <div
            key={q}
            className={i === 0 ? "" : "sm:border-l sm:border-neutral-200 sm:dark:border-neutral-800 sm:pl-6"}
          >
            <div className="font-mono text-[10.5px] uppercase tracking-[1.4px] text-amber-700 dark:text-amber-600">
              FAQ · {String(i + 1).padStart(2, "0")}
            </div>
            <div className="mt-2 text-[13.5px] font-bold tracking-[-0.01em] text-neutral-900 dark:text-neutral-100">
              {q}
            </div>
            <div className="mt-1.5 text-[12.5px] leading-[1.65] text-neutral-600 dark:text-neutral-400">
              {a}
            </div>
          </div>
        ))}
      </div>

      <p className="mt-[22px] text-center font-mono text-[11px] tracking-wide text-neutral-500">
        결제 시 이용약관 및 환불정책에 동의하는 것으로 간주됩니다.
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
      className={`
        relative flex flex-1 flex-col rounded-[14px] px-6 pt-[26px] pb-6
        ${
          highlight
            ? `border border-amber-500/50 dark:border-amber-500/45
               bg-gradient-to-b from-amber-500/[0.08] to-amber-500/[0.02]
               shadow-[0_1px_0_0_rgba(255,255,255,0.5)_inset,0_24px_60px_rgba(245,181,68,0.18),0_2px_8px_rgba(0,0,0,0.06)]
               dark:shadow-[0_1px_0_0_rgba(255,255,255,0.05)_inset,0_24px_60px_rgba(245,181,68,0.10),0_2px_8px_rgba(0,0,0,0.4)]`
            : `border border-neutral-200 dark:border-neutral-800
               bg-gradient-to-b from-white to-neutral-50
               dark:from-neutral-900 dark:to-neutral-950
               shadow-[0_1px_0_0_rgba(255,255,255,0.6)_inset,0_8px_24px_rgba(0,0,0,0.06)]
               dark:shadow-[0_1px_0_0_rgba(255,255,255,0.03)_inset,0_8px_24px_rgba(0,0,0,0.3)]`
        }
      `}
    >
      {highlight && (
        <div
          className="
            absolute -top-2.5 left-[22px] rounded px-2.5 py-1
            font-mono text-[9.5px] font-extrabold uppercase tracking-[1.2px] text-amber-950
            bg-gradient-to-b from-amber-300 to-amber-400
            shadow-[0_4px_12px_rgba(245,181,68,0.45)]
          "
        >
          MOST POPULAR
        </div>
      )}

      <span
        className={`font-mono text-[10.5px] font-medium uppercase tracking-[1.4px] ${
          highlight ? "text-amber-700 dark:text-amber-600" : "text-neutral-500"
        }`}
      >
        {tier.unit ? tier.unit.toUpperCase() : "FREE"}
      </span>

      <div className="mt-2 text-[17px] font-bold tracking-[-0.018em] text-neutral-900 dark:text-neutral-100">
        {tier.name}
      </div>
      <div className="mt-1 min-h-[36px] whitespace-pre-line text-xs leading-[1.55] text-neutral-500">
        {tier.tagline}
      </div>

      {/* 가격 — prorate 분기 */}
      <div className="mt-[18px]">
        {hasProrate && preview ? (
          <>
            <div className="flex items-baseline gap-2">
              <span className="text-[30px] font-extrabold leading-none tracking-[-0.027em] text-neutral-900 dark:text-neutral-100 tabular-nums">
                ₩{preview.finalAmount.toLocaleString()}
              </span>
              <span className="text-xs text-neutral-400 line-through tabular-nums">
                ₩{preview.baseAmount.toLocaleString()}
              </span>
            </div>
            <p className="mt-1 text-[11px] text-emerald-600 dark:text-emerald-400">
              현재 구독 잔여 ₩{preview.prorateDiscount.toLocaleString()} 차감
            </p>
          </>
        ) : (
          <div className="flex items-baseline gap-1">
            <span className="text-[30px] font-extrabold leading-none tracking-[-0.027em] text-neutral-900 dark:text-neutral-100 tabular-nums">
              {tier.price === 0 ? "무료" : `₩${tier.price.toLocaleString()}`}
            </span>
            {tier.price > 0 && tier.unit && (
              <span className="text-xs text-neutral-500">/ {tier.unit}</span>
            )}
          </div>
        )}
      </div>

      <div
        className={`mt-5 h-px ${
          highlight
            ? "bg-[linear-gradient(90deg,transparent,rgba(245,181,68,0.3)_50%,transparent)]"
            : "bg-[linear-gradient(90deg,transparent,#e5e5e5_50%,transparent)] dark:bg-[linear-gradient(90deg,transparent,#2a2a2e_50%,transparent)]"
        }`}
      />

      <ul className="mt-[18px] flex flex-1 flex-col gap-2.5">
        {tier.features.map((f, i) => (
          <li
            key={i}
            className="flex items-start gap-2.5 text-[12.5px] leading-[1.55] text-neutral-800 dark:text-neutral-100"
          >
            <CheckIcon
              className={`mt-0.5 h-3.5 w-3.5 flex-shrink-0 ${
                highlight ? "text-amber-500 dark:text-amber-300" : "text-neutral-400"
              }`}
            />
            <span>{f}</span>
          </li>
        ))}
      </ul>

      {/* 버튼 — 5가지 상태 분기 */}
      <div className="mt-6">
        {isFree ? (
          <button
            type="button"
            disabled
            className="w-full cursor-default rounded-lg border border-dashed border-neutral-300 bg-transparent px-3.5 py-3 text-[12.5px] font-bold tracking-[-0.005em] text-neutral-500 dark:border-neutral-700"
          >
            현재 플랜
          </button>
        ) : isCurrent ? (
          <button
            type="button"
            disabled
            className="w-full cursor-default rounded-lg border border-emerald-500/40 bg-emerald-500/[0.08] px-3.5 py-3 text-[12.5px] font-bold text-emerald-700 dark:text-emerald-300"
          >
            ✓ 이용 중
          </button>
        ) : isBlocked ? (
          <div>
            <button
              type="button"
              disabled
              className="w-full cursor-default rounded-lg border border-neutral-300 bg-transparent px-3.5 py-3 text-[12.5px] font-medium text-neutral-500 dark:border-neutral-700"
            >
              현재 구독 종료 후 가능
            </button>
            {preview?.reason && (
              <p className="mt-2 text-[10.5px] leading-tight text-neutral-500">
                {preview.reason}
              </p>
            )}
          </div>
        ) : (
          <button
            type="button"
            onClick={() => onPay(tier.key as SubscriptionPlan)}
            disabled={disabled}
            className={`
              w-full rounded-lg px-3.5 py-3
              text-[12.5px] font-bold tracking-[-0.005em]
              transition disabled:cursor-not-allowed disabled:opacity-60
              ${
                highlight
                  ? `bg-gradient-to-b from-amber-300 to-amber-400 text-amber-950
                     shadow-[0_1px_0_rgba(255,255,255,0.4)_inset,0_-1px_0_rgba(0,0,0,0.1)_inset,0_8px_20px_rgba(245,181,68,0.32)]
                     dark:shadow-[0_1px_0_rgba(255,255,255,0.3)_inset,0_-1px_0_rgba(0,0,0,0.15)_inset,0_8px_20px_rgba(245,181,68,0.22)]
                     hover:brightness-105`
                  : `border border-neutral-300 dark:border-neutral-700 bg-transparent
                     text-neutral-900 dark:text-neutral-100
                     hover:bg-neutral-100 dark:hover:bg-neutral-900`
              }
            `}
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
              <span className="inline-flex items-center justify-center gap-2">
                <span className="rounded-[3px] bg-[#FEE500] px-1.5 py-0.5 text-[10px] font-extrabold leading-none text-[#3C1E1E]">
                  pay
                </span>
                {hasProrate ? "카카오페이로 업그레이드" : `카카오페이로 ${tier.cta}`}
              </span>
            )}
          </button>
        )}
      </div>
    </div>
  );
}

function CheckIcon({ className = "" }: { className?: string }) {
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
