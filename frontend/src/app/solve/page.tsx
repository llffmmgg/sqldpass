"use client";

import { useEffect, useState } from "react";
import {
  getSubjects,
  getQuestions,
  submitSolve,
  type Subject,
  type Question,
  type SolveResponse,
} from "@/lib/api";

type Phase = "select" | "solve" | "result";

export default function SolvePage() {
  const [phase, setPhase] = useState<Phase>("select");
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<Subject | null>(null);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [result, setResult] = useState<SolveResponse | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getSubjects().then(setSubjects);
  }, []);

  const leafSubjects = subjects.flatMap((s) =>
    s.children.length > 0 ? s.children : [s]
  );

  async function handleSelectSubject(subject: Subject) {
    setSelectedSubject(subject);
    setLoading(true);
    const qs = await getQuestions(subject.id, 10);
    setQuestions(qs);
    setAnswers({});
    setCurrentIndex(0);
    setPhase("solve");
    setLoading(false);
  }

  function handleSelectOption(questionId: number, option: number) {
    setAnswers((prev) => ({ ...prev, [questionId]: option }));
  }

  async function handleSubmit() {
    if (!selectedSubject) return;
    setLoading(true);
    const solveAnswers = questions.map((q) => ({
      questionId: q.id,
      selectedOption: answers[q.id] || 0,
    }));
    const res = await submitSolve(1, {
      subjectId: selectedSubject.id,
      answers: solveAnswers,
    });
    setResult(res);
    setPhase("result");
    setLoading(false);
  }

  function handleReset() {
    setPhase("select");
    setSelectedSubject(null);
    setQuestions([]);
    setAnswers({});
    setResult(null);
    setCurrentIndex(0);
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <p className="text-muted">로딩 중...</p>
      </main>
    );
  }

  // 과목 선택
  if (phase === "select") {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <h1 className="text-2xl font-bold sm:text-3xl">과목 선택</h1>
          <p className="mt-2 text-muted">풀고 싶은 과목을 선택하세요</p>

          <div className="mt-8 space-y-3">
            {subjects.map((parent) => (
              <div key={parent.id}>
                <h2 className="text-sm font-medium text-muted mb-2">
                  {parent.name}
                </h2>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  {(parent.children.length > 0
                    ? parent.children
                    : [parent]
                  ).map((child) => (
                    <button
                      key={child.id}
                      onClick={() => handleSelectSubject(child)}
                      className="rounded-lg border border-border bg-surface px-4 py-3 text-left text-sm font-medium transition-all duration-300 hover:-translate-y-0.5 hover:border-amber-500/40 hover:shadow-[0_0_16px_var(--glow)]"
                    >
                      {child.name}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {leafSubjects.length === 0 && (
            <p className="mt-8 text-center text-muted">
              과목 데이터를 불러오는 중...
            </p>
          )}
        </div>
      </main>
    );
  }

  // 문제 풀기
  if (phase === "solve") {
    const question = questions[currentIndex];
    if (!question) return null;

    const lines = question.content.split("\n");
    const allAnswered = questions.every((q) => answers[q.id] !== undefined);

    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          {/* 진행 상태 */}
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted">
              {selectedSubject?.name}
            </span>
            <span className="text-sm text-muted">
              {currentIndex + 1} / {questions.length}
            </span>
          </div>

          <div className="mt-2 h-1 rounded-full bg-border">
            <div
              className="h-full rounded-full bg-primary transition-all duration-300"
              style={{
                width: `${((currentIndex + 1) / questions.length) * 100}%`,
              }}
            />
          </div>

          {/* 문제 */}
          <div className="mt-8 rounded-xl border border-border bg-surface p-6">
            <p className="font-medium leading-relaxed whitespace-pre-wrap">
              {lines[0]}
            </p>

            {/* 선택지 (content에서 1. 2. 3. 4. 패턴 파싱) */}
            <ul className="mt-6 space-y-2">
              {[1, 2, 3, 4].map((num) => {
                const optionLine = lines.find((l) =>
                  l.trimStart().startsWith(`${num}.`)
                );
                const optionText =
                  optionLine?.replace(/^\s*\d+\.\s*/, "") || `선택지 ${num}`;
                const isSelected = answers[question.id] === num;

                return (
                  <li key={num}>
                    <button
                      onClick={() => handleSelectOption(question.id, num)}
                      className={`w-full rounded-lg border px-4 py-3 text-left text-sm transition-all duration-200 ${
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

          {/* 네비게이션 */}
          <div className="mt-6 flex items-center justify-between">
            <button
              onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
              disabled={currentIndex === 0}
              className="rounded-lg border border-border px-4 py-2 text-sm transition hover:bg-surface disabled:opacity-30"
            >
              이전
            </button>

            {currentIndex < questions.length - 1 ? (
              <button
                onClick={() => setCurrentIndex((i) => i + 1)}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover"
              >
                다음
              </button>
            ) : (
              <button
                onClick={handleSubmit}
                disabled={!allAnswered}
                className="btn-glow rounded-lg bg-primary px-6 py-2 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:opacity-30 disabled:animate-none disabled:shadow-none"
              >
                제출하기
              </button>
            )}
          </div>

          {/* 문제 번호 바로가기 */}
          <div className="mt-6 flex flex-wrap gap-2">
            {questions.map((q, i) => (
              <button
                key={q.id}
                onClick={() => setCurrentIndex(i)}
                className={`h-8 w-8 rounded text-xs font-medium transition ${
                  i === currentIndex
                    ? "bg-primary text-zinc-900"
                    : answers[q.id] !== undefined
                      ? "bg-amber-500/20 text-amber-400 border border-amber-500/30"
                      : "bg-surface border border-border text-muted"
                }`}
              >
                {i + 1}
              </button>
            ))}
          </div>
        </div>
      </main>
    );
  }

  // 결과
  if (phase === "result" && result) {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <div className="text-center">
            <h1 className="text-3xl font-bold">채점 결과</h1>
            <div className="mt-6 inline-flex items-baseline gap-1">
              <span className="text-6xl font-bold bg-gradient-to-r from-amber-400 to-amber-300 bg-clip-text text-transparent">
                {result.score}
              </span>
              <span className="text-2xl text-muted">점</span>
            </div>
            <p className="mt-2 text-muted">
              {result.totalCount}문제 중 {result.correctCount}문제 정답
            </p>
          </div>

          <div className="mt-10 space-y-3">
            {result.answers.map((a, i) => (
              <div
                key={a.questionId}
                className={`rounded-lg border px-4 py-3 text-sm ${
                  a.correct
                    ? "border-green-500/30 bg-green-500/5"
                    : "border-red-500/30 bg-red-500/5"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span>
                    문제 {i + 1}
                  </span>
                  <span className={a.correct ? "text-green-400" : "text-red-400"}>
                    {a.correct ? "정답" : `오답 (선택: ${a.selectedOption}, 정답: ${a.correctOption})`}
                  </span>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-8 flex justify-center gap-4">
            <button
              onClick={handleReset}
              className="rounded-lg border border-border px-6 py-3 text-sm font-semibold transition hover:bg-surface"
            >
              다시 풀기
            </button>
            <a
              href="/"
              className="rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover"
            >
              홈으로
            </a>
          </div>
        </div>
      </main>
    );
  }

  return null;
}
