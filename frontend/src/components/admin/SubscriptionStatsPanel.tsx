"use client";

import { useEffect, useId, useMemo, useState } from "react";
import {
  AnimatePresence,
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "framer-motion";

import {
  fetchRevenueByPlan,
  fetchRevenueByProvider,
  fetchRevenueByProviderAndPlan,
  fetchRevenueStats,
  type AdminSubscriptionPlan,
  type RevenueByPlan,
  type RevenuePoint,
} from "@/lib/adminApi";
import { PLAN_TOKENS, type SubscriptionPlanKey } from "@/lib/plan-tokens";

const RANGES = [7, 30, 90] as const;
type Range = (typeof RANGES)[number];

const CHANNEL_OPTIONS = [
  { id: "ALL", label: "전체" },
  { id: "PORTONE", label: "PortOne" },
  { id: "PLAY_BILLING", label: "Play" },
  { id: "APP_STORE", label: "App Store" },
] as const;
type ChannelId = (typeof CHANNEL_OPTIONS)[number]["id"];

const SPRING_FAST = { stiffness: 220, damping: 28, mass: 0.6 };
const PATH_TWEEN = { duration: 0.35, ease: [0.4, 0, 0.2, 1] as const };
const FADE_TWEEN = { duration: 0.25, ease: [0.4, 0, 0.2, 1] as const };

function AnimatedKRW({ value }: { value: number }) {
  const mv = useMotionValue(value);
  const spring = useSpring(mv, SPRING_FAST);
  const rounded = useTransform(spring, (v) => `₩${Math.round(v).toLocaleString()}`);
  useEffect(() => {
    mv.set(value);
  }, [value, mv]);
  return <motion.span>{rounded}</motion.span>;
}

function AnimatedCount({ value }: { value: number }) {
  const mv = useMotionValue(value);
  const spring = useSpring(mv, SPRING_FAST);
  const rounded = useTransform(spring, (v) => `${Math.round(v).toLocaleString()}건`);
  useEffect(() => {
    mv.set(value);
  }, [value, mv]);
  return <motion.span>{rounded}</motion.span>;
}

export default function SubscriptionStatsPanel() {
  const [days, setDays] = useState<Range>(30);
  const [channel, setChannel] = useState<ChannelId>("ALL");
  const [points, setPoints] = useState<RevenuePoint[]>([]);
  const [byPlan, setByPlan] = useState<RevenueByPlan[]>([]);
  const [pending, setPending] = useState(true);
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setPending(true);

    const load = async () => {
      try {
        if (channel === "ALL") {
          const [rev, plan] = await Promise.all([
            fetchRevenueStats(days),
            fetchRevenueByPlan(days),
          ]);
          if (cancelled) return;
          setPoints(rev);
          setByPlan(plan);
        } else {
          const [byProv, byProvPlan] = await Promise.all([
            fetchRevenueByProvider(days),
            fetchRevenueByProviderAndPlan(days),
          ]);
          if (cancelled) return;
          // 한 provider 당 같은 날짜에 한 row 라 별도 합산 불필요.
          setPoints(
            byProv
              .filter((p) => p.provider === channel)
              .map((p) => ({
                date: p.date,
                revenue: p.revenue,
                refundAmount: p.refundAmount,
                count: p.count,
              })),
          );
          setByPlan(
            byProvPlan
              .filter((p) => p.provider === channel)
              .map((p) => ({
                plan: p.plan as AdminSubscriptionPlan,
                count: p.count,
                revenue: p.revenue,
              })),
          );
        }
      } catch {
        if (cancelled) return;
        setPoints([]);
        setByPlan([]);
      } finally {
        if (cancelled) return;
        setPending(false);
        setHasLoaded(true);
      }
    };

    load();

    return () => {
      cancelled = true;
    };
  }, [days, channel]);

  const totalRevenue = points.reduce((s, p) => s + p.revenue, 0);
  const totalRefund = points.reduce((s, p) => s + p.refundAmount, 0);
  const totalCount = points.reduce((s, p) => s + p.count, 0);

  const initialLoad = pending && !hasLoaded;

  return (
    <section className="mb-6 rounded-xl border border-border bg-surface p-5" aria-busy={pending}>
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-text">매출 통계</h2>
          <p className="mt-0.5 text-[11px] text-text-muted">archived 구독은 집계에서 제외</p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* 채널 segmented control */}
          <div
            role="group"
            aria-label="채널 선택"
            className="relative inline-flex items-center rounded-sm border border-border bg-bg-elevated p-0.5 text-xs"
          >
            {CHANNEL_OPTIONS.map((opt) => {
              const isActive = channel === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  aria-pressed={isActive}
                  aria-label={`채널 ${opt.label}`}
                  disabled={initialLoad}
                  onClick={() => setChannel(opt.id)}
                  className={`relative z-10 rounded-sm px-2.5 py-1 font-medium transition-colors duration-200 ${
                    isActive ? "text-text" : "text-text-subtle hover:text-text"
                  } ${initialLoad ? "cursor-wait opacity-70" : ""}`}
                >
                  {isActive && (
                    <motion.span
                      layoutId="admin-revenue-channel-indicator"
                      className="absolute inset-0 -z-10 rounded-sm bg-surface shadow-sm"
                      transition={{ type: "spring", stiffness: 380, damping: 32 }}
                      aria-hidden
                    />
                  )}
                  <span className="relative">{opt.label}</span>
                </button>
              );
            })}
          </div>

          {/* segmented control — 대시보드 패턴 */}
          <div
            role="group"
            aria-label="기간 선택"
            className="relative inline-flex items-center rounded-sm border border-border bg-bg-elevated p-0.5 text-xs"
          >
            {RANGES.map((r) => {
              const isActive = days === r;
              return (
                <button
                  key={r}
                  type="button"
                  aria-pressed={isActive}
                  aria-label={`최근 ${r}일`}
                  disabled={initialLoad}
                  onClick={() => setDays(r)}
                  className={`relative z-10 rounded-sm px-2.5 py-1 font-medium transition-colors duration-200 ${
                    isActive ? "text-text" : "text-text-subtle hover:text-text"
                  } ${initialLoad ? "cursor-wait opacity-70" : ""}`}
                >
                  {isActive && (
                    <motion.span
                      layoutId="admin-revenue-active-indicator"
                      className="absolute inset-0 -z-10 rounded-sm bg-surface shadow-sm"
                      transition={{ type: "spring", stiffness: 380, damping: 32 }}
                      aria-hidden
                    />
                  )}
                  <span className="relative">{r}일</span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <StatCard label="총 매출" value={<AnimatedKRW value={totalRevenue} />} />
        <StatCard
          label="환불액"
          value={<AnimatedKRW value={totalRefund} />}
          tone="danger"
        />
        <StatCard label="결제 건수" value={<AnimatedCount value={totalCount} />} />
      </div>

      <div className="mt-5">
        <RevenueLineChart points={points} initialLoad={initialLoad} />
      </div>

      <div className="mt-5">
        <PlanBarChart data={byPlan} initialLoad={initialLoad} />
      </div>
    </section>
  );
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: React.ReactNode;
  tone?: "danger";
}) {
  return (
    <div className="rounded-lg border border-border bg-bg-elevated px-4 py-3">
      <p className="text-[11px] font-medium text-text-muted">{label}</p>
      <p
        className={`mt-1 text-xl font-bold tabular-nums ${
          tone === "danger" ? "text-danger" : "text-text"
        }`}
      >
        {value}
      </p>
    </div>
  );
}

// ── 매출/환불 라인 차트 — 대시보드 StudyActivityChart 패턴 ───────
const VB_W = 600;
const VB_H = 160;
const PADDING_LEFT = 12;
const PADDING_RIGHT = 56; // ₩ 큰 숫자 라벨 공간
const PADDING_TOP = 16;
const PADDING_BOTTOM = 28;
const PLOT_W = VB_W - PADDING_LEFT - PADDING_RIGHT;
const PLOT_H = VB_H - PADDING_TOP - PADDING_BOTTOM;

function niceCeil(v: number): number {
  if (v <= 0) return 100;
  const pow = Math.pow(10, Math.floor(Math.log10(v)));
  const n = v / pow;
  let step;
  if (n <= 1) step = 1;
  else if (n <= 2) step = 2;
  else if (n <= 5) step = 5;
  else step = 10;
  return step * pow;
}

function buildLinePath(pts: { x: number; y: number }[]): string {
  if (pts.length === 0) return "";
  if (pts.length === 1) return `M ${pts[0].x} ${pts[0].y}`;
  let d = `M ${pts[0].x} ${pts[0].y}`;
  for (let i = 1; i < pts.length; i++) d += ` L ${pts[i].x} ${pts[i].y}`;
  return d;
}

function buildAreaPath(pts: { x: number; y: number }[], baseY: number): string {
  const line = buildLinePath(pts);
  if (!line) return "";
  const last = pts[pts.length - 1];
  const first = pts[0];
  return `${line} L ${last.x} ${baseY} L ${first.x} ${baseY} Z`;
}

function formatKrwTick(v: number): string {
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(v % 1_000_000 === 0 ? 0 : 1)}M`;
  if (v >= 1000) return `${(v / 1000).toFixed(v % 1000 === 0 ? 0 : 1)}k`;
  return v.toLocaleString();
}

function RevenueLineChart({
  points,
  initialLoad,
}: {
  points: RevenuePoint[];
  initialLoad: boolean;
}) {
  const gradId = useId();

  const maxRevenue = Math.max(...points.map((p) => p.revenue), 0);
  const maxRefund = Math.max(...points.map((p) => p.refundAmount), 0);
  const rawMax = Math.max(maxRevenue, maxRefund, 1);
  const yMax = niceCeil(rawMax);

  const colW = points.length > 0 ? PLOT_W / points.length : PLOT_W;
  const baseY = PADDING_TOP + PLOT_H;

  const yTicks = useMemo(
    () =>
      [0, yMax / 2, yMax].map((v) => ({
        value: Math.round(v),
        y: PADDING_TOP + (1 - v / yMax) * PLOT_H,
      })),
    [yMax],
  );

  const labelInterval = points.length <= 7 ? 1 : points.length <= 14 ? 2 : 5;
  const visibleLabels = useMemo(
    () =>
      points
        .map((p, i) => ({ p, i }))
        .filter(({ i }) => i % labelInterval === 0 || i === points.length - 1),
    [points, labelInterval],
  );

  const revenuePts = useMemo(
    () =>
      points.map((p, i) => ({
        x: PADDING_LEFT + i * colW + colW / 2,
        y: baseY - (p.revenue / yMax) * PLOT_H,
      })),
    [points, colW, baseY, yMax],
  );
  const refundPts = useMemo(
    () =>
      points.map((p, i) => ({
        x: PADDING_LEFT + i * colW + colW / 2,
        y: baseY - (p.refundAmount / yMax) * PLOT_H,
      })),
    [points, colW, baseY, yMax],
  );

  const revenueArea = buildAreaPath(revenuePts, baseY);
  const revenueLine = buildLinePath(revenuePts);
  const refundLine = buildLinePath(refundPts);

  if (initialLoad) {
    return <div className="h-40 rounded-md border border-border bg-bg-elevated" />;
  }
  if (points.length === 0) {
    return (
      <div className="flex h-40 items-center justify-center rounded-md border border-border bg-bg-elevated text-xs text-text-muted">
        해당 기간 결제가 없습니다
      </div>
    );
  }

  return (
    <div className="rounded-md border border-border bg-bg-elevated p-3">
      <div className="mb-2 flex items-center gap-4 text-[11px] text-text-muted">
        <span className="flex items-center gap-1.5">
          <span className="h-1.5 w-3 rounded-full bg-primary" /> 매출
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-1.5 w-3 rounded-full bg-danger" /> 환불
        </span>
      </div>

      <svg
        viewBox={`0 0 ${VB_W} ${VB_H}`}
        className="block w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label="일별 매출/환불 추이"
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--primary)" stopOpacity={0.28} />
            <stop offset="100%" stopColor="var(--primary)" stopOpacity={0} />
          </linearGradient>
        </defs>

        {/* y grid + label (우측 정렬) */}
        <AnimatePresence mode="popLayout">
          {yTicks.map((t) => (
            <motion.g
              key={`ytick-${t.value}`}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={FADE_TWEEN}
            >
              <motion.line
                x1={PADDING_LEFT}
                x2={PADDING_LEFT + PLOT_W}
                animate={{ y1: t.y, y2: t.y }}
                initial={false}
                transition={PATH_TWEEN}
                stroke="var(--border-strong)"
                strokeWidth={1}
                strokeDasharray={t.value === 0 ? "0" : "3 3"}
                opacity={t.value === 0 ? 0.9 : 0.75}
              />
              <motion.text
                x={PADDING_LEFT + PLOT_W + 6}
                animate={{ y: t.y + 3 }}
                initial={false}
                transition={PATH_TWEEN}
                textAnchor="start"
                fill="var(--text-subtle)"
                fontSize="10"
              >
                {formatKrwTick(t.value)}
              </motion.text>
            </motion.g>
          ))}
        </AnimatePresence>

        {/* 매출 area + line */}
        <motion.path
          animate={{ d: revenueArea }}
          initial={false}
          transition={PATH_TWEEN}
          fill={`url(#${gradId})`}
        />
        <motion.path
          animate={{ d: revenueLine }}
          initial={false}
          transition={PATH_TWEEN}
          fill="none"
          stroke="var(--primary)"
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* 환불 line — area 없이 line 만, 강조 약 */}
        <motion.path
          animate={{ d: refundLine }}
          initial={false}
          transition={PATH_TWEEN}
          fill="none"
          stroke="var(--danger)"
          strokeWidth={1.5}
          strokeDasharray="4 3"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* x 라벨 */}
        <AnimatePresence mode="popLayout">
          {visibleLabels.map(({ p, i }) => {
            const cx = PADDING_LEFT + i * colW + colW / 2;
            return (
              <motion.text
                key={p.date}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1, x: cx }}
                exit={{ opacity: 0 }}
                transition={FADE_TWEEN}
                y={PADDING_TOP + PLOT_H + 16}
                textAnchor="middle"
                fill="var(--text-subtle)"
                fontSize="10"
              >
                {p.date.slice(5)}
              </motion.text>
            );
          })}
        </AnimatePresence>
      </svg>
    </div>
  );
}

