"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import { getMockExams, type ExamType, type MockExamSummary } from "@/lib/mockExamApi";

export default function MockExamsPage() {
  return (
    <AuthGuard>
      <MockExamsListContent />
    </AuthGuard>
  );
}

type Filter = "ALL" | ExamType;

function MockExamsListContent() {
  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>("ALL");

  useEffect(() => {
    getMockExams()
      .then(setExams)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  const filtered = useMemo(() => {
    if (!exams) return null;
    if (filter === "ALL") return exams;
    return exams.filter((e) => e.examType === filter);
  }, [exams, filter]);

  if (error) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </main>
    );
  }

  if (!exams || !filtered) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <Spinner message="모의고사 목록 불러오는 중..." />
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">모의고사</h1>
        <p className="mt-2 text-sm text-muted">
          SQLD 50문항 · 정보처리기사 실기 20문항 모의고사 모두 지원
        </p>

        {/* 필터 탭 */}
        <div className="mt-6 flex gap-2 rounded-lg border border-border bg-surface p-1 text-sm">
          <FilterTab label="전체" count={exams.length} active={filter === "ALL"} onClick={() => setFilter("ALL")} />
          <FilterTab
            label="SQLD"
            count={exams.filter((e) => e.examType === "SQLD").length}
            active={filter === "SQLD"}
            onClick={() => setFilter("SQLD")}
            accent="amber"
          />
          <FilterTab
            label="정처기 실기"
            count={exams.filter((e) => e.examType === "ENGINEER_PRACTICAL").length}
            active={filter === "ENGINEER_PRACTICAL"}
            onClick={() => setFilter("ENGINEER_PRACTICAL")}
            accent="emerald"
          />
        </div>

        {filtered.length === 0 ? (
          <div className="mt-12 rounded-xl border border-border bg-surface p-8 text-center text-muted">
            {filter === "ALL"
              ? "아직 등록된 모의고사가 없습니다. 관리자가 새 모의고사를 만들면 여기 표시됩니다."
              : "해당 시험의 모의고사가 아직 없습니다."}
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {filtered.map((exam) => (
              <MockExamCard key={exam.id} exam={exam} />
            ))}
          </div>
        )}
      </div>
    </main>
  );
}

function FilterTab({
  label,
  count,
  active,
  onClick,
  accent,
}: {
  label: string;
  count: number;
  active: boolean;
  onClick: () => void;
  accent?: "amber" | "emerald";
}) {
  const activeClass =
    accent === "emerald"
      ? "bg-emerald-500/15 text-emerald-300 ring-1 ring-emerald-500/30"
      : accent === "amber"
      ? "bg-amber-500/15 text-amber-300 ring-1 ring-amber-500/30"
      : "bg-border text-foreground";
  return (
    <button
      onClick={onClick}
      className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition ${
        active ? activeClass : "text-muted hover:text-foreground"
      }`}
    >
      {label} <span className="ml-1 text-xs opacity-60 tabular-nums">{count}</span>
    </button>
  );
}

function MockExamCard({ exam }: { exam: MockExamSummary }) {
  const isEngineer = exam.examType === "ENGINEER_PRACTICAL";
  const hoverBorder = isEngineer ? "hover:border-emerald-500/40" : "hover:border-amber-500/40";
  const glow = isEngineer
    ? "hover:shadow-[0_0_20px_rgba(16,185,129,0.2)]"
    : "hover:shadow-[0_0_16px_var(--glow)]";

  return (
    <Link
      href={`/mock-exams/${exam.id}`}
      className={`block rounded-xl border border-border bg-surface p-5 transition-all hover:-translate-y-0.5 ${hoverBorder} ${glow}`}
    >
      <div className="flex items-center justify-between gap-2">
        <ExamBadge examType={exam.examType} />
        <span className="text-xs text-muted tabular-nums">#{exam.sequence}</span>
      </div>
      <h2 className="mt-3 text-lg font-semibold leading-tight">{exam.name}</h2>
      <p className="mt-2 text-sm text-muted">총 {exam.totalQuestions}문항</p>
      <p className="mt-1 text-xs text-muted/70">
        {new Date(exam.createdAt).toLocaleDateString("ko-KR")}
      </p>
    </Link>
  );
}

export function ExamBadge({ examType }: { examType: ExamType }) {
  if (examType === "ENGINEER_PRACTICAL") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-emerald-300">
        <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
        정처기 실기
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/40 bg-amber-500/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-amber-300">
      <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
      SQLD
    </span>
  );
}
