"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";

import Spinner from "@/components/Spinner";
import { Card, Container } from "@/components/ui";
import MockExamCardView from "@/components/exam/MockExamCard";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromExamType,
  type CertKey,
} from "@/lib/cert-tokens";
import { isLoggedIn } from "@/lib/auth";
import {
  getMockExams,
  type ExamType,
  type MockExamSummary,
} from "@/lib/mockExamApi";
import { isExamNew } from "@/lib/mockExamNew";
import { getPublicMockExams } from "@/lib/publicApi";

type DifficultyFilter = "ALL" | "쉬움" | "보통" | "어려움" | "매우 어려움";

/** 자격증별 권장 시간 (분). 모의고사 카드 메타에 노출. */
const EXAM_DURATION_MIN: Record<ExamType, number> = {
  SQLD: 90,
  ENGINEER_PRACTICAL: 150,
  ENGINEER_WRITTEN: 150,
  COMPUTER_LITERACY_1: 60,
  COMPUTER_LITERACY_2: 40,
  ADSP: 90,
};

const DIFFICULTY_OPTIONS: { value: DifficultyFilter; label: string; activeClass: string }[] = [
  { value: "ALL", label: "전체", activeClass: "border-primary/40 bg-primary/10 text-primary" },
  { value: "쉬움", label: "쉬움", activeClass: "border-emerald-500/40 bg-emerald-500/10 text-emerald-500" },
  { value: "보통", label: "보통", activeClass: "border-amber-500/40 bg-amber-500/10 text-amber-500" },
  { value: "어려움", label: "어려움", activeClass: "border-orange-500/40 bg-orange-500/10 text-orange-500" },
  { value: "매우 어려움", label: "매우 어려움", activeClass: "border-red-500/40 bg-red-500/10 text-red-500" },
];

export default function MockExamsClient() {
  return (
    <Suspense fallback={null}>
      <MockExamsContent />
    </Suspense>
  );
}