// ── 플랜별 분포 — 카테고리 막대 (선형 차트 부적합, 막대 유지) ──
function PlanBarChart({
  data,
  initialLoad,
}: {
  data: RevenueByPlan[];
  initialLoad: boolean;
}) {
  if (initialLoad) {
    return <div className="h-24 rounded-md border border-border bg-bg-elevated" />;
  }
  if (data.length === 0) {
    return (
      <div className="flex h-24 items-center justify-center rounded-md border border-border bg-bg-elevated text-xs text-text-muted">
        플랜별 데이터가 없습니다
      </div>
    );
  }

  const maxRevenue = Math.max(...data.map((d) => d.revenue), 1);

  return (
    <div className="rounded-md border border-border bg-bg-elevated p-3">
      <p className="mb-2 text-[11px] font-medium text-text-muted">플랜별 매출 분포</p>
      <ul className="space-y-2">
        {data.map((d) => {
          const token = PLAN_TOKENS[d.plan as SubscriptionPlanKey];
          const widthPct = (d.revenue / maxRevenue) * 100;
          return (
            <li key={d.plan} className="flex items-center gap-3">
              <span className={`shrink-0 text-[11px] font-semibold ${token?.text ?? "text-text"}`}>
                {token?.label ?? d.plan}
              </span>
              <div className="relative h-3 flex-1 overflow-hidden rounded-sm bg-surface-hover">
                <motion.div
                  className={`h-full ${token?.bar ?? "bg-primary"}`}
                  initial={false}
                  animate={{ width: `${widthPct}%` }}
                  transition={{ type: "spring", stiffness: 180, damping: 28 }}
                />
              </div>
              <span className="shrink-0 text-[11px] tabular-nums text-text-muted">
                {d.count}건 · ₩{d.revenue.toLocaleString()}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
