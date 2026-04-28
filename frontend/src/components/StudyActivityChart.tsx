"use client";

import { useEffect, useRef, useState } from "react";

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

/** 0 → target 까지 ease-out으로 ramp. target 변경되면 다시 시작. */
function useCountUp(target: number, durationMs = 900): number {
  const [value, setValue] = useState(0);
  const startedRef = useRef(false);
  useEffect(() => {
    startedRef.current = false;
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

/** 가장 최근 날짜부터 거꾸로 연속해서 풀이가 있는 일수. */
function computeStreak(data: DayData[]): number {
  let streak = 0;
  for (let i = data.length - 1; i >= 0; i--) {
    if (data[i].count > 0) streak++;
    else break;
  }
  return streak;
}

/** 전주(0..6) 대비 이번 주(7..13) 변동률 (%). 전주 0이면 +∞로 보지 않고 단순 +100. */
function computeWeekDelta(data: DayData[]): number | null {
  if (data.length < 14) return null;
  const prev = data.slice(0, 7).reduce((s, d) => s + d.count, 0);
  const cur = data.slice(7, 14).reduce((s, d) => s + d.count, 0);
  if (prev === 0 && cur === 0) return 0;
  if (prev === 0) return 100;
  return Math.round(((cur - prev) / prev) * 100);
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

  const totalAnimated = useCountUp(total);
  const streak = computeStreak(data);
  const weekDelta = computeWeekDelta(data);

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

  const todayIdx = enriched.findIndex((d) => d.isToday);
  const todayBar = todayIdx >= 0 ? enriched[todayIdx] : null;

  const areaPath = buildAreaPath(enriched.map((d) => ({ x: d.centerX, y: d.y })));
  const avgY = yFor(avg, max);
  const avgTopPct = (avgY / VB_H) * 100;
  const overallY = hasOverall ? yFor(overallAvg!, max) : 0;
  const overallTopPct = hasOverall ? (overallY / VB_H) * 100 : 0;
  const labelsOverlap = hasOverall && Math.abs(avgTopPct - overallTopPct) < 12;

  const hovered = hoveredIdx !== null ? enriched[hoveredIdx] : null;

  const deltaPositive = weekDelta != null && weekDelta > 0;
  const deltaNegative = weekDelta != null && weekDelta < 0;

  return (
    <div
      className="mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5"
      role="img"
      aria-label={`최근 2주 학습량 차트. 총 ${total}문제, 일평균 ${avgDisplay}문제`}
    >
      {/* ── 헤더: 큰 총합 + 변동률 chip / 우측 보조 메타 ─────────── */}
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
            {weekDelta != null && total > 0 && (
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
          </div>
        </div>
        <div className="flex items-center gap-2 text-[10px] text-muted tabular-nums">
          {streak > 0 && (
            <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold text-amber-600 dark:text-amber-300">
              <span aria-hidden>🔥</span>
              {streak}일 연속
            </span>
          )}
          <span className="inline-flex items-center gap-1">
            <span
              className="inline-block h-0.5 w-2.5 border-t border-dashed border-muted"
              aria-hidden
            />
            내 평균 <span className="font-semibold text-foreground">{avgDisplay}</span>
          </span>
          {hasOverall && (
            <span className="inline-flex items-center gap-1">
              <span
                className="inline-block h-0.5 w-2.5 border-t border-dashed border-accent"
                aria-hidden
              />
              전체 <span className="font-semibold text-accent">{overallDisplay}</span>
            </span>
          )}
          {hasMax && (
            <span>
              최다 <span className="font-semibold text-foreground">{data[maxIdx].count}</span>
            </span>
          )}
        </div>
      </div>

      {/* ── 차트 본체 ─────────────────────────────────────────── */}
      <div className="relative mt-5 h-44">
        <svg
          className="absolute inset-0 h-full w-full"
          viewBox={`0 0 ${VB_W} ${VB_H}`}
          preserveAspectRatio="none"
          aria-hidden
        >
          <defs>
            <linearGradient id="study-activity-area" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.32" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
            </linearGradient>
            <linearGradient id="study-activity-bar" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="1" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0.55" />
            </linearGradient>
            <linearGradient id="study-activity-bar-today" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="1" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0.85" />
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

          {/* 평균선 — dash flow 애니메이션 */}
          {total > 0 && (
            <line
              x1={0}
              y1={avgY}
              x2={VB_W}
              y2={avgY}
              stroke="var(--muted)"
              strokeWidth={1}
              strokeDasharray="6 5"
              opacity={0.5}
              vectorEffect="non-scaling-stroke"
              style={{
                animation: "study-dash-flow 1.6s linear infinite",
              }}
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
              strokeDasharray="6 5"
              opacity={0.65}
              vectorEffect="non-scaling-stroke"
              style={{
                animation: "study-dash-flow 1.6s linear infinite reverse",
              }}
            />
          )}
        </svg>

        {/* 평균선 인라인 라벨 */}
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

        {/* 막대 + 호버 영역 */}
        <div className="absolute inset-0 flex items-end gap-1.5">
          {enriched.map((day, i) => {
            const isActive = hoveredIdx === i;
            const isAdjacent =
              hoveredIdx !== null && Math.abs(hoveredIdx - i) === 1 && !isActive;
            const dim = hoveredIdx !== null && !isActive && !isAdjacent;
            const barHeight = mounted ? day.heightPct : 0;
            return (
              <button
                type="button"
                key={day.date}
                onMouseEnter={() => setHoveredIdx(i)}
                onMouseLeave={() => setHoveredIdx(null)}
                onFocus={() => setHoveredIdx(i)}
                onBlur={() => setHoveredIdx(null)}
                className={`group relative flex h-full flex-1 cursor-pointer flex-col items-center justify-end rounded-md outline-none transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-primary ${
                  isActive ? "bg-primary/[0.08]" : "hover:bg-primary/[0.05]"
                }`}
                aria-label={`${day.month}월 ${day.dayNum}일 ${DOW_LONG[day.dow]}, ${day.count}문제`}
              >
                {/* 오늘 막대 위 불꽃 + ring pulse */}
                {day.isToday && day.count > 0 && (
                  <span
                    className="pointer-events-none absolute z-[1] -translate-y-1 text-[11px] leading-none"
                    style={{
                      bottom: `calc(${barHeight}% + 4px)`,
                      animation: "study-flame-bob 1.6s ease-in-out infinite",
                    }}
                    aria-hidden
                  >
                    🔥
                  </span>
                )}
                {day.count > 0 ? (
                  <div
                    className={`relative w-full max-w-[26px] origin-bottom overflow-hidden rounded-full ${
                      day.isToday
                        ? "bg-[linear-gradient(180deg,var(--primary)_0%,var(--primary)_100%)] shadow-[0_0_18px_var(--glow)]"
                        : isActive
                          ? "bg-primary scale-y-[1.06] shadow-[0_0_14px_var(--glow)]"
                          : isAdjacent
                            ? "bg-primary/75 scale-y-[1.02]"
                            : dim
                              ? "bg-primary/25"
                              : "bg-[linear-gradient(180deg,var(--primary)_0%,var(--primary)_55%,transparent_120%)] opacity-90"
                    }`}
                    style={{
                      height: `${barHeight}%`,
                      transition: `height 520ms cubic-bezier(0.22, 1, 0.36, 1) ${i * 28}ms, background-color 140ms ease, transform 160ms ease, opacity 140ms ease`,
                    }}
                  >
                    {day.isToday && (
                      <span
                        className="pointer-events-none absolute inset-x-0 top-0 h-full rounded-full ring-2 ring-primary/55"
                        style={{ animation: "study-ring-pulse 1.8s ease-out infinite" }}
                        aria-hidden
                      />
                    )}
                  </div>
                ) : (
                  <div
                    className={`mb-1 h-1.5 w-1.5 rounded-full transition-colors duration-150 ${
                      isActive ? "bg-primary" : "bg-border"
                    }`}
                  />
                )}
              </button>
            );
          })}
        </div>

        {/* 호버 툴팁 */}
        {hovered && (
          <div
            className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg border border-border bg-background px-2.5 py-1.5 text-xs shadow-lg"
            style={{
              left: `${(hovered.centerX / VB_W) * 100}%`,
              top: `${(hovered.y / VB_H) * 100}%`,
              marginTop: "-12px",
            }}
          >
            <p className="font-semibold tabular-nums">{hovered.count}문제</p>
            <p className="text-[10px] text-muted tabular-nums">
              {hovered.month}/{hovered.dayNum} ({DOW_SHORT[hovered.dow]})
            </p>
          </div>
        )}

        {/* 오늘 라벨 (정적, 항상 표시) — 호버 중엔 숨김 */}
        {todayBar && todayBar.count > 0 && hoveredIdx === null && (
          <div
            className="pointer-events-none absolute z-[1] -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-md bg-primary px-1.5 py-0.5 text-[9px] font-bold text-white shadow-md"
            style={{
              left: `${(todayBar.centerX / VB_W) * 100}%`,
              top: `${(todayBar.y / VB_H) * 100}%`,
              marginTop: "-22px",
              animation: "study-today-bob 2s ease-in-out infinite",
            }}
          >
            오늘 {todayBar.count}
          </div>
        )}
      </div>

      {/* ── X축 라벨 ───────────────────────────────────────── */}
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

      {/* keyframes — 컴포넌트 스코프 내 정의 (Tailwind 미설정 키프레임) */}
      <style jsx>{`
        @keyframes study-dash-flow {
          to {
            stroke-dashoffset: -22;
          }
        }
        @keyframes study-ring-pulse {
          0% {
            box-shadow: 0 0 0 0 rgba(36, 180, 126, 0.55);
            opacity: 0.9;
          }
          70% {
            box-shadow: 0 0 0 10px rgba(36, 180, 126, 0);
            opacity: 0.2;
          }
          100% {
            box-shadow: 0 0 0 0 rgba(36, 180, 126, 0);
            opacity: 0;
          }
        }
        @keyframes study-flame-bob {
          0%,
          100% {
            transform: translateY(-2px) scale(1);
          }
          50% {
            transform: translateY(-5px) scale(1.08);
          }
        }
        @keyframes study-today-bob {
          0%,
          100% {
            transform: translate(-50%, 0);
          }
          50% {
            transform: translate(-50%, -3px);
          }
        }
      `}</style>
    </div>
  );
}
