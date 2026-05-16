"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AnimatePresence,
  motion,
  useMotionValue,
  useSpring,
  useTransform,
} from "framer-motion";

type DayData = { date: string; total: number; correct: number; wrong: number };

type Period = 7 | 14 | 30;

type StudyActivityChartProps = {
  /** 최근 30일치 일별 풀이 데이터 (오래된 → 최신 순). 기간 토글로 슬라이스. */
  data: DayData[];
  /** 외부 페치 진행 중인지 — true 면 segmented control 에 pending 상태 표시.
   * skeleton 으로 대체하지 않고 기존 차트 유지. */
  pending?: boolean;
};

const VB_W = 600;
const VB_H = 200;
const PADDING_LEFT = 12;
const PADDING_RIGHT = 36;
const PADDING_TOP = 16;
const PADDING_BOTTOM = 28;
const PLOT_W = VB_W - PADDING_LEFT - PADDING_RIGHT;
const PLOT_H = VB_H - PADDING_TOP - PADDING_BOTTOM;

const DOW_SHORT = ["일", "월", "화", "수", "목", "금", "토"];

const SPRING_FAST = { stiffness: 220, damping: 28, mass: 0.6 };
const PATH_TWEEN = { duration: 0.35, ease: [0.4, 0, 0.2, 1] as const };
const FADE_TWEEN = { duration: 0.25, ease: [0.4, 0, 0.2, 1] as const };

function niceCeil(v: number): number {
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
  for (let i = 1; i < points.length; i++) {
    d += ` L ${points[i].x} ${points[i].y}`;
  }
  return d;
}

function buildAreaPath(points: { x: number; y: number }[], baselineY: number): string {
  const line = buildLinePath(points);
  if (!line) return "";
  const last = points[points.length - 1];
  const first = points[0];
  return `${line} L ${last.x} ${baselineY} L ${first.x} ${baselineY} Z`;
}

function computeStreak(data: DayData[]): number {
  let streak = 0;
  for (let i = data.length - 1; i >= 0; i--) {
    if (data[i].total > 0) streak++;
    else break;
  }
  return streak;
}

function computeWeekDelta(data: DayData[]): number | null {
  if (data.length < 14) return null;
  const recent14 = data.slice(-14);
  const prev = recent14.slice(0, 7).reduce((s, d) => s + d.total, 0);
  const cur = recent14.slice(7, 14).reduce((s, d) => s + d.total, 0);
  if (prev === 0 && cur === 0) return 0;
  if (prev === 0) return 100;
  return Math.round(((cur - prev) / prev) * 100);
}

/** spring 으로 보간되는 정수 숫자. period 변경 시 0 으로 리셋되지 않고 현재값에서 부드럽게 이동. */
function AnimatedInt({ value, className }: { value: number; className?: string }) {
  const mv = useMotionValue(value);
  const spring = useSpring(mv, SPRING_FAST);
  const rounded = useTransform(spring, (v) => Math.round(v).toString());
  useEffect(() => {
    mv.set(value);
  }, [value, mv]);
  return <motion.span className={className}>{rounded}</motion.span>;
}

