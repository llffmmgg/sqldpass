"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";

import Spinner from "@/components/Spinner";
import { Badge, Card, Container } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromExamType,
  type CertKey,
} from "@/lib/cert-tokens";
import {
  listPastExams,
  type PastExamSummary,
} from "@/lib/pastExamApi";

const CERT_TO_SLUG: Record<CertKey, string> = {
  SQLD: "sqld",
  ENGINEER_PRACTICAL: "engineer",
  ENGINEER_WRITTEN: "engineer-written",
  COMPUTER_LITERACY_1: "computer-literacy-1",
  COMPUTER_LITERACY_2: "computer-literacy-2",
  ADSP: "adsp",
};

const UNKNOWN_YEAR = -1;

export default function PastExamsPage() {
  return (
    <Suspense fallback={null}>
      <PastExamsPageContent />
    </Suspense>
  );
}

function PastExamsPageContent() {
  const searchParams = useSearchParams();
  const certParam = searchParams?.get("cert");
  const activeCert: CertKey =
    (certParam && certParam in CERT_TOKENS ? (certParam as CertKey) : null) ?? "SQLD";

  // 자격증별 개수 카운트용 — 최초 1회 전 자격증을 병렬로 조회
  const [countsByCert, setCountsByCert] = useState<Record<CertKey, number>>({
    SQLD: 0,
    ENGINEER_PRACTICAL: 0,
    ENGINEER_WRITTEN: 0,
    COMPUTER_LITERACY_1: 0,
    COMPUTER_LITERACY_2: 0,
    ADSP: 0,
  });

  const [exams, setExams] = useState<PastExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all(
      (Object.keys(CERT_TO_SLUG) as CertKey[]).map((k) =>
        listPastExams(CERT_TO_SLUG[k])
          .then((list) => [k, list.length] as const)
          .catch(() => [k, 0] as const),
      ),
    ).then((entries) => {
      if (cancelled) return;
      const next = { ...countsByCert };
      for (const [k, n] of entries) next[k] = n;
      setCountsByCert(next);
    });
    return () => {
      cancelled = true;
    };
    // 최초 1회만 집계. activeCert와 무관.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let cancelled = false;
    setExams(null);
    setError(null);
    listPastExams(CERT_TO_SLUG[activeCert])
      .then((data) => {
        if (!cancelled) setExams(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "불러오기 실패");
      });
    return () => {
      cancelled = true;
    };
  }, [activeCert]);

  const token = CERT_TOKENS[activeCert];

  // 연도별 그룹핑 — 최신 연도 먼저, 같은 연도 안에서는 회차 내림차순
  const grouped = useMemo(() => {
    if (!exams) return null;
    const map = new Map<number, PastExamSummary[]>();
    for (const e of exams) {
      const year = e.examYear ?? UNKNOWN_YEAR;
      const arr = map.get(year) ?? [];
      arr.push(e);
      map.set(year, arr);
    }
    const sorted = Array.from(map.entries())
      .map(([year, list]) => ({
        year,
        list: list.slice().sort((a, b) => {
          const ra = a.examRound ?? 0;
          const rb = b.examRound ?? 0;
          if (ra !== rb) return rb - ra;
          return b.id - a.id;
        }),
      }))
      .sort((a, b) => b.year - a.year);
    return sorted;
  }, [exams]);

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-16">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">기출 복원</h1>
        <p className="mt-2 text-sm text-text-muted">
          {token.labelLong} · 실제 시험 회차를 복원해서 제한 시간 안에 응시해보세요. 로그인 없이도 해설과 정답을 바로 확인할 수 있습니다.
        </p>

        {/* 자격증 탭 — 모의고사와 동일한 단일 pill + cert dot + count */}
        <div className="mt-6 -mx-1 flex gap-1 overflow-x-auto rounded-lg border border-border bg-surface p-1 text-sm">
          {CERT_LIST.map((c) => {
            const active = c.key === activeCert;
            const count = countsByCert[c.key] ?? 0;
            return (
              <Link
                key={c.key}
                href={`/past-exams?cert=${c.key}`}
                scroll={false}
                className={`flex shrink-0 items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  active
                    ? "bg-primary/10 text-primary ring-1 ring-primary/20"
                    : "text-text-muted hover:bg-surface-hover hover:text-text"
                }`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${c.tailwind.dot}`} />
                {c.label}
                <span className="text-xs opacity-60 tabular-nums">{count}</span>
              </Link>
            );
          })}
        </div>

        {/* 목록 */}
        <div className="mt-8">
          {error && (
            <Card padding="md" className="border-danger/40 bg-danger/10 text-sm text-danger">
              {error}
            </Card>
          )}

          {!error && exams === null && (
            <div className="flex justify-center py-16">
              <Spinner message="기출 회차 불러오는 중..." />
            </div>
          )}

          {!error && exams && exams.length === 0 && (
            <Card padding="lg" className="text-center">
              <p className="text-base font-semibold">아직 준비 중입니다</p>
              <p className="mt-2 text-sm text-text-muted">
                {token.label} 기출 복원 회차가 아직 등록되지 않았습니다. 블로그의 회차별 대비 가이드부터 살펴보세요.
              </p>
              <Link
                href={`/blog?cert=${token.key}`}
                className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline"
              >
                관련 블로그 보기 →
              </Link>
            </Card>
          )}

          {grouped && grouped.length > 0 && (
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
                      <PastExamCard key={exam.id} exam={exam} />
                    ))}
                  </div>
                </section>
              ))}
            </div>
          )}
        </div>

        <div className="mt-14 flex items-center justify-center gap-6 text-sm text-text-muted">
          <Link href="/solve" className="transition-colors hover:text-text">
            무한 문제 풀이 →
          </Link>
          <span className="text-border">·</span>
          <Link href="/mock-exams" className="transition-colors hover:text-text">
            모의고사 →
          </Link>
        </div>
      </Container>
    </main>
  );
}

function PastExamCard({ exam }: { exam: PastExamSummary }) {
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

        {exam.solved &&
          exam.bestCorrectCount != null &&
          exam.bestTotalCount != null && (
            <span
              className="pointer-events-none absolute right-2 top-1 select-none font-[family-name:var(--font-caveat)] text-3xl font-bold leading-none text-red-500/90 sm:text-4xl"
              style={{ transform: "rotate(-12deg)" }}
            >
              {exam.bestCorrectCount}
              <span className="text-2xl sm:text-3xl">/</span>
              {exam.bestTotalCount}
            </span>
          )}

        <div className="flex flex-wrap items-center gap-1.5 pr-20">
          {token && (
            <span
              className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.text}`}
            >
              <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} />
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
          {isNew && <NewBadge />}
        </div>

        <h3 className="mt-3 text-lg font-semibold leading-tight">
          {exam.examRound ? `제${exam.examRound}회` : exam.name}
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
          응시하기 →
        </div>
      </Card>
    </Link>
  );
}

function NewBadge() {
  return (
    <span className="inline-flex items-center rounded-full border border-rose-500/40 bg-rose-500/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-rose-500 dark:text-rose-300">
      NEW
    </span>
  );
}

function isWithinDays(iso: string | null | undefined, days: number): boolean {
  if (!iso) return false;
  const created = new Date(iso).getTime();
  if (Number.isNaN(created)) return false;
  return Date.now() - created <= days * 24 * 60 * 60 * 1000;
}

function formatExamDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(
    d.getDate(),
  ).padStart(2, "0")}`;
}
