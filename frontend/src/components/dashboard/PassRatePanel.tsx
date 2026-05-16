"use client";

import Link from "next/link";
import { motion, useMotionValue, useSpring, useTransform } from "framer-motion";
import { useEffect } from "react";
import type { CertActivity } from "@/lib/dashboard/activeCerts";
import { computePassRate, BAND_META } from "@/lib/dashboard/passRate";
import { CERT_TOKENS } from "@/lib/cert-tokens";

type Props = {
  /** 풀이 ≥5 건. 합격률 % 표시. */
  activeCerts: CertActivity[];
  /** 풀이 1-4 건. 합격률 추정 불가 — CTA cell 표시. */
  inactiveCerts: CertActivity[];
};

const SPRING_PCT = { stiffness: 180, damping: 28, mass: 0.6 };

function AnimatedPct({ value }: { value: number }) {
  const mv = useMotionValue(value);
  const spring = useSpring(mv, SPRING_PCT);
  const rounded = useTransform(spring, (v) => `${Math.round(v)}`);
  useEffect(() => {
    mv.set(value);
  }, [value, mv]);
  return <motion.span>{rounded}</motion.span>;
}

export default function PassRatePanel({ activeCerts, inactiveCerts }: Props) {
  const hasAny = activeCerts.length + inactiveCerts.length > 0;
  if (!hasAny) {
    // 풀이 0건 — 전체 empty state
    return (
      <section className="overflow-hidden rounded-xl border border-border bg-surface p-6">
        <h3 className="text-sm font-semibold">예상 합격률</h3>
        <p className="mt-2 text-sm text-text-muted">
          모의고사를 한 회차 풀면 자격증별 합격률을 추정해드릴게요.
        </p>
        <Link
          href="/mock-exams"
          className="mt-4 inline-flex items-center rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-fg transition-colors hover:bg-primary-hover"
        >
          모의고사 풀기
        </Link>
      </section>
    );
  }

  return (
    <section className="overflow-hidden rounded-xl border border-border bg-surface">
      <header className="flex items-center justify-between gap-2 border-b border-border px-5 py-3">
        <h3 className="text-sm font-semibold">예상 합격률</h3>
        <span className="text-xs text-text-muted">
          최근 5회 평균 · 풀이수 30+ 면 정밀도 최대
        </span>
      </header>
      {/* half-width 2-col 컨테이너 안에 들어가므로 cell 은 1-2 cols 로 유지 */}
      <div className="grid grid-cols-1 divide-y divide-border sm:grid-cols-2 sm:divide-x sm:divide-y-0">
        {activeCerts.map((a) => {
          const rate = computePassRate(a);
          const token = CERT_TOKENS[a.cert];
          const meta = BAND_META[rate.band];
          return (
            <div key={a.cert} className="px-5 py-4">
              <div className="flex items-center gap-1.5">
                <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} aria-hidden />
                <span className="truncate text-xs font-medium text-text-muted">
                  {token.label}
                </span>
              </div>
              <p className={`mt-1.5 text-2xl font-bold tabular-nums ${meta.textClass}`}>
                <AnimatedPct value={rate.probPct} />
                <span className="ml-0.5 text-xs font-normal text-text-subtle">%</span>
              </p>
              <div className="mt-2 h-1.5 overflow-hidden rounded-sm bg-bg-elevated">
                <motion.div
                  className="h-full"
                  initial={false}
                  animate={{ width: `${rate.probPct}%` }}
                  transition={{ type: "spring", stiffness: 180, damping: 28 }}
                  style={{ backgroundColor: meta.barColor }}
                />
              </div>
              <p className="mt-1 flex items-center justify-between text-[10px] text-text-subtle tabular-nums">
                <span>{meta.label}</span>
                <span>
                  합격선 {rate.margin >= 0 ? "+" : ""}
                  {rate.margin}
                </span>
              </p>
            </div>
          );
        })}
        {inactiveCerts.map((a) => {
          const token = CERT_TOKENS[a.cert];
          return (
            <Link
              key={a.cert}
              href={`/mock-exams?cert=${a.cert}`}
              className="block px-5 py-4 transition-colors hover:bg-bg-elevated"
            >
              <div className="flex items-center gap-1.5">
                <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} aria-hidden />
                <span className="truncate text-xs font-medium text-text-muted">
                  {token.label}
                </span>
              </div>
              <p className="mt-1.5 text-sm font-medium text-text-subtle">추정 불가</p>
              <p className="mt-1 text-[11px] leading-relaxed text-text-muted">
                풀이 {a.solveCount}건 · 모의고사 1회 풀면 정확해져요
              </p>
              <p className="mt-2 text-[11px] font-medium text-primary">
                모의고사 풀기 →
              </p>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
