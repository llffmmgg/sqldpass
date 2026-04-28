"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 트리거, count-up RAF, SVG path 길이 측정은 effect 안에서 setState 가 자연스러운 패턴 */

import { useEffect, useRef, useState } from "react";

type DayData = { date: string; total: number; correct: number; wrong: number };

type StudyActivityChartProps = {
  data: DayData[];
  /** 전체 사용자 14일 일평균. 제공 시 비교용 점선을 추가 표시. */
  overallAvg?: number;
};

const DOW_SHORT = ["일", "월", "화", "수", "목", "금", "토"];

const VB_W = 600;
const VB_H = 200;
const PADDING_LEFT = 32; // Y축 라벨 폭
const PADDING_RIGHT = 12;
const PADDING_TOP = 16;
const PADDING_BOTTOM = 28;
const PLOT_W = VB_W - PADDING_LEFT - PADDING_RIGHT;
const PLOT_H = VB_H - PADDING_TOP - PADDING_BOTTOM;
const COL_W = PLOT_W / 13; // 14 점이면 간격은 13개

const COLOR_TOTAL = "var(--primary)"; // emerald
const COLOR_CORRECT = "#3b82f6"; // blue-500 — primary 와 톤 분리
const COLOR_WRONG = "#f43f5e"; // rose-500

