"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { EXAM_CERTS, pickUpcoming, diffDays } from "@/lib/examSchedules";
import { CERT_TOKENS, slugFromCert } from "@/lib/cert-tokens";
import {
  PASS_THRESHOLD,
  type CertActivity,
} from "@/lib/dashboard/activeCerts";

type Props = {
  /** 활성 자격증 리스트 (풀이수 5건 이상). 풀이수 많은 순으로 정렬돼 있음 */
  activeCerts: CertActivity[];
};

type FocusInfo = {
  activity: CertActivity;
  dDay: number | null;        // 시험 D-day (null = 상시 시험)
  examLabel: string | null;   // "제62회" / "2026년 1회" 등
  dateLabel: string | null;   // "8/22" 또는 "10/24~11/13"
  passGap: number;            // 합격선까지 % 차이 (>=0)
  passed: boolean;            // 합격선 도달 여부
};

function buildFocus(activity: CertActivity, now: Date): FocusInfo {
  const slug = slugFromCert(activity.cert);
  const examCert = EXAM_CERTS.find((c) => c.id === slug);
  const upcoming = examCert && !examCert.isAlwaysOpen
    ? pickUpcoming(examCert.schedules, now)
    : null;

  let dDay: number | null = null;
  let dateLabel: string | null = null;
  if (upcoming) {
    const target = new Date(upcoming.date + "T00:00:00+09:00");
    dDay = diffDays(target, now);
    const start = new Date(upcoming.date + "T00:00:00+09:00");
    const startLabel = `${start.getMonth() + 1}/${start.getDate()}`;
    const end = upcoming.endDate ? new Date(upcoming.endDate + "T00:00:00+09:00") : null;
    dateLabel = end
      ? `${startLabel}~${end.getMonth() + 1}/${end.getDate()}`
      : startLabel;
  }

  const threshold = PASS_THRESHOLD[activity.cert];
  const gap = threshold - activity.recent5AvgScore;

  return {
    activity,
    dDay,
    examLabel: upcoming?.label ?? null,
    dateLabel,
    passGap: Math.max(0, Math.round(gap * 10) / 10),
    passed: gap <= 0,
  };
}

/** 가장 임박한 자격증 선택 — D-day 작은 순. D-day 없는 자격증 (상시) 은 후순위. */
function pickFocus(activeCerts: CertActivity[], now: Date): FocusInfo | null {
  if (activeCerts.length === 0) return null;
  const enriched = activeCerts.map((a) => buildFocus(a, now));
  enriched.sort((a, b) => {
    // D-day 있는 쪽 우선, 그 다음 D-day 작은 순
    if (a.dDay == null && b.dDay == null) return 0;
    if (a.dDay == null) return 1;
    if (b.dDay == null) return -1;
    return a.dDay - b.dDay;
  });
  return enriched[0];
}

