"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 stats fetch 후 setState */

import { useEffect, useState } from "react";

import {
  fetchRevenueByPlan,
  fetchRevenueStats,
  type RevenueByPlan,
  type RevenuePoint,
} from "@/lib/adminApi";
import { PLAN_TOKENS, type SubscriptionPlanKey } from "@/lib/plan-tokens";

const RANGES = [7, 30, 90] as const;
type Range = (typeof RANGES)[number];

export default function SubscriptionStatsPanel() {
  const [days, setDays] = useState<Range>(30);
  const [points, setPoints] = useState<RevenuePoint[]>([]);
  const [byPlan, setByPlan] = useState<RevenueByPlan[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([fetchRevenueStats(days), fetchRevenueByPlan(days)])
      .then(([rev, plan]) => {
        setPoints(rev);
        setByPlan(plan);
      })
      .catch(() => {
        setPoints([]);
        setByPlan([]);
      })
      .finally(() => setLoading(false));
  }, [days]);

  const totalRevenue = points.reduce((s, p) => s + p.revenue, 0);
  const totalRefund = points.reduce((s, p) => s + p.refundAmount, 0);
  const totalCount = points.reduce((s, p) => s + p.count, 0);

  return (
    <section className="mb-6 rounded-xl border border-border bg-surface p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold text-text">매출 통계</h2>
          <p className="mt-0.5 text-[11px] text-text-muted">archived 구독은 집계에서 제외</p>
        </div>
        <div className="flex gap-1 rounded-md border border-border p-1">
          {RANGES.map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setDays(r)}
              className={`rounded px-2.5 py-1 text-xs font-medium transition-colors ${
                days === r
                  ? "bg-primary text-primary-fg"
                  : "text-text-muted hover:text-text"
              }`}
            >
              {r}일
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <StatCard label="총 매출" value={`₩${totalRevenue.toLocaleString()}`} />
        <StatCard label="환불액" value={`₩${totalRefund.toLocaleString()}`} tone="danger" />
        <StatCard label="결제 건수" value={`${totalCount}건`} />
      </div>

      <div className="mt-5">
        <RevenueLineChart points={points} loading={loading} />
      </div>

      <div className="mt-5">
        <PlanBarChart data={byPlan} loading={loading} />
      </div>
    </section>
  );
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone?: "danger";
}) {
  return (
    <div className="rounded-lg border border-border bg-bg-elevated px-4 py-3">
      <p className="text-[11px] font-medium text-text-muted">{label}</p>
      <p
        className={`mt-1 text-xl font-bold tabular-nums ${
          tone === "danger" ? "text-rose-400" : "text-text"
        }`}
      >
        {value}
      </p>
    </div>
  );
}