function toLocalDateStr(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function yFor(value: number, max: number): number {
  if (max <= 0) return PADDING_TOP + PLOT_H;
  return PADDING_TOP + (1 - value / max) * PLOT_H;
}

/** Catmull-Rom 효과의 부드러운 곡선 — midpoint cubic bezier */
function buildLinePath(points: { x: number; y: number }[]): string {
  if (points.length === 0) return "";
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`;
  let d = `M ${points[0].x} ${points[0].y}`;
  for (let i = 1; i < points.length; i++) {
    const prev = points[i - 1];
    const cur = points[i];
    const midX = (prev.x + cur.x) / 2;
    d += ` C ${midX} ${prev.y}, ${midX} ${cur.y}, ${cur.x} ${cur.y}`;
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

function useCountUp(target: number, durationMs = 900): number {
  const [value, setValue] = useState(0);
  useEffect(() => {
    let raf = 0;
    let start: number | null = null;
    const tick = (ts: number) => {
      if (start === null) start = ts;
      const t = Math.min(1, (ts - start) / durationMs);
      const eased = 1 - Math.pow(1 - t, 3);
      setValue(Math.round(target * eased));
      if (t < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target, durationMs]);
  return value;
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
  const prev = data.slice(0, 7).reduce((s, d) => s + d.total, 0);
  const cur = data.slice(7, 14).reduce((s, d) => s + d.total, 0);
  if (prev === 0 && cur === 0) return 0;
  if (prev === 0) return 100;
  return Math.round(((cur - prev) / prev) * 100);
}

function fmtNum(v: number): string {
  if (v >= 10) return Math.round(v).toString();
  return v.toFixed(1);
}

function niceCeil(v: number): number {
  if (v <= 5) return 5;
  if (v <= 10) return 10;
  if (v <= 20) return 20;
  if (v <= 50) return 50;
  if (v <= 100) return 100;
  return Math.ceil(v / 50) * 50;
}

export default function StudyActivityChart({ data, overallAvg }: StudyActivityChartProps) {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);
  const [mounted, setMounted] = useState(false);
  const svgRef = useRef<SVGSVGElement | null>(null);

  useEffect(() => {
    const t = requestAnimationFrame(() => setMounted(true));
    return () => cancelAnimationFrame(t);
  }, []);

  const todayStr = toLocalDateStr(new Date());
  const totalSum = data.reduce((s, d) => s + d.total, 0);
  const correctSum = data.reduce((s, d) => s + d.correct, 0);
  const totalAvg = totalSum / data.length;
  const correctAvg = correctSum / data.length;
  const wrongAvg = (totalSum - correctSum) / data.length;
  const accuracy = totalSum > 0 ? Math.round((correctSum / totalSum) * 100) : null;

  const hasOverall = typeof overallAvg === "number" && overallAvg > 0;
  const dataMax = Math.max(...data.map((d) => d.total), hasOverall ? overallAvg! : 0, 1);
  const max = niceCeil(dataMax);

  const totalAnimated = useCountUp(totalSum);
  const streak = computeStreak(data);
  const weekDelta = computeWeekDelta(data);

  const enriched = data.map((day, i) => {
    const d = new Date(day.date);
    const dow = d.getDay();
    const isToday = day.date === todayStr;
    const isWeekend = dow === 0 || dow === 6;
    const cx = PADDING_LEFT + i * COL_W;
    return {
      ...day,
      dow,
      month: d.getMonth() + 1,
      dayNum: d.getDate(),
      isToday,
      isWeekend,
      cx,
      yTotal: yFor(day.total, max),
      yCorrect: yFor(day.correct, max),
      yWrong: yFor(day.wrong, max),
    };
  });

  const todayIdx = enriched.findIndex((d) => d.isToday);
  const todayPoint = todayIdx >= 0 ? enriched[todayIdx] : null;

  const totalPoints = enriched.map((d) => ({ x: d.cx, y: d.yTotal }));
  const correctPoints = enriched.map((d) => ({ x: d.cx, y: d.yCorrect }));
  const wrongPoints = enriched.map((d) => ({ x: d.cx, y: d.yWrong }));

  const baselineY = PADDING_TOP + PLOT_H;
  const totalLine = buildLinePath(totalPoints);
  const totalArea = buildAreaPath(totalPoints, baselineY);
  const correctLine = buildLinePath(correctPoints);
  const wrongLine = buildLinePath(wrongPoints);

  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((r) => ({
    value: Math.round(max * r),
    y: PADDING_TOP + (1 - r) * PLOT_H,
  }));

  const hovered = hoveredIdx !== null ? enriched[hoveredIdx] : null;

  function onSvgMouseMove(e: React.MouseEvent<SVGSVGElement>) {
    const svg = svgRef.current;
    if (!svg) return;
    const rect = svg.getBoundingClientRect();
    const xRatio = (e.clientX - rect.left) / rect.width;
    const xInView = xRatio * VB_W;
    const xInPlot = xInView - PADDING_LEFT;
    if (xInPlot < -COL_W / 2 || xInPlot > PLOT_W + COL_W / 2) {
      setHoveredIdx(null);
      return;
    }
    const idx = Math.round(xInPlot / COL_W);
    setHoveredIdx(Math.max(0, Math.min(13, idx)));
  }

  const deltaPositive = weekDelta != null && weekDelta > 0;
  const deltaNegative = weekDelta != null && weekDelta < 0;

  return (
    <div
      className="mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5"
      role="img"
      aria-label={`최근 2주 학습량. 총 ${totalSum}문제, 정답 ${correctSum}, 오답 ${totalSum - correctSum}`}
    >
      {/* ── 헤더 ───────────────────────────────────────────────── */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xs font-medium uppercase tracking-wider text-muted">
            최근 2주 학습량
          </h2>
          <div className="mt-1 flex items-baseline gap-2">
            <span className="text-3xl font-bold tabular-nums text-foreground">
              {totalAnimated}
            </span>
            <span className="text-xs text-muted">문제</span>
            {weekDelta != null && totalSum > 0 && (
              <span
                className={`inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-[10px] font-bold tabular-nums transition-opacity duration-500 ${
                  mounted ? "opacity-100" : "opacity-0"
                } ${
                  deltaPositive
                    ? "bg-primary/15 text-primary"
                    : deltaNegative
                      ? "bg-red-500/15 text-red-500 dark:text-red-300"
                      : "bg-border/50 text-muted"
                }`}
              >
                <span aria-hidden>
                  {deltaPositive ? "▲" : deltaNegative ? "▼" : "·"}
                </span>
                {weekDelta > 0 ? `+${weekDelta}` : weekDelta}%
              </span>
            )}
            {accuracy != null && (
              <span className="rounded-full border border-border bg-background px-2 py-0.5 text-[10px] font-semibold tabular-nums text-muted">
                정답률 <span className="text-foreground">{accuracy}%</span>
              </span>
            )}
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-[10px] text-muted tabular-nums">
          {streak > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold text-amber-600 dark:text-amber-300">
              <span aria-hidden>🔥</span>
              {streak}일 연속
            </span>
          )}
          <Legend color={COLOR_TOTAL} label="전체" value={fmtNum(totalAvg)} />
          <Legend color={COLOR_CORRECT} label="맞춘" value={fmtNum(correctAvg)} />
          <Legend color={COLOR_WRONG} label="틀린" value={fmtNum(wrongAvg)} />
          {hasOverall && (
            <span className="inline-flex items-center gap-1">
              <span
                className="inline-block h-0.5 w-2.5 border-t border-dashed border-accent"
                aria-hidden
              />
              전체 <span className="font-semibold text-accent">{fmtNum(overallAvg!)}</span>
            </span>
          )}
        </div>
      </div>

      {/* ── 차트 본체 ─────────────────────────────────────────── */}
      <div className="relative mt-5 h-52">
        <svg
          ref={svgRef}
          className="absolute inset-0 h-full w-full"
          viewBox={`0 0 ${VB_W} ${VB_H}`}
          preserveAspectRatio="none"
          onMouseMove={onSvgMouseMove}
          onMouseLeave={() => setHoveredIdx(null)}
        >
          <defs>
            <linearGradient id="study-area-total" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.32" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
            </linearGradient>
          </defs>

          {/* 가로 그리드 + Y축 라벨 */}
          {yTicks.map((tick) => (
            <g key={tick.value}>
              <line
                x1={PADDING_LEFT}
                y1={tick.y}
                x2={VB_W - PADDING_RIGHT}
                y2={tick.y}
                stroke="var(--border)"
                strokeWidth={1}
                strokeDasharray="2 4"
                opacity={tick.value === 0 ? 0.6 : 0.35}
                vectorEffect="non-scaling-stroke"
              />
              <text
                x={PADDING_LEFT - 6}
                y={tick.y + 3}
                textAnchor="end"
                fill="var(--muted)"
                fontSize="10"
                opacity="0.7"
                style={{ fontVariantNumeric: "tabular-nums" }}
              >
                {tick.value}
              </text>
            </g>
          ))}

          {/* 호버 세로 점선 */}
          {hovered && (
            <line
              x1={hovered.cx}
              y1={PADDING_TOP}
              x2={hovered.cx}
              y2={baselineY}
              stroke="var(--muted)"
              strokeWidth={1}
              strokeDasharray="3 3"
              opacity={0.45}
              vectorEffect="non-scaling-stroke"
            />
          )}

          {/* 전체 area */}
          {totalSum > 0 && (
            <path
              d={totalArea}
              fill="url(#study-area-total)"
              style={{
                opacity: mounted ? 1 : 0,
                transition: "opacity 500ms ease-out 700ms",
              }}
            />
          )}

          {/* 라인들 — pathLength=1 정규화로 dataset 변경에도 안정적인 draw 애니메이션 */}
          <path
            d={wrongLine}
            fill="none"
            stroke={COLOR_WRONG}
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            opacity={0.85}
            vectorEffect="non-scaling-stroke"
            pathLength={1}
            strokeDasharray="1 1"
            style={{
              strokeDashoffset: mounted ? 0 : 1,
              transition: "stroke-dashoffset 1100ms cubic-bezier(0.22,1,0.36,1) 250ms",
            }}
          />
          <path
            d={correctLine}
            fill="none"
            stroke={COLOR_CORRECT}
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            opacity={0.9}
            vectorEffect="non-scaling-stroke"
            pathLength={1}
            strokeDasharray="1 1"
            style={{
              strokeDashoffset: mounted ? 0 : 1,
              transition: "stroke-dashoffset 1000ms cubic-bezier(0.22,1,0.36,1) 150ms",
            }}
          />
          <path
            d={totalLine}
            fill="none"
            stroke={COLOR_TOTAL}
            strokeWidth={2.5}
            strokeLinecap="round"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
            pathLength={1}
            strokeDasharray="1 1"
            style={{
              strokeDashoffset: mounted ? 0 : 1,
              transition: "stroke-dashoffset 900ms cubic-bezier(0.22,1,0.36,1) 0ms",
              filter: "drop-shadow(0 0 6px var(--glow))",
            }}
          />

          {/* 호버 점 3개 */}
          {hovered && (
            <>
              <HoverDot cx={hovered.cx} cy={hovered.yWrong} color={COLOR_WRONG} />
              <HoverDot cx={hovered.cx} cy={hovered.yCorrect} color={COLOR_CORRECT} />
              <HoverDot cx={hovered.cx} cy={hovered.yTotal} color={COLOR_TOTAL} primary />
            </>
          )}

          {/* 오늘 위치 ring pulse — 호버 중엔 숨김 */}
          {todayPoint && totalSum > 0 && hoveredIdx === null && (
            <g>
              <circle
                cx={todayPoint.cx}
                cy={todayPoint.yTotal}
                r={5}
                fill="var(--primary)"
                stroke="var(--surface)"
                strokeWidth={2}
              />
              <circle
                cx={todayPoint.cx}
                cy={todayPoint.yTotal}
                r={5}
                fill="none"
                stroke="var(--primary)"
                strokeOpacity={0.55}
                strokeWidth={2}
                style={{ animation: "study-ping 1.8s ease-out infinite", transformBox: "fill-box", transformOrigin: "center" }}
              />
            </g>
          )}
        </svg>

        {/* 다크 툴팁 — ref.png 스타일 */}
        {hovered && (
          <Tooltip
            x={hovered.cx}
            y={Math.min(hovered.yTotal, hovered.yCorrect, hovered.yWrong)}
            month={hovered.month}
            day={hovered.dayNum}
            dow={hovered.dow}
            total={hovered.total}
            correct={hovered.correct}
            wrong={hovered.wrong}
            isLeft={hoveredIdx! > 9}
          />
        )}
      </div>

      {/* X축 라벨 — 호버 인덱스만 pill highlight */}
      <div
        className="mt-1 flex"
        style={{
          paddingLeft: `calc(${(PADDING_LEFT / VB_W) * 100}% - ${COL_W / 2 / VB_W * 100}%)`,
          paddingRight: `calc(${(PADDING_RIGHT / VB_W) * 100}% - ${COL_W / 2 / VB_W * 100}%)`,
        }}
      >
        {enriched.map((day, i) => {
          const isHovered = hoveredIdx === i;
          return (
            <div
              key={`label-${day.date}`}
              className="flex flex-1 flex-col items-center"
            >
              <span
                className={`rounded-full px-1.5 py-0.5 text-[9px] tabular-nums transition-colors duration-150 ${
                  isHovered
                    ? "bg-border text-foreground font-semibold"
                    : day.isToday
                      ? "font-bold text-primary"
                      : day.isWeekend
                        ? "text-accent/70"
                        : "text-muted/60"
                }`}
              >
                {day.dayNum}
              </span>
              <span
                className={`text-[8px] ${
                  day.isToday
                    ? "font-semibold text-primary"
                    : day.isWeekend
                      ? "text-accent/60"
                      : "text-muted/45"
                }`}
              >
                {DOW_SHORT[day.dow]}
              </span>
            </div>
          );
        })}
      </div>

      <style jsx>{`
        @keyframes study-ping {
          0% {
            transform: scale(1);
            opacity: 0.7;
          }
          75% {
            transform: scale(2.6);
            opacity: 0;
          }
          100% {
            transform: scale(2.6);
            opacity: 0;
          }
        }
      `}</style>
    </div>
  );
}

function Legend({ color, label, value }: { color: string; label: string; value: string }) {
  return (
    <span className="inline-flex items-center gap-1">
      <span
        className="inline-block h-2 w-2 rounded-full"
        style={{ backgroundColor: color }}
        aria-hidden
      />
      {label} <span className="font-semibold text-foreground">{value}</span>
    </span>
  );
}

function HoverDot({
  cx,
  cy,
  color,
  primary = false,
}: {
  cx: number;
  cy: number;
  color: string;
  primary?: boolean;
}) {
  return (
    <g>
      <circle
        cx={cx}
        cy={cy}
        r={primary ? 5 : 4}
        fill="var(--surface)"
        stroke={color}
        strokeWidth={primary ? 2.5 : 2}
      />
      {primary && (
        <circle
          cx={cx}
          cy={cy}
          r={2}
          fill={color}
        />
      )}
    </g>
  );
}

function Tooltip({
  x,
  y,
  month,
  day,
  dow,
  total,
  correct,
  wrong,
  isLeft,
}: {
  x: number;
  y: number;
  month: number;
  day: number;
  dow: number;
  total: number;
  correct: number;
  wrong: number;
  isLeft: boolean;
}) {
  const xPct = (x / VB_W) * 100;
  const yPct = (y / VB_H) * 100;
  const rate = total > 0 ? Math.round((correct / total) * 100) : 0;
  return (
    <div
      className="pointer-events-none absolute z-10 rounded-xl border border-zinc-700 bg-zinc-900 px-3 py-2 text-xs text-white shadow-xl ring-1 ring-black/10"
      style={{
        left: `${xPct}%`,
        top: `${yPct}%`,
        transform: `translate(${isLeft ? "-100%" : "0"}, calc(-100% - 14px))`,
        marginLeft: isLeft ? "-12px" : "12px",
        minWidth: 150,
      }}
    >
      <p className="text-[11px] font-semibold tabular-nums">
        {month}월 {day}일 ({DOW_SHORT[dow]})
      </p>
      <div className="mt-1.5 space-y-0.5">
        <TooltipRow color={COLOR_TOTAL} label="전체" value={total} />
        <TooltipRow color={COLOR_CORRECT} label="맞춘" value={correct} />
        <TooltipRow color={COLOR_WRONG} label="틀린" value={wrong} />
      </div>
      {total > 0 && (
        <p className="mt-1.5 border-t border-zinc-700 pt-1 text-[10px] tabular-nums text-zinc-400">
          정답률 <span className="font-semibold text-white">{rate}%</span>
        </p>
      )}
    </div>
  );
}

function TooltipRow({ color, label, value }: { color: string; label: string; value: number }) {
  return (
    <div className="flex items-center justify-between gap-3 text-[11px] tabular-nums">
      <span className="flex items-center gap-1.5">
        <span
          className="inline-block h-2 w-0.5 rounded-full"
          style={{ backgroundColor: color }}
          aria-hidden
        />
        <span className="text-zinc-300">{label}</span>
      </span>
      <span className="font-semibold">{value}</span>
    </div>
  );
}
