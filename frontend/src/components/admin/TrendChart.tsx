"use client";

/* eslint-disable react-hooks/set-state-in-effect -- fetch 시작 시 pending sync setState */

import { useEffect, useId, useMemo, useState } from "react";
import {
  AnimatePresence,
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "framer-motion";
import { getTrend, type AdminTrendPoint } from "@/lib/adminApi";

const PERIOD_OPTIONS = [7, 14, 30] as const;
type Period = (typeof PERIOD_OPTIONS)[number];
type Field = "newMembers" | "newSolves";

const SPRING_FAST = { stiffness: 220, damping: 28, mass: 0.6 };
const PATH_TWEEN = { duration: 0.35, ease: [0.4, 0, 0.2, 1] as const };
const FADE_TWEEN = { duration: 0.25, ease: [0.4, 0, 0.2, 1] as const };

function AnimatedInt({ value, className }: { value: number; className?: string }) {
  const mv = useMotionValue(value);
  const spring = useSpring(mv, SPRING_FAST);
  const rounded = useTransform(spring, (v) => Math.round(v).toLocaleString());
  useEffect(() => {
    mv.set(value);
  }, [value, mv]);
  return <motion.span className={className}>{rounded}</motion.span>;
}

export default function TrendChart() {
  const [days, setDays] = useState<Period>(7);
  const [points, setPoints] = useState<AdminTrendPoint[] | null>(null);
  const [pending, setPending] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    setPending(true);
    setError(null);
    getTrend(days)
      .then((r) => {
        if (!alive) return;
        setPoints(r.points);
        setPending(false);
      })
      .catch((e) => {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "불러오기 실패");
        setPending(false);
      });
    return () => {
      alive = false;
    };
  }, [days]);

  const initialLoad = pending && !points;

  return (
    <div className="rounded-xl border border-border bg-surface p-5" aria-busy={pending}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <h2 className="text-sm font-semibold">회원/풀이 추이</h2>

        {/* segmented control — 대시보드 패턴: framer-motion layoutId 슬라이딩 indicator */}
        <div
          role="group"
          aria-label="기간 선택"
          className="relative inline-flex items-center rounded-sm border border-border bg-bg-elevated p-0.5 text-xs"
        >
          {PERIOD_OPTIONS.map((p) => {
            const isActive = days === p;
            return (
              <button
                key={p}
                type="button"
                aria-pressed={isActive}
                aria-label={`최근 ${p}일`}
                disabled={initialLoad}
                onClick={() => setDays(p)}
                className={`relative z-10 rounded-sm px-2.5 py-1 font-medium transition-colors duration-200 ${
                  isActive ? "text-text" : "text-text-subtle hover:text-text"
                } ${initialLoad ? "cursor-wait opacity-70" : ""}`}
              >
                {isActive && (
                  <motion.span
                    layoutId="admin-trend-active-indicator"
                    className="absolute inset-0 -z-10 rounded-sm bg-surface shadow-sm"
                    transition={{ type: "spring", stiffness: 380, damping: 32 }}
                    aria-hidden
                  />
                )}
                <span className="relative">{p}일</span>
              </button>
            );
          })}
        </div>
      </div>

      {error && (
        <p className="mt-3 rounded-sm border border-danger bg-bg-elevated px-3 py-2 text-xs text-danger">
          데이터를 불러올 수 없습니다: {error}
        </p>
      )}

      {initialLoad ? (
        <div className="mt-4 h-32 rounded-lg bg-bg-elevated" />
      ) : points && points.length > 0 ? (
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <LineSeries
            title="신규 가입"
            points={points}
            field="newMembers"
            color="var(--info)"
          />
          <LineSeries
            title="풀이"
            points={points}
            field="newSolves"
            color="var(--primary)"
          />
        </div>
      ) : (
        <p className="mt-4 text-xs text-text-muted">데이터가 없습니다.</p>
      )}
    </div>
  );
}

// ── SVG 좌표계 (대시보드 StudyActivityChart 동일) ──
const VB_W = 600;
const VB_H = 160;
const PADDING_LEFT = 12;
const PADDING_RIGHT = 36;
const PADDING_TOP = 16;
const PADDING_BOTTOM = 28;
const PLOT_W = VB_W - PADDING_LEFT - PADDING_RIGHT;
const PLOT_H = VB_H - PADDING_TOP - PADDING_BOTTOM;

function niceCeil(v: number): number {
  if (v <= 0) return 4;
  if (v <= 5) return 5;
  if (v <= 10) return 10;
  if (v <= 20) return 20;
  if (v <= 50) return 50;
  if (v <= 100) return 100;
  return Math.ceil(v / 50) * 50;
}

