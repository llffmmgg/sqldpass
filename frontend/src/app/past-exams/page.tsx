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

  const [exams, setExams] = useState<PastExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-14">
        <header>
          <Badge cert={token.key} variant="soft" size="sm" dot>
            기출 복원
          </Badge>
          <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
            {token.labelLong} 기출 복원
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-text-muted">
            실제 시험 회차를 복원해서 제한 시간 안에 응시해보세요. 로그인 없이도 해설과 정답을 바로 확인할 수 있습니다.
          </p>
        </header>

        {/* 자격증 탭 */}
        <div className="mt-8 flex flex-wrap gap-2">
          {CERT_LIST.map((t) => {
            const active = t.key === activeCert;
            return (
              <Link
                key={t.key}
                href={`/past-exams?cert=${t.key}`}
                className={`rounded-full border px-3.5 py-1.5 text-xs font-semibold transition-colors ${
                  active
                    ? `${t.tailwind.border} ${t.tailwind.bgSoft} ${t.tailwind.text}`
                    : "border-border text-text-muted hover:border-primary/40 hover:text-text"
                }`}
              >
                {t.label}
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

          {exams && exams.length > 0 && (
            <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {exams.map((exam) => (
                <li key={exam.id}>
                  <PastExamCard exam={exam} />
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="mt-12 flex items-center justify-center gap-6 text-sm text-text-muted">
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
  const metaLabel = useMemo(() => buildMeta(exam), [exam]);

  return (
    <Link
      href={`/past-exams/${exam.id}`}
      className={`group block rounded-xl border border-border bg-surface p-5 transition-all duration-200 ${
        token?.tailwind.borderHover ?? "hover:border-primary/40"
      } ${token?.tailwind.bgHover ?? ""}`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-text-subtle">
          {token?.shortLabel ?? exam.examType}
        </span>
        {exam.expertVerified && (
          <span className="rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold text-emerald-400">
            전문가 검수
          </span>
        )}
      </div>
      <h2 className="mt-2 text-lg font-bold tracking-tight">
        {exam.examYear && exam.examRound
          ? `${exam.examYear}년 제${exam.examRound}회`
          : exam.examRound
            ? `제${exam.examRound}회`
            : exam.name}
      </h2>
      <p className="mt-1 text-sm text-text-muted">{metaLabel}</p>
      <div className="mt-4 flex items-center justify-between text-xs font-semibold text-text-muted">
        <span>실제 시험 복원 · 무료 응시</span>
        <span className="text-primary transition-transform group-hover:translate-x-1">응시하기 →</span>
      </div>
    </Link>
  );
}

function buildMeta(exam: PastExamSummary): string {
  const parts: string[] = [];
  parts.push(`${exam.totalQuestions}문제`);
  if (exam.examDate) {
    const d = new Date(exam.examDate);
    if (!Number.isNaN(d.getTime())) {
      parts.push(
        `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(
          d.getDate(),
        ).padStart(2, "0")}`,
      );
    }
  } else if (exam.examYear) {
    parts.push(`${exam.examYear}년`);
  }
  return parts.join(" · ");
}
