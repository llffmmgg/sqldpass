"use client";

import { useEffect, useMemo, useState } from "react";

type DayData = { date: string; total: number; correct: number; wrong: number };

type Period = 7 | 14 | 30;

type StudyActivityChartProps = {
  /** 최근 30일치 일별 풀이 데이터 (오래된 → 최신 순). 기간 토글로 슬라이스. */
  data: DayData[];
  /** 전체 사용자 일평균 — 기준선 점선 */
  overallAvg?: number;
};

const VB_W = 600;
const VB_H = 200;
const PADDING_LEFT = 12;     // y축이 우측으로 이동했으므로 좌측 패딩 축소
const PADDING_RIGHT = 36;    // 우측 y축 라벨 공간 확보
const PADDING_TOP = 16;
const PADDING_BOTTOM = 28;
const PLOT_W = VB_W - PADDING_LEFT - PADDING_RIGHT;
const PLOT_H = VB_H - PADDING_TOP - PADDING_BOTTOM;

const DOW_SHORT = ["일", "월", "화", "수", "목", "금", "토"];

function niceCeil(v: number): number {
  if (v <= 5) return 5;
  if (v <= 10) return 10;
  if (v <= 20) return 20;
  if (v <= 50) return 50;
  if (v <= 100) return 100;
  return Math.ceil(v / 50) * 50;
}

/** Catmull-Rom 효과 — midpoint cubic bezier. Supabase 대시보드 톤 부드러운 곡선. */
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

