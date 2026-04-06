"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import { getMockExams, type MockExamSummary } from "@/lib/mockExamApi";

export default function MockExamsPage() {
  return (
    <AuthGuard>
      <MockExamsListContent />
    </AuthGuard>
  );
}

function MockExamsListContent() {
  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMockExams()
      .then(setExams)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  if (error) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </main>
    );
  }

  if (!exams) {
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
        <p className="mt-2 text-muted">진짜 SQLD 시험과 동일한 50문항 구성</p>

        {exams.length === 0 ? (
          <div className="mt-12 rounded-xl border border-border bg-surface p-8 text-center text-muted">
            아직 등록된 모의고사가 없습니다.
            <br />
            관리자가 새 모의고사를 만들면 여기 표시됩니다.
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {exams.map((exam) => (
              <Link
                key={exam.id}
                href={`/mock-exams/${exam.id}`}
                className="block rounded-xl border border-border bg-surface p-5 transition-all hover:-translate-y-0.5 hover:border-amber-500/40 hover:shadow-[0_0_16px_var(--glow)]"
              >
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold">{exam.name}</h2>
                  <span className="text-xs text-muted">
                    #{exam.sequence}
                  </span>
                </div>
                <p className="mt-2 text-sm text-muted">
                  총 {exam.totalQuestions}문항
                </p>
                <p className="mt-1 text-xs text-muted/70">
                  {new Date(exam.createdAt).toLocaleDateString("ko-KR")}
                </p>
              </Link>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