// ----------------------------------------------------------
// 라인 차트 — 매출(primary) + 환불(rose). SVG polyline.
// ----------------------------------------------------------
function RevenueLineChart({
  points,
  loading,
}: {
  points: RevenuePoint[];
  loading: boolean;
}) {
  if (loading) {
    return <div className="h-40 rounded-md border border-border bg-bg-elevated" />;
  }
  if (points.length === 0) {
    return (
      <div className="flex h-40 items-center justify-center rounded-md border border-border bg-bg-elevated text-xs text-text-muted">
        해당 기간 결제가 없습니다
      </div>
    );
  }

  const W = 760;
  const H = 200;
  const padL = 56;
  const padR = 12;
  const padT = 12;
  const padB = 28;
  const innerW = W - padL - padR;
  const innerH = H - padT - padB;

  const maxRevenue = Math.max(...points.map((p) => p.revenue));
  const maxRefund = Math.max(...points.map((p) => p.refundAmount));
  const yMax = Math.max(maxRevenue, maxRefund, 1);

  const xOf = (i: number) =>
    padL + (points.length === 1 ? innerW / 2 : (innerW * i) / (points.length - 1));
  const yOf = (v: number) => padT + innerH - (innerH * v) / yMax;

  const revenuePath = points.map((p, i) => `${i === 0 ? "M" : "L"}${xOf(i)},${yOf(p.revenue)}`).join(" ");
  const refundPath = points.map((p, i) => `${i === 0 ? "M" : "L"}${xOf(i)},${yOf(p.refundAmount)}`).join(" ");

  // y축 4단계 grid
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((r) => ({
    y: padT + innerH * (1 - r),
    label: Math.round(yMax * r).toLocaleString(),
  }));

  // x축 라벨 — 양 끝 + 중간 2개
  const xLabelIndices = points.length <= 4
    ? points.map((_, i) => i)
    : [0, Math.floor(points.length / 3), Math.floor((2 * points.length) / 3), points.length - 1];

  return (
    <div className="rounded-md border border-border bg-bg-elevated p-3">
      <div className="mb-2 flex items-center gap-4 text-[11px] text-text-muted">
        <span className="flex items-center gap-1.5">
          <span className="h-1.5 w-3 rounded-full bg-primary" /> 매출
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-1.5 w-3 rounded-full bg-rose-400" /> 환불
        </span>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} className="block h-44 w-full">
        {/* y축 grid */}
        {yTicks.map((t, i) => (
          <g key={i}>
            <line
              x1={padL}
              x2={W - padR}
              y1={t.y}
              y2={t.y}
              stroke="currentColor"
              strokeOpacity={0.12}
              strokeWidth={1}
            />
            <text
              x={padL - 6}
              y={t.y + 3}
              fontSize={9}
              textAnchor="end"
              fill="currentColor"
              fillOpacity={0.5}
              className="tabular-nums"
            >
              {t.label}
            </text>
          </g>
        ))}

        {/* refund 먼저(아래) → revenue 위 */}
        <path d={refundPath} fill="none" stroke="rgb(251 113 133)" strokeWidth={1.5} />
        {points.map((p, i) =>
          p.refundAmount > 0 ? (
            <circle key={`r-${i}`} cx={xOf(i)} cy={yOf(p.refundAmount)} r={2} fill="rgb(251 113 133)" />
          ) : null,
        )}

        <path d={revenuePath} fill="none" stroke="var(--primary)" strokeWidth={1.75} />
        {points.map((p, i) => (
          <circle key={`v-${i}`} cx={xOf(i)} cy={yOf(p.revenue)} r={2.5} fill="var(--primary)" />
        ))}

        {/* x축 라벨 */}
        {xLabelIndices.map((i) => (
          <text
            key={`x-${i}`}
            x={xOf(i)}
            y={H - 8}
            fontSize={9}
            textAnchor="middle"
            fill="currentColor"
            fillOpacity={0.5}
          >
            {points[i].date.slice(5)} {/* MM-DD */}
          </text>
        ))}
      </svg>
    </div>
  );
}

// ----------------------------------------------------------
// 플랜별 분포 막대 — 각 plan 색은 PLAN_TOKENS.
// ----------------------------------------------------------
function PlanBarChart({ data, loading }: { data: RevenueByPlan[]; loading: boolean }) {
  if (loading) {
    return <div className="h-24 rounded-md border border-border bg-bg-elevated" />;
  }
  if (data.length === 0) {
    return (
      <div className="flex h-24 items-center justify-center rounded-md border border-border bg-bg-elevated text-xs text-text-muted">
        플랜별 데이터가 없습니다
      </div>
    );
  }

  const maxRevenue = Math.max(...data.map((d) => d.revenue), 1);

  return (
    <div className="rounded-md border border-border bg-bg-elevated p-3">
      <p className="mb-2 text-[11px] font-medium text-text-muted">플랜별 매출 분포</p>
      <ul className="space-y-2">
        {data.map((d) => {
          const token = PLAN_TOKENS[d.plan as SubscriptionPlanKey];
          const widthPct = (d.revenue / maxRevenue) * 100;
          return (
            <li key={d.plan} className="flex items-center gap-3">
              <span className={`shrink-0 text-[11px] font-semibold ${token?.text ?? "text-text"}`}>
                {token?.label ?? d.plan}
              </span>
              <div className="relative h-3 flex-1 overflow-hidden rounded-sm bg-surface-hover">
                <div
                  className={`h-full ${token?.bar ?? "bg-primary"}`}
                  style={{ width: `${widthPct}%` }}
                />
              </div>
              <span className="shrink-0 text-[11px] tabular-nums text-text-muted">
                {d.count}건 · ₩{d.revenue.toLocaleString()}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