function buildLinePath(points: { x: number; y: number }[]): string {
  if (points.length === 0) return "";
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`;
  let d = `M ${points[0].x} ${points[0].y}`;
  for (let i = 1; i < points.length; i++) d += ` L ${points[i].x} ${points[i].y}`;
  return d;
}

function buildAreaPath(points: { x: number; y: number }[], baselineY: number): string {
  const line = buildLinePath(points);
  if (!line) return "";
  const last = points[points.length - 1];
  const first = points[0];
  return `${line} L ${last.x} ${baselineY} L ${first.x} ${baselineY} Z`;
}

function formatTick(v: number): string {
  if (v >= 1000) return `${(v / 1000).toFixed(v % 1000 === 0 ? 0 : 1)}k`;
  return v.toLocaleString(undefined, { maximumFractionDigits: 0 });
}

function LineSeries({
  title,
  points: data,
  field,
  color,
}: {
  title: string;
  points: AdminTrendPoint[];
  field: Field;
  color: string;
}) {
  const gradId = useId();
  const values = data.map((p) => p[field]);
  const total = values.reduce((s, v) => s + v, 0);
  const avg = data.length > 0 ? total / data.length : 0;
  const dataMax = Math.max(...values, 1);
  const max = niceCeil(dataMax);

  const colW = PLOT_W / data.length;
  const baseY = PADDING_TOP + PLOT_H;

  const yTicks = useMemo(
    () =>
      [0, max / 2, max].map((v) => ({
        value: Math.round(v),
        y: PADDING_TOP + (1 - v / max) * PLOT_H,
      })),
    [max],
  );

  const labelInterval = data.length <= 7 ? 1 : data.length <= 14 ? 2 : 5;
  const visibleLabels = useMemo(
    () =>
      data
        .map((d, i) => ({ d, i }))
        .filter(({ i }) => i % labelInterval === 0 || i === data.length - 1),
    [data, labelInterval],
  );

  const pts = useMemo(
    () =>
      data.map((p, i) => ({
        x: PADDING_LEFT + i * colW + colW / 2,
        y: baseY - (p[field] / max) * PLOT_H,
      })),
    [data, colW, baseY, max, field],
  );
  const areaPath = buildAreaPath(pts, baseY);
  const linePath = buildLinePath(pts);

  return (
    <div>
      <div className="mb-2 flex items-baseline justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <span className="h-1.5 w-1.5 rounded-full" style={{ background: color }} aria-hidden />
          <span className="text-xs font-semibold text-text">{title}</span>
        </div>
        <span className="text-[10px] text-text-muted">
          합계{" "}
          <AnimatedInt
            value={total}
            className="font-semibold tabular-nums text-text"
          />
          <span className="mx-1 text-border">·</span>
          평균{" "}
          <span className="font-semibold tabular-nums text-text">{avg.toFixed(1)}</span>
        </span>
      </div>

      <svg
        viewBox={`0 0 ${VB_W} ${VB_H}`}
        className="w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label={`${title} 일별 추이`}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.28} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>

        {/* y grid + label */}
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
                {formatTick(t.value)}
              </motion.text>
            </motion.g>
          ))}
        </AnimatePresence>

        {/* area + line — d 모핑 */}
        <motion.path
          animate={{ d: areaPath }}
          initial={false}
          transition={PATH_TWEEN}
          fill={`url(#${gradId})`}
        />
        <motion.path
          animate={{ d: linePath }}
          initial={false}
          transition={PATH_TWEEN}
          fill="none"
          stroke={color}
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* x 라벨 — date key 안정, 좌표만 부드럽게 이동 */}
        <AnimatePresence mode="popLayout">
          {visibleLabels.map(({ d, i }) => {
            const cx = PADDING_LEFT + i * colW + colW / 2;
            const dt = new Date(d.date + "T00:00:00");
            return (
              <motion.text
                key={d.date}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1, x: cx }}
                exit={{ opacity: 0 }}
                transition={FADE_TWEEN}
                y={PADDING_TOP + PLOT_H + 16}
                textAnchor="middle"
                fill="var(--text-subtle)"
                fontSize="10"
              >
                {`${dt.getMonth() + 1}/${dt.getDate()}`}
              </motion.text>
            );
          })}
        </AnimatePresence>

        {total === 0 && (
          <text
            x={VB_W / 2}
            y={VB_H / 2}
            textAnchor="middle"
            fontSize={12}
            fill="var(--text-muted)"
          >
            해당 기간 활동이 없습니다
          </text>
        )}
      </svg>
    </div>
  );
}
