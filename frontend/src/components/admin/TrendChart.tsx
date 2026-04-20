"use client";

import { useEffect, useState } from "react";
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
    <div className="rounded-xl border border-border bg-surface p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-base font-semibold">회원/풀이 추이</h2>
        <div className="flex items-center gap-1 rounded-lg border border-border bg-background p-0.5">
          {PERIOD_OPTIONS.map((opt) => (
            <button
              key={opt.days}
              onClick={() => setDays(opt.days)}
              className={`rounded-md px-3 py-1 text-xs font-medium transition ${
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
        <p className="mt-4 rounded-md border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-300">
          데이터를 불러올 수 없습니다: {error}
        </p>
      )}

      {loading && !points ? (
        <div className="mt-6 h-56 animate-pulse rounded-lg bg-background" />
      ) : points && points.length > 0 ? (
        <div className="mt-6 space-y-6">
          <SvgBarChart
            title="신규 가입"
            points={points}
            field="newMembers"
            barFill="#a78bfa"
          />
          <SvgBarChart
            title="풀이"
            points={points}
            field="newSolves"
            barFill="#4ade80"
          />
        </div>
      ) : (
        <p className="mt-6 text-sm text-muted">데이터가 없습니다.</p>
      )}
    </div>
  );
}

const W = 720;
const H = 180;
const PAD_L = 40;
const PAD_R = 12;
const PAD_T = 12;
const PAD_B = 28;

function SvgBarChart({
  title,
  points,
  field,
  barFill,
}: {
  title: string;
  points: AdminTrendPoint[];
  field: Field;
  barFill: string;
}) {
  const values = points.map((p) => p[field]);
  const rawMax = Math.max(0, ...values);
  const max = niceMax(rawMax);
  const total = values.reduce((s, v) => s + v, 0);
  const avg = total / points.length;

  const plotW = W - PAD_L - PAD_R;
  const plotH = H - PAD_T - PAD_B;
  const n = points.length;
  const slot = plotW / n;
  const barW = Math.max(2, slot * 0.7);
  const yTicks = [0, max * 0.25, max * 0.5, max * 0.75, max];

  const labelEvery = n <= 7 ? 1 : n <= 14 ? 2 : 5;

  return (
    <div>
      <div className="mb-2 flex items-baseline justify-between">
        <div className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full" style={{ background: barFill }} />
          <span className="text-xs font-medium text-foreground">{title}</span>
        </div>
        <span className="text-[11px] text-muted">
          기간 합계 {total.toLocaleString()} · 일평균 {avg.toFixed(1)}
        </span>
      </div>

      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label={`${title} 일별 추이`}
      >
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
                strokeDasharray={i === 0 ? "" : "2 3"}
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

        {/* 막대 */}
        {points.map((p, i) => {
          const v = p[field];
          const h = max === 0 ? 0 : (v / max) * plotH;
          const x = PAD_L + i * slot + (slot - barW) / 2;
          const y = PAD_T + plotH - h;
          return (
            <g key={p.date}>
              <rect
                x={x}
                y={y}
                width={barW}
                height={Math.max(h, v > 0 ? 2 : 0)}
                rx={2}
                fill={barFill}
                opacity={v === 0 ? 0 : 1}
              >
                <title>
                  {p.date}: {v.toLocaleString()}
                </title>
              </rect>
            </g>
          );
        })}

        {/* x축 라벨 */}
        {points.map((p, i) => {
          const show = i % labelEvery === 0 || i === n - 1;
          if (!show) return null;
          const cx = PAD_L + i * slot + slot / 2;
          return (
            <text
              key={p.date}
              x={cx}
              y={H - 10}
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
            fontSize={11}
            className="fill-current text-muted"
          >
            해당 기간 활동이 없습니다
          </text>
        )}
      </svg>
    </div>
  );
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