function useCountUp(target: number, durationMs = 800): number {
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

export default function StudyActivityChart({ data, overallAvg }: StudyActivityChartProps) {
  const [period, setPeriod] = useState<Period>(14);
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);

  const sliced = useMemo(() => data.slice(-period), [data, period]);

  // 이전 기간 비교용 — period 두 배의 history 가 있을 때만. 30일 모드는 현재 30일치만이라 prev 없음.
  const prevSliced = useMemo(() => {
    const start = data.length - period * 2;
    const end = data.length - period;
    if (start < 0) return [];
    return data.slice(start, end);
  }, [data, period]);
  const hasPrev = prevSliced.length === period;

  const totalSum = sliced.reduce((s, d) => s + d.total, 0);
  const correctSum = sliced.reduce((s, d) => s + d.correct, 0);
  const wrongSum = totalSum - correctSum;
  const accuracy = totalSum > 0 ? Math.round((correctSum / totalSum) * 100) : null;
  const totalAnimated = useCountUp(totalSum);

  const streak = computeStreak(sliced);
  // 전체 30일 데이터 기준 (period 무관하게 안정적). period >= 14 일 때만 표시.
  const weekDelta = computeWeekDelta(data);

  const hasOverall = typeof overallAvg === "number" && overallAvg > 0;
  const prevMax = hasPrev ? Math.max(...prevSliced.map((d) => d.total), 0) : 0;
  const dataMax = Math.max(
    ...sliced.map((d) => d.total),
    hasOverall ? overallAvg! : 0,
    prevMax,
    1,
  );
  const max = niceCeil(dataMax);

  const colW = PLOT_W / sliced.length;
  const baseY = PADDING_TOP + PLOT_H;

  const yTicks = [0, max / 2, max].map((v) => ({
    value: Math.round(v),
    y: PADDING_TOP + (1 - v / max) * PLOT_H,
  }));

  // x 라벨 간격 — 7일은 모두, 14일은 격일, 30일은 5일 간격 + 마지막 항상
  const labelInterval = sliced.length <= 7 ? 1 : sliced.length <= 14 ? 2 : 5;

  const deltaPositive = weekDelta != null && weekDelta > 0;
  const deltaNegative = weekDelta != null && weekDelta < 0;
  const hovered = hoveredIdx !== null ? sliced[hoveredIdx] : null;

  return (
    <section
      className="mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5"
      role="img"
      aria-label={`최근 ${period}일 학습량. 총 ${totalSum}문제, 정답 ${correctSum}, 오답 ${wrongSum}`}
    >
      {/* 헤더 */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xs font-medium uppercase tracking-wider text-text-subtle">
            최근 {period}일 학습량
          </h2>
          <div className="mt-1.5 flex items-baseline gap-2">
            <span className="text-3xl font-bold tabular-nums text-text">
              {totalAnimated}
            </span>
            <span className="text-xs text-text-muted">문제</span>
            {accuracy !== null && (
              <span className="ml-1 text-xs text-text-muted tabular-nums">
                · 정답률 <span className="font-semibold text-text">{accuracy}%</span>
              </span>
            )}
          </div>
          <div className="mt-2 flex items-center gap-3 text-[11px]">
            {streak > 0 && (
              <span className="inline-flex items-center gap-1 text-text-muted">
                <span aria-hidden>🔥</span>
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

        {/* 기간 토글 — Supabase segmented control */}
        <div
          role="tablist"
          aria-label="기간 선택"
          className="inline-flex items-center rounded-sm border border-border bg-bg-elevated p-0.5 text-xs"
        >
          {([7, 14, 30] as Period[]).map((p) => (
            <button
              key={p}
              type="button"
              role="tab"
              aria-selected={period === p}
              onClick={() => setPeriod(p)}
              className={`rounded-sm px-2.5 py-1 font-medium transition-colors ${
                period === p
                  ? "bg-surface text-text shadow-sm"
                  : "text-text-subtle hover:text-text"
              }`}
            >
              {p}일
            </button>
          ))}
        </div>
      </div>

      {/* 차트 + 호버 */}
      <div className="relative mt-4">
        <svg
          viewBox={`0 0 ${VB_W} ${VB_H}`}
          className="w-full"
          preserveAspectRatio="none"
        >
          {/* y grid + label — Google Analytics 식 우측 정렬 */}
          {yTicks.map((t, i) => (
            <g key={i}>
              <line
                x1={PADDING_LEFT}
                x2={PADDING_LEFT + PLOT_W}
                y1={t.y}
                y2={t.y}
                stroke="var(--border)"
                strokeWidth={1}
                strokeDasharray={t.value === 0 ? "0" : "2 3"}
                opacity={t.value === 0 ? 0.9 : 0.5}
              />
              <text
                x={PADDING_LEFT + PLOT_W + 6}
                y={t.y + 3}
                textAnchor="start"
                fill="var(--text-subtle)"
                fontSize="10"
              >
                {t.value}
              </text>
            </g>
          ))}

          {/* 전체 평균 점선 */}
          {hasOverall && overallAvg! <= max && (
            <g>
              <line
                x1={PADDING_LEFT}
                x2={PADDING_LEFT + PLOT_W}
                y1={PADDING_TOP + (1 - overallAvg! / max) * PLOT_H}
                y2={PADDING_TOP + (1 - overallAvg! / max) * PLOT_H}
                stroke="var(--primary)"
                strokeWidth={1}
                strokeDasharray="3 3"
                opacity={0.55}
              />
              <text
                x={PADDING_LEFT + PLOT_W - 4}
                y={PADDING_TOP + (1 - overallAvg! / max) * PLOT_H - 4}
                textAnchor="end"
                fill="var(--primary)"
                fontSize="9"
                opacity={0.75}
              >
                평균
              </text>
            </g>
          )}

          {/* gradient def — area fill 용 (Supabase 시그니처 emerald glow) */}
          <defs>
            <linearGradient id="totalAreaGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--primary)" stopOpacity="0.28" />
              <stop offset="100%" stopColor="var(--primary)" stopOpacity="0" />
            </linearGradient>
          </defs>

          {/* 이전 기간 라인 — dashed, 옅은 톤. period 두 배 history 있을 때만 (7d/14d) */}
          {hasPrev && (() => {
            const prevPoints = prevSliced.map((d, i) => ({
              x: PADDING_LEFT + i * colW + colW / 2,
              y: baseY - (d.total / max) * PLOT_H,
            }));
            const prevLinePath = buildLinePath(prevPoints);
            return (
              <path
                d={prevLinePath}
                fill="none"
                stroke="var(--primary)"
                strokeWidth={1.5}
                strokeDasharray="4 4"
                strokeLinecap="round"
                strokeLinejoin="round"
                opacity={0.4}
              />
            );
          })()}

          {/* area fill + line — 일별 총 풀이수 단일 metric */}
          {(() => {
            const points = sliced.map((d, i) => ({
              x: PADDING_LEFT + i * colW + colW / 2,
              y: baseY - (d.total / max) * PLOT_H,
            }));
            const areaPath = buildAreaPath(points, baseY);
            const linePath = buildLinePath(points);
            return (
              <g>
                <path d={areaPath} fill="url(#totalAreaGradient)" />
                <path
                  d={linePath}
                  fill="none"
                  stroke="var(--primary)"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </g>
            );
          })()}

          {/* hit area + 호버 dot */}
          {sliced.map((d, i) => {
            const cx = PADDING_LEFT + i * colW + colW / 2;
            const cy = baseY - (d.total / max) * PLOT_H;
            const isHovered = hoveredIdx === i;
            return (
              <g
                key={d.date}
                onMouseEnter={() => setHoveredIdx(i)}
                onMouseLeave={() => setHoveredIdx((prev) => (prev === i ? null : prev))}
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

          {/* x 라벨 */}
          {sliced.map((d, i) => {
            if (i % labelInterval !== 0 && i !== sliced.length - 1) return null;
            const cx = PADDING_LEFT + i * colW + colW / 2;
            const dt = new Date(d.date);
            const label =
              sliced.length <= 7
                ? DOW_SHORT[dt.getDay()]
                : `${dt.getMonth() + 1}/${dt.getDate()}`;
            return (
              <text
                key={d.date}
                x={cx}
                y={PADDING_TOP + PLOT_H + 16}
                textAnchor="middle"
                fill="var(--text-subtle)"
                fontSize="10"
              >
                {label}
              </text>
            );
          })}
        </svg>

        {/* HTML 툴팁 — 풍부한 비교 카드. Google Analytics 식: 현재 + 이전 + 변화 */}
        {hovered && hoveredIdx !== null && (() => {
          const prevHovered = hasPrev ? prevSliced[hoveredIdx] : null;
          const delta = prevHovered ? hovered.total - prevHovered.total : null;
          const fmtDate = (s: string) =>
            new Date(s).toLocaleDateString("ko-KR", {
              month: "short",
              day: "numeric",
              weekday: "short",
            });
          return (
            <div
              className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-md border border-border bg-bg-elevated px-4 py-3 text-[11px] shadow-md min-w-[200px]"
              style={{
                left: `${((PADDING_LEFT + hoveredIdx * colW + colW / 2) / VB_W) * 100}%`,
                top: `${((baseY - (hovered.total / max) * PLOT_H - 8) / VB_H) * 100}%`,
              }}
            >
              <p className="font-semibold text-text">{fmtDate(hovered.date)}</p>

              <div className="mt-2 space-y-1.5">
                <div className="flex items-center justify-between gap-4">
                  <span className="inline-flex items-center gap-1.5 text-text-muted">
                    <span className="inline-block h-2 w-2 rounded-sm bg-primary" aria-hidden />
                    이번 기간
                  </span>
                  <span className="font-bold tabular-nums text-text">{hovered.total}</span>
                </div>

                {prevHovered && (
                  <div className="flex items-center justify-between gap-4">
                    <span className="inline-flex items-center gap-1.5 text-text-muted">
                      <span className="inline-block h-0.5 w-2 bg-primary opacity-50" aria-hidden />
                      이전 기간
                    </span>
                    <span className="font-medium tabular-nums text-text-muted">{prevHovered.total}</span>
                  </div>
                )}

                {delta !== null && delta !== 0 && (
                  <div className="flex items-center justify-between gap-4 border-t border-border pt-1.5">
                    <span className="text-text-muted">변화</span>
                    <span
                      className={`font-semibold tabular-nums ${delta > 0 ? "text-primary" : "text-danger"}`}
                    >
                      {delta > 0 ? "▲" : "▼"}{Math.abs(delta)}
                    </span>
                  </div>
                )}
              </div>

              <p className="mt-2 text-text-subtle">
                정답 {hovered.correct} · 오답 {hovered.wrong}
              </p>
            </div>
          );
        })()}
      </div>

      {/* 범례 — 이전 기간 비교 또는 전체 평균 점선 있을 때 표시 */}
      {(hasPrev || hasOverall) && (
        <div className="mt-3 flex items-center justify-end gap-4 text-[11px] text-text-muted">
          <span className="inline-flex items-center gap-1.5">
            <span className="inline-block h-0.5 w-3 bg-primary" aria-hidden />
            이번 기간
          </span>
          {hasPrev && (
            <span className="inline-flex items-center gap-1.5">
              <span
                className="inline-block h-0.5 w-3 bg-primary opacity-50"
                style={{ borderTop: "1px dashed currentColor" }}
                aria-hidden
              />
              이전 기간
            </span>
          )}
          {hasOverall && (
            <span className="inline-flex items-center gap-1.5">
              <span className="inline-block h-px w-3 bg-primary opacity-60" aria-hidden />
              전체 평균
            </span>
          )}
        </div>
      )}
    </section>
  );
}
