"use client";

import { useEffect, useState } from "react";

type DayData = { date: string; count: number };

type StudyActivityChartProps = {
  data: DayData[];
};

const DOW_SHORT = ["일", "월", "화", "수", "목", "금", "토"];
const DOW_LONG = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"];

const VB_W = 560;
const VB_H = 160;
const CHART_TOP = 12;
const CHART_BOTTOM = 150;
const CHART_H = CHART_BOTTOM - CHART_TOP;
const COL_W = VB_W / 14;

function toLocalDateStr(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function yFor(count: number, max: number): number {
  return CHART_BOTTOM - (count / max) * CHART_H;
}

function buildAreaPath(points: { x: number; y: number }[]): string {
  if (points.length === 0) return "";
  let d = `M ${points[0].x} ${points[0].y}`;
  for (let i = 1; i < points.length; i++) {
    const prev = points[i - 1];
    const cur = points[i];
    const midX = (prev.x + cur.x) / 2;
    d += ` C ${midX} ${prev.y}, ${midX} ${cur.y}, ${cur.x} ${cur.y}`;
  }
  d += ` L ${points[points.length - 1].x} ${CHART_BOTTOM}`;
  d += ` L ${points[0].x} ${CHART_BOTTOM}`;
  d += " Z";
  return d;
}

export default function StudyActivityChart({ data }: StudyActivityChartProps) {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const t = requestAnimationFrame(() => setMounted(true));
    return () => cancelAnimationFrame(t);
  }, []);

  const todayStr = toLocalDateStr(new Date());
  const total = data.reduce((s, d) => s + d.count, 0);
  const avg = total / data.length;
  const max = Math.max(...data.map((d) => d.count), 1);

  let maxIdx = 0;
  data.forEach((d, i) => {
    if (d.count > data[maxIdx].count) maxIdx = i;
  });
  const hasMax = data[maxIdx].count > 0;
  const avgDisplay = avg >= 10 ? Math.round(avg).toString() : avg.toFixed(1);

  const enriched = data.map((day, i) => {
    const d = new Date(day.date);
    const dow = d.getDay();
    const isToday = day.date === todayStr;
    const isWeekend = dow === 0 || dow === 6;
    const centerX = i * COL_W + COL_W / 2;
    const y = yFor(day.count, max);
    const heightPct = day.count > 0 ? Math.max((day.count / max) * 100, 5) : 0;
    return {
      ...day,
      dow,
      month: d.getMonth() + 1,
      dayNum: d.getDate(),
      isToday,
      isWeekend,
      centerX,
      y,
      heightPct,
    };
  });

  const areaPath = buildAreaPath(enriched.map((d) => ({ x: d.centerX, y: d.y })));
  const avgY = yFor(avg, max);
  const avgTopPct = (avgY / VB_H) * 100;

  const hovered = hoveredIdx !== null ? enriched[hoveredIdx] : null;

  return (
    <div
      className="mt-6 rounded-xl border border-border bg-surface p-5"
      role="img"
      aria-label={`최근 2주 학습량 차트. 총 ${total}문제, 일평균 ${avgDisplay}문제`}
    >
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-sm font-semibold">최근 2주 학습량</h2>
        <div className="flex items-center gap-3 text-xs text-muted tabular-nums">
          <span>
            총 <span className="font-semibold text-foreground">{total}</span>
          </span>
          <span className="h-3 w-px bg-border" aria-hidden />
          <span>
            일평균 <span className="font-semibold text-foreground">{avgDisplay}</span>
          </span>
          {hasMax && (
            <>
              <span className="h-3 w-px bg-border" aria-hidden />
              <span>
                최다 <span className="font-semibold text-foreground">{data[maxIdx].count}</span>
              </span>
            </>
          )}
        </div>
      </div>

      <div className="relative mt-5 h-40">
        <svg
          className="absolute inset-0 h-full w-full"
          viewBox={`0 0 ${VB_W} ${VB_H}`}
          preserveAspectRatio="none"
          aria-hidden
        >
          <defs>
            <linearGradient id="study-activity-area" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.28" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
            </linearGradient>
          </defs>
          {total > 0 && (
            <path
              d={areaPath}
              fill="url(#study-activity-area)"
              style={{
                opacity: mounted ? 1 : 0,
                transition: "opacity 600ms ease-out 200ms",
              }}
            />
          )}
          {total > 0 && (
            <line
              x1={0}
              y1={avgY}
              x2={VB_W}
              y2={avgY}
              stroke="var(--muted)"
              strokeWidth={1}
              strokeDasharray="4 4"
              opacity={0.4}
              vectorEffect="non-scaling-stroke"
            />
          )}
        </svg>

        {total > 0 && (
          <span
            className="pointer-events-none absolute right-0 -translate-y-1/2 rounded-sm bg-surface px-1 text-[10px] font-medium text-muted tabular-nums"
            style={{ top: `${avgTopPct}%` }}
          >
            평균 {avgDisplay}
          </span>
        )}

        <div className="absolute inset-0 flex items-end gap-1.5">
          {enriched.map((day, i) => {
            const isActive = hoveredIdx === i;
            const dim = hoveredIdx !== null && !isActive;
            const barHeight = mounted ? day.heightPct : 0;
            return (
              <button
                type="button"
                key={day.date}
                onMouseEnter={() => setHoveredIdx(i)}
                onMouseLeave={() => setHoveredIdx(null)}
                onFocus={() => setHoveredIdx(i)}
                onBlur={() => setHoveredIdx(null)}
                className="group relative flex h-full flex-1 cursor-pointer flex-col items-center justify-end rounded-md outline-none focus-visible:ring-2 focus-visible:ring-primary"
                aria-label={`${day.month}월 ${day.dayNum}일 ${DOW_LONG[day.dow]}, ${day.count}문제`}
              >
                {day.count > 0 ? (
                  <div
                    className={`w-full max-w-[22px] origin-bottom rounded-t-md ${
                      day.isToday
                        ? "bg-primary shadow-[0_0_12px_var(--glow)]"
                        : isActive
                          ? "bg-primary/80"
                          : dim
                            ? "bg-primary/25"
                            : "bg-primary/55"
                    }`}
                    style={{
                      height: `${barHeight}%`,
                      transition: `height 600ms cubic-bezier(0.22, 1, 0.36, 1) ${i * 30}ms, background-color 200ms ease`,
                    }}
                  />
                ) : (
                  <div className="mb-1 h-1 w-1 rounded-full bg-border" />
                )}
              </button>
            );
          })}
        </div>

        {hovered && (
          <div
            className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg border border-border bg-background px-2.5 py-1.5 text-xs shadow-lg"
            style={{
              left: `${(hovered.centerX / VB_W) * 100}%`,
              top: `${(hovered.y / VB_H) * 100}%`,
              marginTop: "-8px",
            }}
          >
            <p className="font-semibold tabular-nums">{hovered.count}문제</p>
            <p className="text-[10px] text-muted tabular-nums">
              {hovered.month}/{hovered.dayNum} ({DOW_SHORT[hovered.dow]})
            </p>
          </div>
        )}
      </div>

      <div className="mt-2 flex gap-1.5">
        {enriched.map((day) => (
          <div
            key={`label-${day.date}`}
            className="flex flex-1 flex-col items-center gap-0.5"
          >
            <span
              className={`text-[10px] tabular-nums ${
                day.isToday
                  ? "font-bold text-primary"
                  : day.isWeekend
                    ? "text-accent/70"
                    : "text-muted/60"
              }`}
            >
              {day.month}/{day.dayNum}
            </span>
            <span
              className={`text-[9px] ${
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
        ))}
      </div>
    </div>
  );
}
