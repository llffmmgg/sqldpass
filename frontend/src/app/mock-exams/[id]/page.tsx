"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import QuestionContent from "@/components/QuestionContent";
import { parseQuestion } from "@/lib/parseQuestion";
import { getMockExam, type MockExamDetail } from "@/lib/mockExamApi";
import { submitSolve, type SolveResponse } from "@/lib/api";

export default function MockExamDetailPage() {
  return (
    <AuthGuard>
      <MockExamDetailContent />
    </AuthGuard>
  );
}

function MockExamDetailContent() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params?.id);

  const [exam, setExam] = useState<MockExamDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [answers, setAnswers] = useState<Map<number, number>>(new Map());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SolveResponse | null>(null);

  useEffect(() => {
    if (!id) return;
    getMockExam(id)
      .then(setExam)
      .catch((e) => setError(e instanceof Error ? e.message : "모의고사를 불러올 수 없습니다."));
  }, [id]);

  if (error) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-400">{error}</p>
          <button
            onClick={() => router.push("/mock-exams")}
            className="mt-4 text-sm text-muted hover:text-foreground"
          >
            ← 모의고사 목록으로
          </button>
        </div>
      </main>
    );
  }

  if (!exam) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <Spinner message="모의고사 불러오는 중..." />
      </main>
    );
  }

  // 결과 화면
  if (result) {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <h1 className="text-2xl font-bold">{exam.name} 결과</h1>
          <div className="mt-8 rounded-xl border border-border bg-surface p-8 text-center">
            <p className="text-sm text-muted">점수</p>
            <p className="mt-2 text-5xl font-bold text-primary">{result.score}점</p>
            <p className="mt-4 text-sm text-muted">
              {result.correctCount} / {result.totalCount} 정답
            </p>
          </div>
          <div className="mt-6 flex gap-3">
            <button
              onClick={() => router.push("/mock-exams")}
              className="flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-muted hover:text-foreground"
            >
              목록으로
            </button>
            <button
              onClick={() => router.push(`/history/${result.id}`)}
              className="flex-1 rounded-lg bg-primary py-3 text-sm font-semibold text-zinc-900 hover:bg-primary-hover"
            >
              상세 보기
            </button>
          </div>
        </div>
      </main>
    );
  }

  const total = exam.questions.length;
  const current = exam.questions[currentIdx];
  const parsed = parseQuestion(current.content);
  const selectedOption = answers.get(current.id) ?? null;
  const answeredCount = answers.size;

  function selectOption(opt: number) {
    const next = new Map(answers);
    next.set(current.id, opt);
    setAnswers(next);
  }

  function goPrev() {
    if (currentIdx > 0) setCurrentIdx(currentIdx - 1);
  }

  function goNext() {
    if (currentIdx < total - 1) setCurrentIdx(currentIdx + 1);
  }

  async function handleSubmit() {
    if (!exam) return;
    if (answeredCount < total) {
      const ok = confirm(
        `아직 ${total - answeredCount}문제 풀지 않았습니다. 그대로 제출할까요?`
      );
      if (!ok) return;
    }

    setSubmitting(true);
    try {
      const payload = {
        mockExamId: exam.id,
        answers: exam.questions.map((q) => ({
          questionId: q.id,
          selectedOption: answers.get(q.id) ?? 0,
        })),
      };
      const res = await submitSolve(payload);
      setResult(res);
    } catch (e) {
      alert(e instanceof Error ? e.message : "제출에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
        {/* 상단 진행률 */}
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted">{exam.name}</span>
          <span className="font-medium">
            {currentIdx + 1} / {total} ({answeredCount} 답안)
          </span>
        </div>
        <div className="mt-2 h-1 overflow-hidden rounded-full bg-border">
          <div
            className="h-full bg-primary transition-all"
            style={{ width: `${((currentIdx + 1) / total) * 100}%` }}
          />
        </div>

        {/* 문제 */}
        <div className="mt-6 rounded-xl border border-border bg-surface p-6">
          <p className="text-xs font-medium text-muted">{current.subjectName}</p>
          <h2 className="mt-2 text-base font-semibold">
            문항 {currentIdx + 1}
          </h2>
          <div className="mt-4">
            <QuestionContent segments={parsed.segments} />
          </div>

          {/* 4지선다 */}
          <ul className="mt-6 space-y-2">
            {parsed.options.map((optionText, idx) => {
              const num = idx + 1;
              const isSelected = selectedOption === num;
              return (
                <li key={num}>
                  <button
                    onClick={() => selectOption(num)}
                    className={`w-full rounded-lg border px-4 py-3 text-left text-sm transition ${
                      isSelected
                        ? "border-amber-500 bg-amber-500/10 text-foreground"
                        : "border-border hover:border-amber-500/40 hover:bg-amber-500/5"
                    }`}
                  >
                    <span className="mr-3 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-current text-xs">
                      {num}
                    </span>
                    {optionText}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>

        {/* 이동 + 제출 */}
        <div className="mt-6 flex gap-3">
          <button
            onClick={goPrev}
            disabled={currentIdx === 0}
            className="flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-muted disabled:opacity-30 hover:text-foreground"
          >
            ← 이전
          </button>
          {currentIdx < total - 1 ? (
            <button
              onClick={goNext}
              className="flex-1 rounded-lg bg-primary py-3 text-sm font-semibold text-zinc-900 hover:bg-primary-hover"
            >
              다음 →
            </button>
          ) : (
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="flex-1 rounded-lg bg-primary py-3 text-sm font-semibold text-zinc-900 hover:bg-primary-hover disabled:opacity-50"
            >
              {submitting ? "제출 중..." : "제출"}
            </button>
          )}
        </div>

        {/* 빠른 이동 점프 */}
        <div className="mt-6">
          <p className="text-xs text-muted mb-2">빠른 이동</p>
          <div className="grid grid-cols-10 gap-1.5">
            {exam.questions.map((q, i) => {
              const answered = answers.has(q.id);
              return (
                <button
                  key={q.id}
                  onClick={() => setCurrentIdx(i)}
                  className={`h-8 rounded text-xs font-medium transition ${
                    i === currentIdx
                      ? "bg-primary text-zinc-900"
                      : answered
                      ? "bg-primary/20 text-foreground"
                      : "bg-surface text-muted hover:bg-border"
                  }`}
                >
                  {i + 1}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </main>
  );
}
