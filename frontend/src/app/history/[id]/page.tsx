"use client";

import Link from "next/link";
import { useEffect, useState, use } from "react";
import { getSolve, getQuestionDetail, getSubjects, type SolveResponse, type QuestionDetail, type Subject } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { parseQuestion, OPTION_MARKERS } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";

function buildSubjectMap(subjects: Subject[]): Record<number, string> {
  const map: Record<number, string> = {};
  for (const s of subjects) {
    map[s.id] = s.name;
    for (const child of s.children) {
      map[child.id] = child.name;
    }
  }
  return map;
}

export default function HistoryDetailPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <AuthGuard>
      <HistoryDetailContent params={params} />
    </AuthGuard>
  );
}

function HistoryDetailContent({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [solve, setSolve] = useState<SolveResponse | null>(null);
  const [details, setDetails] = useState<Record<number, QuestionDetail>>({});
  const [subjectMap, setSubjectMap] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getSolve(Number(id)), getSubjects()])
      .then(([solveData, subjects]) => {
        setSolve(solveData);
        setSubjectMap(buildSubjectMap(subjects));
        return Promise.all(
          solveData.answers.map((a) => getQuestionDetail(a.questionId))
        );
      })
      .then((questionDetails) => {
        const map: Record<number, QuestionDetail> = {};
        for (const q of questionDetails) {
          map[q.id] = q;
        }
        setDetails(map);
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading || !solve) {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <Spinner />
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        {/* Header */}
        <Link href="/dashboard" className="text-sm text-muted hover:text-foreground transition-colors">
          &larr; 대시보드로
        </Link>

        {/* Score summary */}
        <div className="mt-6 rounded-xl border border-border bg-surface p-6 text-center">
          <p className="text-sm text-muted">
            {solve.mockExamId != null
              ? `모의고사 #${solve.mockExamId}`
              : solve.subjectId != null
              ? subjectMap[solve.subjectId] || `과목 ${solve.subjectId}`
              : "풀이"}
          </p>
          <p className="mt-2">
            <span className="text-5xl font-bold bg-gradient-to-r from-amber-400 to-amber-300 bg-clip-text text-transparent">
              {solve.score}
            </span>
            <span className="ml-1 text-lg text-muted">점</span>
          </p>
          <p className="mt-2 text-sm text-muted">
            {solve.correctCount}/{solve.totalCount} 정답 &middot; {formatDate(solve.solvedAt)}
          </p>
        </div>

        {/* Answers */}
        <div className="mt-8 space-y-4">
          {solve.answers.map((answer, idx) => {
            const detail = details[answer.questionId];
            const parsed = detail ? parseQuestion(detail.content) : null;

            return (
              <div
                key={answer.questionId}
                className={`rounded-lg border px-5 py-4 ${
                  answer.correct
                    ? "border-green-500/30 bg-green-500/5"
                    : "border-red-500/30 bg-red-500/5"
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <p className="text-sm font-medium text-muted">문제 {idx + 1}</p>
                    {parsed && (
                      <QuestionContent segments={parsed.segments} className="mt-1" />
                    )}
                  </div>
                  <span
                    className={`shrink-0 rounded px-2 py-0.5 text-xs font-semibold ${
                      answer.correct
                        ? "bg-green-500/20 text-green-400"
                        : "bg-red-500/20 text-red-400"
                    }`}
                  >
                    {answer.correct ? "\u2713 정답" : "\u2717 오답"}
                  </span>
                </div>

                {parsed && parsed.options.length > 0 && (
                  <div className="mt-3 space-y-1">
                    {parsed.options.map((opt, optIdx) => {
                      const optNum = optIdx + 1;
                      const isSelected = optNum === answer.selectedOption;
                      const isCorrect = optNum === answer.correctOption;
                      return (
                        <p
                          key={optIdx}
                          className={`rounded px-2 py-1 text-sm ${
                            isCorrect
                              ? "bg-green-500/10 text-green-400 font-medium"
                              : isSelected && !answer.correct
                              ? "bg-red-500/10 text-red-400 line-through"
                              : "text-muted"
                          }`}
                        >
                          {OPTION_MARKERS[optIdx]} {opt}
                          {isCorrect && " ✓"}
                          {isSelected && !isCorrect && " (선택)"}
                        </p>
                      );
                    })}
                  </div>
                )}

                {detail && detail.explanation && (
                  <details className="mt-3 rounded-lg border border-border px-3 py-2 text-sm">
                    <summary className="cursor-pointer font-medium text-amber-400">
                      해설 보기
                    </summary>
                    <p className="mt-2 leading-relaxed text-muted">
                      {detail.explanation}
                    </p>
                  </details>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </main>
  );
}
