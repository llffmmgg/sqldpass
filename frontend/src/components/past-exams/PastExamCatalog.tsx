"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Card, cn } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromExamType,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import type {
  PublicPastExamSummary,
} from "@/lib/publicApi";
import type { PastExamCountsByCert } from "@/lib/pastExamCatalog";
import { getMyBestScores, type BestScoreMap } from "@/lib/api";

const UNKNOWN_YEAR = -1;

export function PastExamTabs({
  activeCert,
  countsByCert,
  newCountsByCert,
}: {
  activeCert: CertKey;
  countsByCert: PastExamCountsByCert;
  newCountsByCert?: PastExamCountsByCert;
}) {
  return (
    <div className="mt-6 -mx-1 flex gap-1 overflow-x-auto rounded-lg border border-border bg-surface p-1 text-sm">
      {CERT_LIST.map((cert) => {
        const active = cert.key === activeCert;
        const count = countsByCert[cert.key] ?? 0;
        const newCount = newCountsByCert?.[cert.key] ?? 0;

        return (
          <Link
            key={cert.key}
            href={`/past-exams/${slugFromCert(cert.key)}`}
            className={cn(
              "flex shrink-0 items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
              active
                ? "bg-primary/10 text-primary ring-1 ring-primary/20"
                : "text-text-muted hover:bg-surface-hover hover:text-text",
            )}
          >
            <span className={cn("h-1.5 w-1.5 rounded-full", cert.tailwind.dot)} />
            {cert.label}
            <span className="text-xs opacity-60 tabular-nums">{count}</span>
            {newCount > 0 && (
              <span
                className="inline-flex items-center rounded-full bg-emerald-500 px-1.5 text-[9px] font-bold leading-4 text-white"
                aria-label={`새로운 회차 ${newCount}개`}
              >
                +{newCount}
              </span>
            )}
          </Link>
        );
      })}
    </div>
  );
}

export function PastExamGrid({
  exams,
}: {
  exams: PublicPastExamSummary[];
}) {
  const grouped = groupPastExams(exams);
  // 회원의 best score 를 클라이언트에서 별도 머지 (SSR + ISR 캐시 우회).
  // 비로그인은 401 → catch 후 빈 객체 fallback.
  const [bestScores, setBestScores] = useState<BestScoreMap>({});
  useEffect(() => {
    let cancelled = false;
    getMyBestScores()
      .then((map) => {
        if (!cancelled) setBestScores(map);
      })
      .catch(() => {
        if (!cancelled) setBestScores({});
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="flex flex-col gap-10">
      {grouped.map(({ year, list }) => (
        <section key={year}>
          <div className="mb-3 flex items-baseline justify-between border-b border-border pb-2">
            <h2 className="text-lg font-semibold tracking-tight">
              {year === UNKNOWN_YEAR ? "연도 미상" : `${year}년`}
            </h2>
            <span className="text-xs text-text-muted tabular-nums">
              {list.length}회차
            </span>
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {list.map((exam) => (
              <PastExamCard key={exam.id} exam={exam} best={bestScores[exam.id]} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

export function PastExamCard({
  exam,
  best,
}: {
  exam: PublicPastExamSummary;
  best?: { correct: number; total: number };
}) {
  const cert = certFromExamType(exam.examType);
  const certLabel = cert ? CERT_TOKENS[cert].label : "";
  const isNew = isWithinDays(exam.createdAt, 3);

  return (
    <Link href={`/past-exams/${exam.id}`} className="group relative block">
      <Card
        variant="interactive"
        padding="none"
        accent={cert ?? undefined}
        className="relative flex min-h-[124px] flex-col overflow-hidden rounded-md p-4 shadow-none hover:-translate-y-0 hover:shadow-none"
      >
        {best && (
          <span
            className="pointer-events-none absolute right-4 top-4 select-none font-[family-name:var(--font-caveat)] text-2xl font-bold leading-none text-red-500/90 sm:text-3xl"
            style={{ transform: "rotate(-12deg)" }}
          >
            {best.correct}
            <span className="text-xl sm:text-2xl">/</span>
            {best.total}
          </span>
        )}

        {/* Eyebrow: "{cert} 기출" · NEW */}
        <div className="flex items-center gap-2 text-[9px] font-medium text-text-muted">
          <span>{certLabel} 기출</span>
          {isNew && (
            <span className="font-semibold text-emerald-600 dark:text-emerald-400">
              NEW
            </span>
          )}
        </div>

        {/* Title */}
        <h3 className="mt-1.5 pr-16 text-base font-bold leading-tight">
          {buildExamTitle(exam)}
        </h3>

        {/* Footer: 메타(문항·날짜·검수완료) + 액션 */}
        <div className="mt-auto flex items-end justify-between gap-3 pt-5 text-[10px] text-text-muted">
          <span className="tabular-nums">
            {exam.totalQuestions}문항
            {exam.examDate && ` · ${formatExamDate(exam.examDate)}`}
            {exam.expertVerified && (
              <>
                {" · "}
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                  검수 완료
                </span>
              </>
            )}
          </span>
          <span className="font-semibold text-text transition-transform group-hover:translate-x-0.5">
            문제 보러가기 →
          </span>
        </div>
      </Card>
    </Link>
  );
}

export function buildExamTitle(exam: PublicPastExamSummary): string {
  if (exam.examRound) {
    return `${exam.examRound}회`;
  }
  return exam.name;
}

function groupPastExams(exams: PublicPastExamSummary[]) {
  const map = new Map<number, PublicPastExamSummary[]>();

  for (const exam of exams) {
    const year = exam.examYear ?? UNKNOWN_YEAR;
    const list = map.get(year) ?? [];
    list.push(exam);
    map.set(year, list);
  }

  return Array.from(map.entries())
    .map(([year, list]) => ({
      year,
      list: list.slice().sort((a, b) => {
        const roundA = a.examRound ?? 0;
        const roundB = b.examRound ?? 0;
        if (roundA !== roundB) return roundB - roundA;
        return b.id - a.id;
      }),
    }))
    .sort((a, b) => b.year - a.year);
}

function isWithinDays(iso: string | null | undefined, days: number): boolean {
  if (!iso) return false;

  const created = new Date(iso).getTime();
  if (Number.isNaN(created)) return false;

  return Date.now() - created <= days * 24 * 60 * 60 * 1000;
}

function formatExamDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;

  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, "0")}.${String(
    date.getDate(),
  ).padStart(2, "0")}`;
}
