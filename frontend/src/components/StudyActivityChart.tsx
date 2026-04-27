"use client";

import { useEffect, useState } from "react";

type DayData = { date: string; count: number };

type StudyActivityChartProps = {
  data: DayData[];
  /** 전체 사용자 14일 일평균. 제공 시 비교용 점선을 추가 표시. */
  overallAvg?: number;
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

export default function StudyActivityChart({ data, overallAvg }: StudyActivityChartProps) {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const t = requestAnimationFrame(() => setMounted(true));
    return () => cancelAnimationFrame(t);
  }, []);

  const todayStr = toLocalDateStr(new Date());
  const total = data.reduce((s, d) => s + d.count, 0);
  const avg = total / data.length;
  const hasOverall = typeof overallAvg === "number" && overallAvg > 0;
  // 전체 평균선이 차트 바깥으로 튀지 않도록 max에 포함
  const max = Math.max(...data.map((d) => d.count), hasOverall ? overallAvg! : 0, 1);

  let maxIdx = 0;
  data.forEach((d, i) => {
    if (d.count > data[maxIdx].count) maxIdx = i;
  });
  const hasMax = data[maxIdx].count > 0;
  const fmtAvg = (v: number) => (v >= 10 ? Math.round(v).toString() : v.toFixed(1));
  const avgDisplay = fmtAvg(avg);
  const overallDisplay = hasOverall ? fmtAvg(overallAvg!) : null;

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
  const overallY = hasOverall ? yFor(overallAvg!, max) : 0;
  const overallTopPct = hasOverall ? (overallY / VB_H) * 100 : 0;
  // 두 라벨이 너무 가까우면 충돌하니 세로 위치 살짝 어긋나게
  const labelsOverlap = hasOverall && Math.abs(avgTopPct - overallTopPct) < 12;

  const hovered = hoveredIdx !== null ? enriched[hoveredIdx] : null;

  return (
    <div
      className="mt-6 rounded-xl border border-border bg-surface p-5"
      role="img"
      aria-label={`최근 2주 학습량 차트. 총 ${total}문제, 일평균 ${avgDisplay}문제`}
    >
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-sm font-semibold">최근 2주 학습량</h2>
        <div className="flex items-center gap-2.5 text-[10px] text-muted tabular-nums">
          <span>
            총 <span className="font-semibold text-foreground">{total}</span>
          </span>
          <span className="h-2.5 w-px bg-border" aria-hidden />
          <span className="inline-flex items-center gap-1">
            <span className="inline-block h-0.5 w-2.5 border-t border-dashed border-muted" aria-hidden />
            내 평균 <span className="font-semibold text-foreground">{avgDisplay}</span>
          </span>
          {hasOverall && (
            <>
              <span className="h-2.5 w-px bg-border" aria-hidden />
              <span className="inline-flex items-center gap-1">
                <span className="inline-block h-0.5 w-2.5 border-t border-dashed border-accent" aria-hidden />
                전체 평균 <span className="font-semibold text-accent">{overallDisplay}</span>
              </span>
            </>
          )}
          {hasMax && (
            <>
              <span className="h-2.5 w-px bg-border" aria-hidden />
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
          {hasOverall && (
            <line
              x1={0}
              y1={overallY}
              x2={VB_W}
              y2={overallY}
              stroke="var(--accent)"
              strokeWidth={1}
              strokeDasharray="4 4"
              opacity={0.6}
              vectorEffect="non-scaling-stroke"
            />
          )}
        </svg>

        {total > 0 && (
          <span
            className="pointer-events-none absolute -translate-y-1/2 rounded-sm bg-surface px-1 text-[9px] font-medium text-muted tabular-nums"
            style={{
              top: `${avgTopPct}%`,
              right: labelsOverlap && avgY > overallY ? "2.5rem" : "0",
            }}
          >
            내 {avgDisplay}
          </span>
        )}

        {hasOverall && (
          <span
            className="pointer-events-none absolute -translate-y-1/2 rounded-sm bg-surface px-1 text-[9px] font-medium text-accent tabular-nums"
            style={{
              top: `${overallTopPct}%`,
              right: labelsOverlap && overallY >= avgY ? "2.5rem" : "0",
            }}
          >
            전체 {overallDisplay}
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
                className={`group relative flex h-full flex-1 cursor-pointer flex-col items-center justify-end rounded-md outline-none transition-colors duration-100 focus-visible:ring-2 focus-visible:ring-primary ${
                  isActive ? "bg-primary/[0.06]" : "hover:bg-primary/[0.04]"
                }`}
                aria-label={`${day.month}월 ${day.dayNum}일 ${DOW_LONG[day.dow]}, ${day.count}문제`}
              >
                {day.count > 0 ? (
                  <div
                    className={`w-full max-w-[24px] origin-bottom rounded-t-md ${
                      day.isToday
                        ? "bg-primary shadow-[0_0_12px_var(--glow)]"
                        : isActive
                          ? "bg-primary shadow-[0_0_10px_var(--glow)] scale-y-105"
                          : dim
                            ? "bg-primary/25"
                            : "bg-primary/55"
                    }`}
                    style={{
                      height: `${barHeight}%`,
                      transition: `height 450ms cubic-bezier(0.22, 1, 0.36, 1) ${i * 20}ms, background-color 100ms ease, transform 120ms ease`,
                    }}
                  />
                ) : (
                  <div className={`mb-1 h-1.5 w-1.5 rounded-full transition-colors duration-100 ${
                    isActive ? "bg-primary" : "bg-border"
                  }`} />
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
              className={`text-[9px] tabular-nums ${
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
        ))}
      </div>
    </div>
  );
}
