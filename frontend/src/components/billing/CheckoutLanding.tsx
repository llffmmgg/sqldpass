"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
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
  /** 가격 바로 아래 한 줄 가치 제안. 각 플랜의 핵심 사용 시나리오를 짧게 어필. */
  pitch: string;
  features: Feature[];
  cta: string;
  highlight?: boolean;
};

const FREE_BASELINE: Feature[] = [
  { text: "쉬움/보통 회차", muted: true },
  { text: "대시보드", muted: true },
];

const TIERS: Tier[] = [
  {
    key: "FREE",
    name: "Free",
    tagline: "기본 문제 풀이 제공",
    price: 0,
    pitch: "문제 30개와 모의고사 1회로 매일 가볍게 감을 잡아보세요.",
    features: [
      { text: "문제 30개/일" },
      { text: "모의고사 1회/일" },
      { text: "쉬움/보통 회차" },
      { text: "즐겨찾기 30개" },
      { text: "대시보드" },
    ],
    cta: "현재 플랜",
  },
  {
    key: "FOCUS",
    name: "Focus",
    tagline: "일상 학습 30일 집중",
    price: 2900,
    unit: "30일",
    pitch: "문제풀이와 모의고사를 무제한으로 풀고 광고 없이 한 달 집중하세요.",
    features: [
      { text: "문제풀이 무제한" },
      { text: "모의고사 무제한" },
      { text: "광고 제거" },
      { text: "오답노트 사용" },
      { text: "즐겨찾기 무제한" },
      ...FREE_BASELINE,
    ],
    cta: "Focus 시작",
  },
  {
    key: "THREE_DAY",
    name: "Thunder",
    tagline: "벼락치기 3일 풀파워",
    price: 3900,
    unit: "3일",
    pitch: "시험 임박 3일, PASS+ 모의고사까지 벼락치기로 마무리하세요.",
    features: [
      { text: "문제풀이 무제한" },
      { text: "모의고사 무제한" },
      { text: "PASS+ 모의고사" },
      { text: "72시간 풀 액세스" },
      { text: "광고 제거" },
      { text: "오답노트 사용" },
      { text: "즐겨찾기 무제한" },
      ...FREE_BASELINE,
    ],
    cta: "Thunder 시작",
  },
  {
    key: "ONE_MONTH",
    name: "Pro",
    tagline: "한 달 집중 합격 코스",
    price: 9900,
    originalPrice: 12900,
    unit: "30일",
    pitch: "PASS+ 모의고사까지 한 달 동안 무제한으로 풀어 합격을 굳히세요.",
    features: [
      { text: "문제풀이 무제한" },
      { text: "모의고사 무제한" },
      { text: "PASS+ 모의고사" },
      { text: "30일 풀 액세스" },
      { text: "광고 제거" },
      { text: "오답노트 사용" },
      { text: "즐겨찾기 무제한" },
      ...FREE_BASELINE,
    ],
    cta: "Pro 시작",
    highlight: true,
  },
  {
    key: "UNLIMITED",
    name: "All Pass",
    tagline: "6개월 풀 액세스",
    price: 29900,
    originalPrice: 39900,
    unit: "180일",
    pitch: "PASS+ 모의고사와 PDF 다운로드까지 6개월 무제한으로 사용하세요.",
    features: [
      { text: "문제풀이 무제한" },
      { text: "모의고사 무제한" },
      { text: "PASS+ 모의고사" },
      { text: "6개월 풀 액세스" },
      { text: "광고 제거" },
      { text: "오답노트 사용" },
      { text: "즐겨찾기 무제한" },
      { text: "PDF 다운로드" },
      ...FREE_BASELINE,
    ],
    cta: "All Pass 시작",
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
      {/* 우측 상단 — 문어 마스코트가 피켓 들고 흔드는 한정 할인 안내 */}
      <div className="pointer-events-none absolute -right-2 -top-6 z-10 hidden sm:block">
        <PromoPicket />
      </div>
      {/* 모바일 — 헤더 위에 동일 피켓 인라인 표시 */}
      <div className="mb-2 flex justify-center sm:hidden">
        <PromoPicket />
      </div>

      <header className="text-center">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-fg">
          요금제
        </span>
        <h1 className="mx-auto mt-5 max-w-2xl text-balance text-4xl font-bold tracking-tight text-text sm:text-5xl">
          내 시험 일정에 맞는
          <br />
          <span className="text-primary">플랜을 선택하세요</span>
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
                <span className="ml-1 opacity-80">· 무기한</span>
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

      {/* 모바일 (< md): 가로 snap-scroll 캐러셀 */}
      <MobilePlanCarousel
        tiers={TIERS}
        currentPlan={currentPlan}
        previews={previews}
        payingPlan={payingPlan}
        onPay={onPay}
      />

      {/* 데스크탑 (md+): 기존 2/5 열 그리드 */}
      <div className="mt-10 hidden gap-6 md:grid md:grid-cols-2 xl:grid-cols-5">
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
          최근 자격증 시험은 점점 어려워지고 있습니다
        </h2>
        <p className="mx-auto mt-4 max-w-xl text-sm leading-relaxed text-text-muted sm:text-base">
          컴퓨터활용능력 1급 실기는 2024년 개정 이후 합격률이{" "}
          <span className="font-semibold text-text">6.9%</span>까지 떨어졌고,
          정보처리기사 실기도 2025년 1회 합격률이{" "}
          <span className="font-semibold text-text">15.1%</span>에 그쳤습니다.
        </p>
        <p className="mx-auto mt-4 max-w-xl text-sm leading-relaxed text-text-muted sm:text-base">
          기출을 반복해서 풀었는데도 막상 시험장에서 낯선 문제가 나오면 당황할 수
          있습니다. 그래서 시험 전에는 기출 복원 문제뿐 아니라, 신유형과 변형 문제를
          반영한 추가 회차까지 풀어보는 것이 좋습니다.
        </p>
        <p className="mx-auto mt-4 max-w-xl text-sm leading-relaxed text-text sm:text-base">
          <span className="font-semibold text-primary">PASS+</span>로 시험 전에 한 번 더
          점검해보세요. 익숙한 문제만 푸는 연습에서 끝내지 말고, 실제 시험에서 마주칠 수
          있는 낯선 문제까지 미리 대비할 수 있습니다.
        </p>
        <p className="mx-auto mt-5 text-[11px] text-text-subtle">
          자료: 큐넷·대한상공회의소 공개 자격 통계
        </p>
      </div>

      {/* FAQ */}
      <div className="mt-16 grid grid-cols-1 gap-6 rounded-2xl border border-border bg-surface/40 p-6 sm:grid-cols-3 sm:gap-8 sm:p-8">
        {[
          ["자동결제 되나요?", "아니요. 한 번 결제 = 한 번만 청구돼요."],
          ["환불은 언제까지?", "결제 후 7일 이내, 사용 이력이 없으면 100% 환불돼요."],
          ["Thunder 만료되면?", "다시 결제하거나 Focus·Pro 로 이어갈 수 있어요."],
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
      className={`group relative flex h-full flex-col rounded-2xl p-5 sm:p-6 transition-all duration-300 ease-out will-change-transform ${
        highlight
          ? "border-2 border-primary/60 bg-gradient-to-b from-primary/[0.06] via-surface to-surface/80 shadow-[0_0_24px_-12px_rgba(62,207,142,0.30)] hover:-translate-y-1 hover:border-primary/80 hover:shadow-[0_0_40px_-8px_rgba(62,207,142,0.45)]"
          : "border border-border bg-surface/40 hover:-translate-y-1 hover:border-primary/40 hover:bg-surface/70 hover:shadow-[0_0_28px_-10px_rgba(62,207,142,0.18)]"
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

      {/* 가격 — prorate 분기. 가격 영역 높이를 고정해 모든 카드의 버튼 정렬을 맞춤.
       * prorate 상태는 3줄(finalAmount / line-through baseAmount / 잔여 차감) 표시되므로
       * min-h 96px 로 비활성 카드들도 같은 높이 확보. */}
      <div className="mt-5 flex min-h-[96px] flex-col justify-start">
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
            <p className="mt-1.5 text-xs tabular-nums text-text-subtle line-through">
              ₩{preview.baseAmount.toLocaleString()}
            </p>
            <p className="mt-1 text-xs text-success">
              현재 구독 잔여 ₩{preview.prorateDiscount.toLocaleString()} 차감
            </p>
          </>
        ) : (
          <>
            <div className="flex items-baseline gap-1.5">
              <span className="text-3xl font-bold tabular-nums tracking-tight text-text">
                {tier.price === 0 ? "무료" : `₩${tier.price.toLocaleString()}`}
              </span>
              {tier.price > 0 && tier.unit && (
                <span className="text-sm text-text-subtle">/ {tier.unit}</span>
              )}
            </div>
            {tier.originalPrice && tier.originalPrice > tier.price ? (
              <p className="mt-1.5 text-xs tabular-nums text-text-subtle">
                정가 <span className="line-through">₩{tier.originalPrice.toLocaleString()}</span>
              </p>
            ) : (
              <p className="mt-1.5 text-xs select-none" aria-hidden>
                &nbsp;
              </p>
            )}
          </>
        )}
      </div>

      {/* 가치 제안 — 가격 아래, CTA 위. 카드 폭에서 한국어가 2~4줄 wrap 되므로 min-h 충분히 잡아 CTA 라인 정렬 */}
      <p
        className={`mt-3 min-h-[88px] text-[13px] leading-relaxed ${
          highlight ? "text-text" : "text-text-muted"
        }`}
      >
        {tier.pitch}
      </p>

      {/* CTA 버튼 — 5가지 상태 분기 (가격 바로 아래, 모든 카드 동일 위치) */}
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

      {/* 구분선 — CTA 와 features 사이 자연스러운 분리 */}
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

// 모바일(< md) 전용 — 5장 카드를 가로로 snap-scroll. 초기 위치는 highlight 플랜(Pro).
// 데스크탑은 hidden md:grid 분기로 별도 그리드 사용.
function MobilePlanCarousel({
  tiers,
  currentPlan,
  previews,
  payingPlan,
  onPay,
}: {
  tiers: Tier[];
  currentPlan: SubscriptionPlan | null;
  previews: Record<SubscriptionPlan, PreviewResponse | null>;
  payingPlan: SubscriptionPlan | null;
  onPay: (plan: SubscriptionPlan) => void;
}) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const itemRefs = useRef<Array<HTMLDivElement | null>>([]);
  // highlight 플랜(Pro)을 기본 활성 dot 으로. tiers 는 정적 상수라 lazy 평가로 충분.
  const [activeIndex, setActiveIndex] = useState(() => {
    const i = tiers.findIndex((t) => t.highlight);
    return i > 0 ? i : 0;
  });

  // 초기 스크롤 — highlight 카드로 즉시 점프(애니메이션 없이). first paint 직후라 jump 자연스러움.
  useEffect(() => {
    const initial = tiers.findIndex((t) => t.highlight);
    if (initial > 0) {
      itemRefs.current[initial]?.scrollIntoView({ inline: "center", block: "nearest" });
    }
  }, [tiers]);

  // 중앙에 가장 많이 보이는 카드 추적 — dot 동기화용. snap 후 안착된 카드를 잡음.
  useEffect(() => {
    const root = scrollRef.current;
    if (!root) return;
    const io = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
        if (visible) {
          const idx = itemRefs.current.findIndex((el) => el === visible.target);
          if (idx >= 0) setActiveIndex(idx);
        }
      },
      { root, threshold: [0.5, 0.75, 1] },
    );
    itemRefs.current.forEach((el) => el && io.observe(el));
    return () => io.disconnect();
  }, [tiers]);

  function scrollToIndex(i: number) {
    itemRefs.current[i]?.scrollIntoView({
      inline: "center",
      block: "nearest",
      behavior: "smooth",
    });
  }

  return (
    <div className="mt-10 md:hidden">
      <div
        ref={scrollRef}
        className="-mx-4 flex snap-x snap-mandatory gap-4 overflow-x-auto px-4 pb-2 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {tiers.map((t, i) => (
          <div
            key={t.key}
            ref={(el) => {
              itemRefs.current[i] = el;
            }}
            className="w-[85%] flex-shrink-0 snap-center first:ml-2 last:mr-2"
          >
            <PlanCard
              tier={t}
              currentPlan={currentPlan}
              preview={t.key === "FREE" ? null : previews[t.key]}
              payingPlan={payingPlan}
              onPay={onPay}
            />
          </div>
        ))}
      </div>

      <div
        className="mt-4 flex justify-center gap-1.5"
        role="tablist"
        aria-label="플랜 인디케이터"
      >
        {tiers.map((t, i) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={activeIndex === i}
            aria-label={`${t.name} 보기`}
            onClick={() => scrollToIndex(i)}
            className={`h-1.5 rounded-full transition-all ${
              activeIndex === i
                ? "w-6 bg-primary"
                : "w-1.5 bg-border hover:bg-border-strong"
            }`}
          />
        ))}
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

// 문어 마스코트가 "할인가 5월 30일까지" 사인을 들고 점프하는 피켓.
// 글자는 이미지에 베이크 인됨 (CSS 오버레이 불필요). Duo 스타일 호흡+점프 모션.
function PromoPicket() {
  return (
    <div className="inline-block w-[200px] select-none sm:w-[220px]">
      <div className="animate-octopus-hop">
        <Image
          src="/promo/octopus-picket.webp"
          alt="할인가 5월 30일까지"
          width={577}
          height={433}
          className="h-auto w-full"
          priority={false}
        />
      </div>
    </div>
  );
}

export function planLabel(plan: SubscriptionPlan): string {
  switch (plan) {
    case "THREE_DAY":
      return "Thunder";
    case "FOCUS":
      return "Focus";
    case "ONE_MONTH":
      return "Pro";
    case "UNLIMITED":
      return "All Pass";
  }
}
