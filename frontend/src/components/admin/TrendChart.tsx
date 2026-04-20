"use client";

import { useEffect, useId, useState } from "react";
import { getTrend, type AdminTrendPoint } from "@/lib/adminApi";

const PERIOD_OPTIONS = [
  { days: 7, label: "7일" },
  { days: 14, label: "14일" },
  { days: 30, label: "30일" },
];

type Field = "newMembers" | "newSolves";

export default function TrendChart() {
  const [days, setDays] = useState(7);
  const [points, setPoints] = useState<AdminTrendPoint[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getTrend(days)
      .then((r) => setPoints(r.points))
      .catch((e) => {
        setPoints([]);
        setError(e instanceof Error ? e.message : "불러오기 실패");
      })
      .finally(() => setLoading(false));
  }, [days]);

  return (
    <div className="rounded-xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold">회원/풀이 추이</h2>
        <div className="flex items-center gap-1 rounded-md border border-border bg-background p-0.5">
          {PERIOD_OPTIONS.map((opt) => (
            <button
              key={opt.days}
              onClick={() => setDays(opt.days)}
              className={`rounded px-2 py-0.5 text-[11px] font-medium transition ${
                days === opt.days
                  ? "bg-primary text-zinc-900"
                  : "text-muted hover:text-foreground"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <p className="mt-3 rounded-md border border-rose-500/30 bg-rose-500/10 px-2.5 py-1.5 text-[11px] text-rose-300">
          데이터를 불러올 수 없습니다: {error}
        </p>
      )}

      {loading && !points ? (
        <div className="mt-3 h-24 animate-pulse rounded-lg bg-background" />
      ) : points && points.length > 0 ? (
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <LineSeries
            title="신규 가입"
            points={points}
            field="newMembers"
            color="#a78bfa"
          />
          <LineSeries
            title="풀이"
            points={points}
            field="newSolves"
            color="#4ade80"
          />
        </div>
      ) : (
        <p className="mt-3 text-xs text-muted">데이터가 없습니다.</p>
      )}
    </div>
  );
}

const W = 400;
const H = 120;
const PAD_L = 34;
const PAD_R = 8;
const PAD_T = 12;
const PAD_B = 20;

function LineSeries({
  title,
  points,
  field,
  color,
}: {
  title: string;
  points: AdminTrendPoint[];
  field: Field;
  color: string;
}) {
  const gradId = useId();
  const values = points.map((p) => p[field]);
  const rawMax = Math.max(0, ...values);
  const max = niceMax(rawMax);
  const total = values.reduce((s, v) => s + v, 0);
  const avg = total / points.length;

  const plotW = W - PAD_L - PAD_R;
  const plotH = H - PAD_T - PAD_B;
  const n = points.length;
  const xStep = n > 1 ? plotW / (n - 1) : 0;

  const coords = points.map((p, i) => {
    const v = p[field];
    return {
      x: PAD_L + (n > 1 ? i * xStep : plotW / 2),
      y: PAD_T + plotH - (max === 0 ? 0 : (v / max) * plotH),
      v,
      date: p.date,
    };
  });

  const linePath = buildSmoothPath(coords);
  const areaPath =
    linePath +
    ` L ${coords[coords.length - 1].x} ${PAD_T + plotH}` +
    ` L ${coords[0].x} ${PAD_T + plotH} Z`;

  const yTicks = [0, max * 0.25, max * 0.5, max * 0.75, max];
  const labelEvery = n <= 7 ? 1 : n <= 14 ? 2 : 5;

  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between">
        <div className="flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full" style={{ background: color }} />
          <span className="text-xs font-semibold text-foreground">{title}</span>
        </div>
        <span className="text-[11px] text-muted">
          합계 <span className="font-medium text-foreground">{total.toLocaleString()}</span>
          <span className="mx-1 text-border">·</span>
          일평균 <span className="font-medium text-foreground">{avg.toFixed(1)}</span>
        </span>
      </div>

      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label={`${title} 일별 추이`}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={0.32} />
            <stop offset="100%" stopColor={color} stopOpacity={0} />
          </linearGradient>
        </defs>

        {/* y축 그리드 + 틱 라벨 */}
        {yTicks.map((tv, i) => {
          const y = PAD_T + plotH - (tv / max) * plotH;
          return (
            <g key={i}>
              <line
                x1={PAD_L}
                x2={W - PAD_R}
                y1={y}
                y2={y}
                stroke="currentColor"
                className="text-border"
                strokeWidth={1}
                strokeDasharray={i === 0 ? "" : "2 4"}
                opacity={i === 0 ? 1 : 0.6}
              />
              <text
                x={PAD_L - 6}
                y={y + 3}
                textAnchor="end"
                fontSize={10}
                className="fill-current text-muted"
              >
                {formatTick(tv)}
              </text>
            </g>
          );
        })}

        {/* 영역 fill */}
        <path d={areaPath} fill={`url(#${gradId})`} />

        {/* 라인 */}
        <path
          d={linePath}
          fill="none"
          stroke={color}
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* 데이터 포인트 */}
        {coords.map((c) => (
          <g key={c.date}>
            <circle
              cx={c.x}
              cy={c.y}
              r={3}
              fill="var(--color-surface, #18181b)"
              stroke={color}
              strokeWidth={1.75}
            >
              <title>
                {c.date}: {c.v.toLocaleString()}
              </title>
            </circle>
          </g>
        ))}

        {/* x축 날짜 라벨 */}
        {points.map((p, i) => {
          const show = i % labelEvery === 0 || i === n - 1;
          if (!show) return null;
          const cx = coords[i].x;
          return (
            <text
              key={p.date}
              x={cx}
              y={H - 8}
              textAnchor="middle"
              fontSize={10}
              className="fill-current text-muted"
            >
              {formatDate(p.date)}
            </text>
          );
        })}

        {total === 0 && (
          <text
            x={W / 2}
            y={H / 2}
            textAnchor="middle"
            fontSize={12}
            className="fill-current text-muted"
          >
            해당 기간 활동이 없습니다
          </text>
        )}
      </svg>
    </div>
  );
}

/** Catmull-Rom → Cubic Bezier 변환으로 자연스러운 스무딩 곡선 생성. */
function buildSmoothPath(coords: { x: number; y: number }[]): string {
  if (coords.length === 0) return "";
  if (coords.length === 1) {
    const { x, y } = coords[0];
    return `M ${x} ${y}`;
  }
  const tension = 0.2;
  let d = `M ${coords[0].x} ${coords[0].y}`;
  for (let i = 0; i < coords.length - 1; i++) {
    const p0 = coords[i - 1] ?? coords[i];
    const p1 = coords[i];
    const p2 = coords[i + 1];
    const p3 = coords[i + 2] ?? p2;
    const cp1x = p1.x + (p2.x - p0.x) * tension;
    const cp1y = p1.y + (p2.y - p0.y) * tension;
    const cp2x = p2.x - (p3.x - p1.x) * tension;
    const cp2y = p2.y - (p3.y - p1.y) * tension;
    d += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
  }
  return d;
}

function niceMax(v: number): number {
  if (v <= 0) return 4;
  const pow = Math.pow(10, Math.floor(Math.log10(v)));
  const n = v / pow;
  let step;
  if (n <= 1) step = 1;
  else if (n <= 2) step = 2;
  else if (n <= 5) step = 5;
  else step = 10;
  return step * pow;
}

function formatTick(v: number): string {
  if (v >= 1000) return `${(v / 1000).toFixed(v % 1000 === 0 ? 0 : 1)}k`;
  return v.toLocaleString(undefined, { maximumFractionDigits: 0 });
}

function formatDate(iso: string): string {
  const d = new Date(iso + "T00:00:00");
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