export default function ExamGoalHero({ activeCerts }: Props) {
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    // 외부(Date) sync — SSR 에서는 null, mount 직후 1회 동기화. hydration mismatch 회피.
    // eslint-disable-next-line react-hooks/set-state-in-effect -- 외부 Date sync, mount 1회
    setNow(new Date());
  }, []);

  // SSR: 빈 자리 차지하지 않고 렌더링 보류
  if (!now) return null;

  // 활성 자격증 없음 — 빈 상태 (5건 미만 사용자)
  if (activeCerts.length === 0) {
    return (
      <section className="mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-bold">시험 목표 설정</h2>
            <p className="mt-1 text-sm text-text-muted">
              모의고사 한 회차를 풀면 자격증별 시험 일정과 합격 진행 상황을 보여드릴게요.
            </p>
          </div>
          <Link
            href="/mock-exams"
            className="inline-flex items-center rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-fg transition-colors hover:bg-primary-hover"
          >
            모의고사 풀기
          </Link>
        </div>
      </section>
    );
  }

  const focus = pickFocus(activeCerts, now);
  if (!focus) return null;

  const token = CERT_TOKENS[focus.activity.cert];

  return (
    <section
      className="relative mt-6 overflow-hidden rounded-xl border border-border bg-surface p-5 sm:p-6"
      aria-label="시험 목표"
    >
      {/* 좌측 cert 컬러 strip */}
      <span
        className={`absolute left-0 top-0 h-full w-1 ${token.tailwind.bg}`}
        aria-hidden
      />

      <div className="flex flex-col gap-4 pl-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0 flex-1">
          {/* cert chip */}
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex items-center gap-1.5 rounded-sm border ${token.tailwind.border} ${token.tailwind.bgSoft} px-2 py-0.5 text-[11px] font-semibold ${token.tailwind.text}`}
            >
              <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} aria-hidden />
              {token.label}
            </span>
            {focus.examLabel && (
              <span className="text-[11px] text-text-subtle">
                {focus.examLabel} · {focus.dateLabel}
              </span>
            )}
          </div>

          {/* D-day 또는 상시 라벨 */}
          {focus.dDay != null ? (
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-xs text-text-muted">시험까지</span>
              <span className="text-3xl font-bold tabular-nums text-text sm:text-4xl">
                D-{focus.dDay}
              </span>
            </div>
          ) : (
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text">상시 응시</span>
              <span className="text-xs text-text-muted">언제든 응시 가능</span>
            </div>
          )}

          {/* 합격 예측 게이지 — UWorld 식 */}
          {(() => {
            const threshold = focus.activity.recent5AvgScore + focus.passGap;
            // 게이지 최대 = max(100, threshold + buffer). 100점 만점 가정.
            const score = focus.activity.recent5AvgScore;
            const scorePct = Math.min(100, Math.max(0, score));
            const thresholdPct = Math.min(100, Math.max(0, threshold));
            // 색 분기: 합격선 도달 = primary, +5점 이내 근접 = warning, 미달 = danger
            const barColor = focus.passed
              ? "var(--primary)"
              : focus.passGap <= 5
                ? "var(--warning)"
                : "var(--danger)";
            const labelClass = focus.passed
              ? "text-primary"
              : focus.passGap <= 5
                ? "text-warning"
                : "text-danger";
            return (
              <div className="mt-3">
                <div className="flex items-baseline justify-between gap-2 text-xs">
                  <span className="text-text-muted">
                    예상 점수{" "}
                    <span className="font-semibold tabular-nums text-text">{score}점</span>
                  </span>
                  <span className={`font-semibold tabular-nums ${labelClass}`}>
                    {focus.passed
                      ? "합격선 도달 ✓"
                      : `합격까지 +${focus.passGap}점`}
                  </span>
                </div>
                {/* 게이지 바 */}
                <div className="relative mt-1.5 h-2 overflow-hidden rounded-sm bg-bg-elevated">
                  {/* 합격선 마커 (점선) */}
                  <div
                    className="absolute top-0 h-full border-l border-dashed border-text-subtle"
                    style={{ left: `${thresholdPct}%` }}
                    aria-hidden
                  />
                  {/* 현재 점수 fill */}
                  <div
                    className="h-full transition-all duration-700"
                    style={{ width: `${scorePct}%`, backgroundColor: barColor }}
                  />
                </div>
                <div className="mt-1 flex justify-between text-[10px] text-text-subtle tabular-nums">
                  <span>0</span>
                  <span style={{ marginLeft: `${thresholdPct - 4}%` }}>
                    합격선 {threshold}
                  </span>
                  <span>100</span>
                </div>
              </div>
            );
          })()}
        </div>

        {/* CTA 2개 */}
        <div className="flex flex-wrap gap-2 sm:flex-col sm:items-stretch">
          <Link
            href={`/mock-exams?cert=${focus.activity.cert}`}
            className="inline-flex items-center justify-center rounded-sm bg-primary px-4 py-2 text-sm font-medium text-primary-fg transition-colors hover:bg-primary-hover"
          >
            모의고사 풀기
          </Link>
          <Link
            href={`/past-exams?cert=${slugFromCert(focus.activity.cert)}`}
            className="inline-flex items-center justify-center rounded-sm border border-border bg-bg-elevated px-4 py-2 text-sm font-medium text-text transition-colors hover:border-border-strong"
          >
            기출 보기
          </Link>
        </div>
      </div>
    </section>
  );
}