export default function StudyActivityChart({ data, pending = false }: StudyActivityChartProps) {
  const [period, setPeriod] = useState<Period>(14);
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);

  const sliced = useMemo(() => data.slice(-period), [data, period]);

  const totalSum = sliced.reduce((s, d) => s + d.total, 0);
  const correctSum = sliced.reduce((s, d) => s + d.correct, 0);
  const wrongSum = totalSum - correctSum;
  const accuracy = totalSum > 0 ? Math.round((correctSum / totalSum) * 100) : null;

  const streak = computeStreak(sliced);
  const weekDelta = computeWeekDelta(data);

  const dataMax = Math.max(...sliced.map((d) => d.total), 1);
  const max = niceCeil(dataMax);

  const colW = PLOT_W / sliced.length;
  const baseY = PADDING_TOP + PLOT_H;

  const yTicks = useMemo(
    () =>
      [0, max / 2, max].map((v) => ({
        value: Math.round(v),
        y: PADDING_TOP + (1 - v / max) * PLOT_H,
      })),
    [max],
  );

  // x 라벨 간격 — 7일은 모두, 14일은 격일, 30일은 5일 간격 + 마지막 항상
  const labelInterval = sliced.length <= 7 ? 1 : sliced.length <= 14 ? 2 : 5;
  const visibleLabels = useMemo(
    () =>
      sliced
        .map((d, i) => ({ d, i }))
        .filter(({ i }) => i % labelInterval === 0 || i === sliced.length - 1),
    [sliced, labelInterval],
  );

  const points = useMemo(
    () =>
      sliced.map((d, i) => ({
        x: PADDING_LEFT + i * colW + colW / 2,
        y: baseY - (d.total / max) * PLOT_H,
      })),
    [sliced, colW, baseY, max],
  );
  const areaPath = buildAreaPath(points, baseY);
  const linePath = buildLinePath(points);

  const deltaPositive = weekDelta != null && weekDelta > 0;
  const deltaNegative = weekDelta != null && weekDelta < 0;
  const hovered = hoveredIdx !== null ? sliced[hoveredIdx] : null;

  return (
    <section
      className="mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5"
      role="img"
      aria-label={`최근 ${period}일 학습량. 총 ${totalSum}문제, 정답 ${correctSum}, 오답 ${wrongSum}`}
      aria-busy={pending}
    >
      {/* 헤더 */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xs font-medium uppercase tracking-wider text-text-subtle">
            최근 {period}일 학습량
          </h2>
          <div className="mt-1.5 flex items-baseline gap-2">
            <AnimatedInt
              value={totalSum}
              className="text-3xl font-bold tabular-nums text-text"
            />
            <span className="text-xs text-text-muted">문제</span>
            {accuracy !== null && (
              <span className="ml-1 text-xs text-text-muted tabular-nums">
                · 정답률{" "}
                <AnimatedInt value={accuracy} className="font-semibold text-text" />
                <span className="font-semibold text-text">%</span>
              </span>
            )}
          </div>
          <div className="mt-2 flex items-center gap-3 text-[11px]">
            {streak > 0 && (
              <span className="inline-flex items-center gap-1 text-text-muted">
                <span>
                  <span className="font-semibold text-text">{streak}</span>일 연속
                </span>
              </span>
            )}
            {weekDelta !== null && period >= 14 && totalSum > 0 && (
              <span
                className={`inline-flex items-center gap-0.5 tabular-nums ${
                  deltaPositive
                    ? "text-primary"
                    : deltaNegative
                      ? "text-danger"
                      : "text-text-muted"
                }`}
              >
                <span aria-hidden>{deltaPositive ? "▲" : deltaNegative ? "▼" : "·"}</span>
                전주 대비 {weekDelta > 0 ? `+${weekDelta}` : weekDelta}%
              </span>
            )}
          </div>
        </div>

        {/* 기간 segmented control — framer-motion layoutId 슬라이딩 indicator.
         * toggle-state 패턴이라 role=tab 대신 button + aria-pressed 조합. */}
        <div
          role="group"
          aria-label="기간 선택"
          className="relative inline-flex items-center rounded-sm border border-border bg-bg-elevated p-0.5 text-xs"
        >
          {([7, 14, 30] as Period[]).map((p) => {
            const isActive = period === p;
            return (
              <button
                key={p}
                type="button"
                aria-pressed={isActive}
                aria-label={`최근 ${p}일`}
                disabled={pending}
                onClick={() => setPeriod(p)}
                className={`relative z-10 rounded-sm px-2.5 py-1 font-medium transition-colors duration-200 ${
                  isActive
                    ? "text-text"
                    : "text-text-subtle hover:text-text"
                } ${pending ? "cursor-wait opacity-70" : ""}`}
              >
                {isActive && (
                  <motion.span
                    layoutId="period-active-indicator"
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

      {/* 차트 + 호버 */}
      <div className="relative mt-4">
        <svg
          viewBox={`0 0 ${VB_W} ${VB_H}`}
          className="w-full"
          preserveAspectRatio="none"
        >
          {/* gradient def — area fill 용 */}
          <defs>
            <linearGradient id="totalAreaGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.28" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
            </linearGradient>
          </defs>

          {/* y grid + label — max 값 변경 시 부드럽게 교체 (AnimatePresence per tick value) */}
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
                  stroke="var(--border)"
                  strokeWidth={1}
                  strokeDasharray={t.value === 0 ? "0" : "2 3"}
                  opacity={t.value === 0 ? 0.9 : 0.5}
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
                  {t.value}
                </motion.text>
              </motion.g>
            ))}
          </AnimatePresence>

          {/* area fill — period 토글 시 d 가 spring 으로 morphing.
           * 동일 element 가 유지되므로 unmount/remount 없음. */}
          <motion.path
            animate={{ d: areaPath }}
            initial={false}
            transition={PATH_TWEEN}
            fill="url(#totalAreaGradient)"
          />
          <motion.path
            animate={{ d: linePath }}
            initial={false}
            transition={PATH_TWEEN}
            fill="none"
            stroke="var(--primary)"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* hit area + 호버 dot */}
          {sliced.map((d, i) => {
            const cx = PADDING_LEFT + i * colW + colW / 2;
            const cy = baseY - (d.total / max) * PLOT_H;
            const isHovered = hoveredIdx === i;
            return (
              <g
                key={d.date}
                onMouseEnter={() => setHoveredIdx(i)}
                onMouseLeave={() =>
                  setHoveredIdx((prev) => (prev === i ? null : prev))
                }
              >
                <rect
                  x={cx - colW / 2}
                  y={PADDING_TOP}
                  width={colW}
                  height={PLOT_H}
                  fill="transparent"
                />
                {isHovered && (
                  <>
                    <line
                      x1={cx}
                      x2={cx}
                      y1={PADDING_TOP}
                      y2={baseY}
                      stroke="var(--border-strong)"
                      strokeWidth={1}
                      strokeDasharray="2 2"
                      opacity={0.6}
                    />
                    <circle cx={cx} cy={cy} r={5} fill="var(--bg)" />
                    <circle cx={cx} cy={cy} r={4} fill="var(--primary)" />
                  </>
                )}
              </g>
            );
          })}

          {/* x 라벨 — date key 안정. 추가/제거되는 라벨만 opacity fade,
           * 유지되는 라벨은 x 좌표만 부드럽게 이동. */}
          <AnimatePresence mode="popLayout">
            {visibleLabels.map(({ d, i }) => {
              const cx = PADDING_LEFT + i * colW + colW / 2;
              const dt = new Date(d.date);
              const label =
                sliced.length <= 7
                  ? DOW_SHORT[dt.getDay()]
                  : `${dt.getMonth() + 1}/${dt.getDate()}`;
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
                  {label}
                </motion.text>
              );
            })}
          </AnimatePresence>
        </svg>

        {/* HTML 툴팁 — 호버 시점 데이터만 표시. fade in/out. */}
        <AnimatePresence>
          {hovered && hoveredIdx !== null && (() => {
            const fmtDate = (s: string) =>
              new Date(s).toLocaleDateString("ko-KR", {
                month: "short",
                day: "numeric",
                weekday: "short",
              });
            return (
              <motion.div
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 4 }}
                transition={FADE_TWEEN}
                className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-md border border-border bg-bg-elevated px-3 py-2 text-[11px] shadow-md"
                style={{
                  left: `${((PADDING_LEFT + hoveredIdx * colW + colW / 2) / VB_W) * 100}%`,
                  top: `${((baseY - (hovered.total / max) * PLOT_H - 8) / VB_H) * 100}%`,
                }}
              >
                <p className="font-semibold text-text">{fmtDate(hovered.date)}</p>
                <p className="mt-1 tabular-nums">
                  <span className="font-bold text-text">{hovered.total}</span>
                  <span className="ml-1 text-text-subtle">문제</span>
                </p>
                <p className="mt-1 text-text-subtle tabular-nums">
                  정답 {hovered.correct} · 오답 {hovered.wrong}
                </p>
              </motion.div>
            );
          })()}
        </AnimatePresence>
      </div>
    </section>
  );
}
