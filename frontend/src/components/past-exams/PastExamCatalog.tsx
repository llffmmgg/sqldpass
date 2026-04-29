"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Badge, Card, cn } from "@/components/ui";
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
  const token = cert ? CERT_TOKENS[cert] : null;
  const isNew = isWithinDays(exam.createdAt, 3);

  return (
    <Link href={`/past-exams/${exam.id}`} className="group relative block">
      <Card
        variant="interactive"
        padding="md"
        accent={cert ?? undefined}
        className="relative overflow-hidden"
      >
        {exam.expertVerified && (
          <div className="pointer-events-none absolute -left-[38px] top-[18px] z-10 -rotate-45 bg-emerald-600 px-10 py-0.5 text-center text-[9px] font-bold tracking-wide text-white shadow-sm dark:bg-emerald-500">
            전문가 검수
          </div>
        )}

        {best && (
          <span
            className="pointer-events-none absolute right-2 top-1 select-none font-[family-name:var(--font-caveat)] text-3xl font-bold leading-none text-red-500/90 sm:text-4xl"
            style={{ transform: "rotate(-12deg)" }}
          >
            {best.correct}
            <span className="text-2xl sm:text-3xl">/</span>
            {best.total}
          </span>
        )}

        <div className="flex flex-wrap items-center gap-1.5 pr-20">
          {token && (
            <span
              className={cn(
                "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide",
                token.tailwind.border,
                token.tailwind.bgSoft,
                token.tailwind.text,
              )}
            >
              <span className={cn("h-1.5 w-1.5 rounded-full", token.tailwind.dot)} />
              {token.label}
            </span>
          )}
          <Badge
            variant="soft"
            size="xs"
            className="border-primary/40 bg-primary/10 text-primary"
          >
            기출
          </Badge>
        </div>

        {isNew && (
          /* eslint-disable-next-line @next/next/no-img-element -- 정적 배지 이미지, next/image 의 추가 최적화가 비용 대비 의미 없음 */
          <img
            src="/badges/new-logo.webp"
            alt="NEW"
            aria-label="새로 추가된 회차"
            className="pointer-events-none absolute -bottom-1 -right-1 z-10 h-16 w-16 select-none rotate-[8deg] object-contain drop-shadow-[0_4px_10px_rgba(16,185,129,0.35)] transition-transform duration-300 ease-out group-hover:-rotate-3 group-hover:scale-110 sm:h-20 sm:w-20"
          />
        )}

        <h3 className="mt-3 text-lg font-semibold leading-tight">
          {buildExamTitle(exam)}
        </h3>

        <div className="mt-2 flex items-center gap-2 text-sm text-text-muted">
          <span>총 {exam.totalQuestions}문항</span>
          {exam.examDate && (
            <>
              <span className="text-border">·</span>
              <span className="tabular-nums">{formatExamDate(exam.examDate)}</span>
            </>
          )}
        </div>

        <div className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-primary transition-transform group-hover:translate-x-1">
          문제 보러가기 →
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
