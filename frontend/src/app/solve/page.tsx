"use client";

import { useEffect, useState } from "react";
import {
  getSubjects,
  getQuestions,
  getQuestionDetail,
  submitSolve,
  type Subject,
  type Question,
  type QuestionDetail,
} from "@/lib/api";
import { parseQuestion } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";

type Phase = "select" | "solve";

export default function SolvePage() {
  return (
    <AuthGuard>
      <SolvePageContent />
    </AuthGuard>
  );
}

function SolvePageContent() {
  const [phase, setPhase] = useState<Phase>("select");
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<Subject | null>(null);
  const [queue, setQueue] = useState<Question[]>([]);
  const [current, setCurrent] = useState<Question | null>(null);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [detail, setDetail] = useState<QuestionDetail | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [solvedCount, setSolvedCount] = useState(0);
  const [correctCount, setCorrectCount] = useState(0);

  useEffect(() => {
    getSubjects().then(setSubjects);
  }, []);

  async function fetchQuestions(subjectId: number): Promise<Question[]> {
    return getQuestions(subjectId, 10);
  }

  async function handleSelectSubject(subject: Subject) {
    setSelectedSubject(subject);
    setLoading(true);
    const qs = await fetchQuestions(subject.id);
    setQueue(qs.slice(1));
    setCurrent(qs[0] || null);
    setSelectedOption(null);
    setRevealed(false);
    setDetail(null);
    setSolvedCount(0);
    setCorrectCount(0);
    setPhase("solve");
    setLoading(false);
  }

  async function handleSelect(option: number) {
    if (revealed || !current) return;
    setSelectedOption(option);
    setRevealed(true);

    const d = await getQuestionDetail(current.id);
    setDetail(d);
    setSolvedCount((c) => c + 1);
    if (option === d.correctOption) {
      setCorrectCount((c) => c + 1);
    }

    if (selectedSubject) {
      submitSolve({
        subjectId: selectedSubject.id,
        answers: [{ questionId: current.id, selectedOption: option }],
      }).catch((e) => console.error("풀이 제출 실패:", e));
    }
  }

  async function handleNext() {
    if (!selectedSubject) return;

    let nextQueue = [...queue];

    // 큐가 비면 새로 가져오기
    if (nextQueue.length === 0) {
      setLoading(true);
      nextQueue = await fetchQuestions(selectedSubject.id);
      setLoading(false);
    }

    setCurrent(nextQueue[0] || null);
    setQueue(nextQueue.slice(1));
    setSelectedOption(null);
    setRevealed(false);
    setDetail(null);
  }

  function handleReset() {
    setPhase("select");
    setSelectedSubject(null);
    setQueue([]);
    setCurrent(null);
    setSelectedOption(null);
    setRevealed(false);
    setDetail(null);
    setSolvedCount(0);
    setCorrectCount(0);
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <Spinner message="문제를 불러오는 중..." />
      </main>
    );
  }

  // 과목 선택
  if (phase === "select") {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">과목 선택</h1>
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

          {subjects.length === 0 && (
            <p className="mt-8 text-center text-muted">
              과목 데이터를 불러오는 중...
            </p>
          )}
        </div>
      </main>
    );
  }

  // 문제 풀기
  if (!current) return null;

  const parsed = parseQuestion(current.content);
  const isCorrect = detail ? selectedOption === detail.correctOption : null;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        {/* 상단 바 */}
        <div className="flex items-center justify-between">
          <button
            onClick={handleReset}
            className="text-sm text-muted hover:text-foreground transition-colors"
          >
            &larr; 과목 선택
          </button>
          <div className="flex items-center gap-3">
            <span className="text-sm text-muted">
              {selectedSubject?.name}
            </span>
            {solvedCount > 0 && (
              <span className="rounded bg-surface border border-border px-2 py-0.5 text-xs text-muted">
                {correctCount}/{solvedCount} 정답
              </span>
            )}
          </div>
        </div>

        {/* 문제 */}
        <div className="mt-8 rounded-xl border border-border bg-surface p-6">
          <QuestionContent segments={parsed.segments} />

          <ul className="mt-6 space-y-2">
            {parsed.options.map((optionText, idx) => {
              const num = idx + 1;
              const isSelected = selectedOption === num;
              const isCorrectOption = detail?.correctOption === num;

              let style = "border-border hover:border-amber-500/40 hover:bg-amber-500/5";
              if (revealed) {
                if (isCorrectOption) {
                  style = "border-green-500 bg-green-500/10 text-green-400";
                } else if (isSelected && !isCorrectOption) {
                  style = "border-red-500 bg-red-500/10 text-red-400";
                } else {
                  style = "border-border opacity-50";
                }
              } else if (isSelected) {
                style = "border-amber-500 bg-amber-500/10 text-foreground";
              }

              return (
                <li key={num}>
                  <button
                    onClick={() => handleSelect(num)}
                    disabled={revealed}
                    className={`w-full rounded-lg border px-4 py-3 text-left text-sm transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 ${style} disabled:cursor-default`}
                  >
                    <span className="mr-3 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-current text-xs">
                      {num}
                    </span>
                    {optionText}
                    {revealed && isCorrectOption && (
                      <span className="ml-2 text-green-400">&#10003;</span>
                    )}
                    {revealed && isSelected && !isCorrectOption && (
                      <span className="ml-2 text-red-400">&#10007;</span>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        </div>

        {/* 정답 확인 후 해설 + 다음 버튼 */}
        {revealed && detail && (
          <div className="mt-4 space-y-4">
            {/* 결과 표시 */}
            <div
              className={`rounded-lg border px-4 py-3 text-sm ${
                isCorrect
                  ? "border-green-500/30 bg-green-500/5 text-green-400"
                  : "border-red-500/30 bg-red-500/5 text-red-400"
              }`}
            >
              {isCorrect ? "정답입니다!" : `오답입니다. 정답은 ${detail.correctOption}번입니다.`}
            </div>

            {/* 해설 */}
            {detail.explanation && (
              <div className="rounded-lg border border-border bg-surface px-4 py-3">
                <p className="text-sm font-medium text-amber-400">해설</p>
                <p className="mt-1 text-sm leading-relaxed text-muted">
                  {detail.explanation}
                </p>
              </div>
            )}

            {/* 다음 문제 */}
            <div className="flex justify-center">
              <button
                onClick={handleNext}
                className="rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover"
              >
                다음 문제
              </button>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
