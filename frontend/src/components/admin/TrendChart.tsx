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
        <div className="mt-6 h-48 animate-pulse rounded-lg bg-background" />
      ) : points && points.length > 0 ? (
        <div className="mt-6 space-y-6">
          <BarSeries
            title="신규 가입"
            points={points}
            field="newMembers"
            barColor="bg-violet-400"
            dotColor="bg-violet-400"
          />
          <BarSeries
            title="풀이"
            points={points}
            field="newSolves"
            barColor="bg-green-400"
            dotColor="bg-green-400"
          />
        </div>
      ) : (
        <p className="mt-6 text-sm text-muted">데이터가 없습니다.</p>
      )}
    </div>
  );
}

function BarSeries({
  title,
  points,
  field,
  barColor,
  dotColor,
}: {
  title: string;
  points: AdminTrendPoint[];
  field: Field;
  barColor: string;
  dotColor: string;
}) {
  const values = points.map((p) => p[field]);
  const max = Math.max(1, ...values);
  const total = values.reduce((s, v) => s + v, 0);
  const avg = total / points.length;
  const labelEvery = points.length <= 7 ? 1 : points.length <= 14 ? 2 : 5;
  const allZero = total === 0;

  return (
    <div>
      <div className="mb-2 flex items-baseline justify-between">
        <div className="flex items-center gap-2">
          <span className={`h-2 w-2 rounded-full ${dotColor}`} />
          <span className="text-xs font-medium text-foreground">{title}</span>
        </div>
        <span className="text-[11px] text-muted">
          기간 합계 {total.toLocaleString()} · 일평균 {avg.toFixed(1)}
        </span>
      </div>

      <div className="relative">
        {/* 기준선 (0 baseline) */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-px bg-border" />
        <div className="pointer-events-none absolute inset-x-0 bottom-1/2 h-px bg-border/40" />

        <div className="flex h-28 items-end gap-1">
          {points.map((p) => {
            const v = p[field];
            const pct = (v / max) * 100;
            return (
              <div
                key={p.date}
                className="group relative flex flex-1 flex-col items-center justify-end"
                title={`${p.date}: ${v.toLocaleString()}`}
              >
                <span className="mb-1 text-[9px] tabular-nums text-muted opacity-0 transition-opacity group-hover:opacity-100">
                  {v.toLocaleString()}
                </span>
                <div
                  className={`w-full min-h-[2px] rounded-sm ${barColor} transition-opacity ${
                    v === 0 ? "opacity-20" : "group-hover:opacity-80"
                  }`}
                  style={{ height: `${Math.max(pct, v > 0 ? 4 : 2)}%` }}
                />
              </div>
            );
          })}
        </div>

        {allZero && (
          <p className="absolute inset-0 flex items-center justify-center text-[11px] text-muted">
            해당 기간 활동이 없습니다
          </p>
        )}
      </div>

      <div className="mt-1.5 flex gap-1">
        {points.map((p, i) => (
          <div
            key={p.date}
            className="flex-1 text-center text-[10px] tabular-nums text-muted"
          >
            {i % labelEvery === 0 || i === points.length - 1 ? formatDate(p.date) : ""}
          </div>
        ))}
      </div>
    </div>
  );
}

function formatDate(iso: string): string {
  const d = new Date(iso + "T00:00:00");
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