function MockExamsContent() {
  const searchParams = useSearchParams();
  const certParam = searchParams?.get("cert");
  const activeCert: CertKey =
    (certParam && certParam in CERT_TOKENS ? (certParam as CertKey) : null) ?? "SQLD";

  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [authed, setAuthed] = useState(false);
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("ALL");

  useEffect(() => {
    const loggedIn = isLoggedIn();
    setAuthed(loggedIn);
    const fetcher = loggedIn ? getMockExams() : getPublicMockExams();
    fetcher
      .then(setExams)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  const token = CERT_TOKENS[activeCert];

  const filtered = useMemo(() => {
    if (!exams) return null;
    return exams
      .filter((e) => e.examType === activeCert)
      .filter((e) => difficulty === "ALL" || e.difficultyLabel === difficulty)
      .slice()
      .sort((a, b) => b.sequence - a.sequence);
  }, [exams, activeCert, difficulty]);

  // 자격증별 난이도 카운트 — 칩에 갯수 표시용
  const difficultyCounts = useMemo(() => {
    if (!exams) return {} as Record<string, number>;
    const counts: Record<string, number> = { ALL: 0, "쉬움": 0, "보통": 0, "어려움": 0, "매우 어려움": 0 };
    exams.filter((e) => e.examType === activeCert).forEach((e) => {
      counts.ALL++;
      if (e.difficultyLabel) counts[e.difficultyLabel] = (counts[e.difficultyLabel] ?? 0) + 1;
    });
    return counts;
  }, [exams, activeCert]);

  return (
    <Container size="narrow" className="py-16">
      <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">모의고사</h2>
      <p className="mt-2 text-sm text-text-muted">
        {token.labelLong} · 실전 타이머와 함께 무료 CBT 로 응시해보세요. 점수 기록·오답 노트는 로그인 후 자동 저장됩니다.
      </p>

      {/* 자격증 탭 — past-exams 와 동일한 단일 pill + cert dot + count + NEW 카운트 */}
      <div className="mt-6 -mx-1 flex gap-1 overflow-x-auto rounded-lg border border-border bg-surface p-1 text-sm">
        {CERT_LIST.map((c) => {
          const active = c.key === activeCert;
          const count = exams ? exams.filter((e) => e.examType === c.key).length : 0;
          const newCount = exams
            ? exams.filter((e) => e.examType === c.key && isExamNew(e)).length
            : 0;
          return (
            <Link
              key={c.key}
              href={`/mock-exams?cert=${c.key}`}
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

      {/* 난이도 필터 칩 */}
      <div className="mt-3 flex flex-wrap gap-1.5">
        {DIFFICULTY_OPTIONS.map((opt) => {
          const active = difficulty === opt.value;
          const count = difficultyCounts[opt.value] ?? 0;
          if (opt.value !== "ALL" && count === 0) return null;
          return (
            <button
              key={opt.value}
              onClick={() => setDifficulty(opt.value)}
              className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors ${
                active
                  ? `${opt.activeClass} ring-1 ring-current/30`
                  : "border-border bg-surface text-text-muted hover:border-primary/40 hover:text-text"
              }`}
            >
              {opt.label}
              <span className="text-[10px] opacity-60 tabular-nums">{count}</span>
            </button>
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
            <Spinner message="모의고사 목록 불러오는 중..." />
          </div>
        )}

        {!error && filtered && filtered.length === 0 && (
          <Card padding="lg" className="text-center">
            <p className="text-base font-semibold">아직 준비 중입니다</p>
            <p className="mt-2 text-sm text-text-muted">
              {token.label} 모의고사가 아직 등록되지 않았습니다.
            </p>
          </Card>
        )}

        {filtered && filtered.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {filtered.map((exam) => (
              <MockExamCard key={exam.id} exam={exam} />
            ))}
          </div>
        )}
      </div>

      <div className="mt-14 flex items-center justify-center gap-6 text-sm text-text-muted">
        <Link href="/solve" className="transition-colors hover:text-text">
          무한 문제 풀이 →
        </Link>
        <span className="text-border">·</span>
        <Link href="/past-exams" className="transition-colors hover:text-text">
          기출 복원 →
        </Link>
      </div>
    </Container>
  );
}

function MockExamCard({ exam }: { exam: MockExamSummary }) {
  const cert = certFromExamType(exam.examType);
  // PREMIUM 자동 분류 — backend 가 isPremium 을 계산해 응답 (visibility=PREMIUM 또는 난이도 ≥ 0.5).
  const isPremium = exam.isPremium ?? exam.visibility === "PREMIUM";
  const certLabel = cert ? CERT_TOKENS[cert].label : "";
  const isPastExam =
    exam.kind === "PAST_EXAM" && exam.examYear != null && exam.examRound != null;
  const examLabel = isPastExam
    ? `${exam.examYear}년 ${exam.examRound}회`
    : `${exam.sequence}회`;
  const title = isPastExam
    ? `기출 ${exam.examYear}년 ${exam.examRound}회`
    : exam.name;
  const score = exam.solved ? exam.bestCorrectCount : null;
  const scoreTotal = exam.solved ? exam.bestTotalCount : null;

  return (
    <MockExamCardView
      href={`/mock-exams/${exam.id}`}
      cert={certLabel}
      examLabel={examLabel}
      title={title}
      difficulty={exam.difficultyLabel ?? null}
      totalQuestions={exam.totalQuestions}
      durationMin={EXAM_DURATION_MIN[exam.examType] ?? 90}
      tier={isPremium ? "pass+" : "free"}
      isNew={isExamNew(exam)}
      score={score}
      scoreTotal={scoreTotal}
      verified={exam.expertVerified ? "corner" : null}
    />
  );
}

export function DifficultyBadge({ label }: { label: MockExamSummary["difficultyLabel"] }) {
  if (!label) return null;
  const cls = difficultyBadgeClass(label);
  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-bold ${cls}`}>
      {label}
    </span>
  );
}

function difficultyBadgeClass(label: NonNullable<MockExamSummary["difficultyLabel"]>): string {
  switch (label) {
    case "쉬움":
      return "border-emerald-500/40 bg-emerald-500/10 text-emerald-600 dark:text-emerald-300";
    case "보통":
      return "border-amber-500/40 bg-amber-500/10 text-amber-600 dark:text-amber-300";
    case "어려움":
      return "border-orange-500/40 bg-orange-500/10 text-orange-600 dark:text-orange-300";
    case "매우 어려움":
      return "border-red-500/40 bg-red-500/10 text-red-600 dark:text-red-300";
  }
}

export function ExamBadge({ examType }: { examType: ExamType }) {
  const cert = certFromExamType(examType);
  if (!cert) return null;
  const t = CERT_TOKENS[cert];
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${t.tailwind.border} ${t.tailwind.bgSoft} ${t.tailwind.text}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${t.tailwind.dot}`} />
      {t.label}
    </span>
  );
}
