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

type PastEntry = {
  question: Question;
  selectedOption: number | null;
  answerText: string;
  revealed: boolean;
  detail: QuestionDetail | null;
};

function SolvePageContent() {
  const [phase, setPhase] = useState<Phase>("select");
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<Subject | null>(null);
  const [queue, setQueue] = useState<Question[]>([]);
  const [current, setCurrent] = useState<Question | null>(null);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [answerText, setAnswerText] = useState<string>("");
  const [detail, setDetail] = useState<QuestionDetail | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [solvedCount, setSolvedCount] = useState(0);
  const [correctCount, setCorrectCount] = useState(0);
  const [submitError, setSubmitError] = useState<string | null>(null);
  /** 이전 문제로 돌아가기 위한 스택 (브라우저 뒤로가기와 연동) */
  const [pastEntries, setPastEntries] = useState<PastEntry[]>([]);

  useEffect(() => {
    getSubjects().then(setSubjects);
  }, []);

  // 브라우저 뒤로가기 처리: 이전 문제 복원, 첫 문제면 과목 선택으로
  useEffect(() => {
    function onPopState() {
      if (phase !== "solve") return;
      if (pastEntries.length > 0) {
        goPrevious();
      } else {
        // 첫 문제 → 과목 선택 화면으로 (히스토리 엔트리는 이미 popstate로 한 칸 빠져나옴)
        handleReset(false);
      }
    }
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  });

  async function fetchQuestions(subjectId: number): Promise<Question[]> {
    return getQuestions(subjectId, 10);
  }

  function resetCurrentInput() {
    setSelectedOption(null);
    setAnswerText("");
    setRevealed(false);
    setDetail(null);
  }

  async function handleSelectSubject(subject: Subject) {
    setSelectedSubject(subject);
    setLoading(true);
    const qs = await fetchQuestions(subject.id);
    setQueue(qs.slice(1));
    setCurrent(qs[0] || null);
    resetCurrentInput();
    setSolvedCount(0);
    setCorrectCount(0);
    setPastEntries([]);
    setPhase("solve");
    setLoading(false);
    // 히스토리에 solve 진입 마크 (뒤로가기 시 select 로 돌아갈 수 있도록)
    if (typeof window !== "undefined") {
      window.history.pushState({ solve: 0 }, "");
    }
  }

  /** 이전 문제로 복원 — popstate 핸들러에서 호출 */
  function goPrevious() {
    setPastEntries((prev) => {
      if (prev.length === 0) return prev;
      const last = prev[prev.length - 1];
      // 현재 보고 있던 문제를 큐 앞으로 다시 넣음
      if (current) {
        setQueue((q) => [current, ...q]);
      }
      setCurrent(last.question);
      setSelectedOption(last.selectedOption);
      setAnswerText(last.answerText);
      setRevealed(last.revealed);
      setDetail(last.detail);
      return prev.slice(0, -1);
    });
  }

  function handleSelect(option: number) {
    if (revealed || !current) return;
    setSelectedOption(option);
  }

  /** 현재 문제에 대해 응답이 작성됐는지 (제출 가능 여부) */
  function hasAnswer(): boolean {
    if (!current) return false;
    if (current.questionType === "MCQ") return selectedOption !== null;
    return answerText.trim().length > 0;
  }

  /** 단답형/약술형 정답 일치 판정 — 백엔드 채점 결과 대신 클라이언트 측 휴리스틱 */
  function isClientSideCorrect(d: QuestionDetail): boolean {
    if (d.questionType === "MCQ") {
      return selectedOption === d.correctOption;
    }
    const norm = (s: string) => s.trim().toLowerCase().replace(/\s+/g, " ");
    const submitted = norm(answerText);
    if (!submitted) return false;
    if (d.answer && norm(d.answer) === submitted) return true;
    // keywords 중 하나라도 정확히 일치하면 정답
    return d.keywords.some((k) => norm(k) === submitted);
  }

  async function handleSubmit() {
    if (revealed || !current || !hasAnswer()) return;
    setRevealed(true);

    const d = await getQuestionDetail(current.id);
    setDetail(d);
    setSolvedCount((c) => c + 1);
    if (isClientSideCorrect(d)) {
      setCorrectCount((c) => c + 1);
    }

    if (selectedSubject) {
      setSubmitError(null);
      submitSolve({
        subjectId: selectedSubject.id,
        answers: [{
          questionId: current.id,
          selectedOption: current.questionType === "MCQ" ? selectedOption ?? undefined : undefined,
          answerText: current.questionType !== "MCQ" ? answerText : undefined,
        }],
      }).catch((e) => {
        console.error("풀이 제출 실패:", e);
        setSubmitError(
          e instanceof Error ? e.message : "풀이 기록 저장에 실패했습니다. 다음 문제로 넘기기 전에 재시도하세요."
        );
      });
    }
  }

  async function retrySubmit() {
    if (!selectedSubject || !current) return;
    setSubmitError(null);
    try {
      await submitSolve({
        subjectId: selectedSubject.id,
        answers: [{
          questionId: current.id,
          selectedOption: current.questionType === "MCQ" ? selectedOption ?? undefined : undefined,
          answerText: current.questionType !== "MCQ" ? answerText : undefined,
        }],
      });
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : "다시 실패했습니다.");
    }
  }

  async function handleNext() {
    if (!selectedSubject || !current) return;

    let nextQueue = [...queue];

    // 큐가 비면 새로 가져오기
    if (nextQueue.length === 0) {
      setLoading(true);
      nextQueue = await fetchQuestions(selectedSubject.id);
      setLoading(false);
    }

    // 현재 문제를 past 스택에 저장 (브라우저 뒤로가기로 복원 가능)
    setPastEntries((prev) => [
      ...prev,
      {
        question: current,
        selectedOption,
        answerText,
        revealed,
        detail,
      },
    ]);

    setCurrent(nextQueue[0] || null);
    setQueue(nextQueue.slice(1));
    resetCurrentInput();

    // 히스토리 엔트리 추가 (다음 뒤로가기에서 이 문제로 돌아오게)
    if (typeof window !== "undefined") {
      window.history.pushState({ solve: pastEntries.length + 1 }, "");
    }
  }

  /**
   * @param popHistory true면 그동안 push한 히스토리 엔트리를 정리한다 (수동 ← 과목 선택 버튼 클릭).
   *                  false면 popstate 핸들러가 이미 한 칸 뒤로 보낸 후 호출된 것이라 정리 불필요.
   */
  function handleReset(popHistory: boolean = true) {
    if (popHistory && typeof window !== "undefined") {
      const pushed = pastEntries.length + 1; // pastEntries 개수 + handleSelectSubject에서의 1
      if (pushed > 0) {
        window.history.go(-pushed);
        // popstate가 phase=select 시점에서 발생하면 일찍 리턴되므로 안전
      }
    }
    setPhase("select");
    setSelectedSubject(null);
    setQueue([]);
    setCurrent(null);
    setPastEntries([]);
    resetCurrentInput();
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
  const isCorrect = detail ? (
    detail.questionType === "MCQ"
      ? selectedOption === detail.correctOption
      : isClientSideCorrect(detail)
  ) : null;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        {/* 상단 바 */}
        <div className="flex items-center justify-between">
          <button
            onClick={() => handleReset()}
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

        {/* 문제 — 모의고사 응시 페이지와 동일한 카드 구조 */}
        <div className="mt-6 rounded-xl border border-border bg-surface p-6">
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-medium text-muted">{selectedSubject?.name}</p>
            <QuestionTypeBadge type={current.questionType} />
          </div>
          <h2 className="mt-2 text-base font-semibold">문항 {solvedCount + 1}</h2>
          <div className="mt-4">
            <QuestionContent content={parsed.body} />
          </div>

          {/* 입력 UI 분기 */}
          {current.questionType === "MCQ" && (
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
          )}

          {current.questionType === "SHORT_ANSWER" && (
            <div className="mt-6">
              <label className="mb-2 block text-xs text-muted">정답 입력</label>
              <input
                type="text"
                value={answerText}
                onChange={(e) => setAnswerText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !revealed) {
                    e.preventDefault();
                    handleSubmit();
                  }
                }}
                disabled={revealed}
                placeholder="정답을 입력하세요 (엔터: 제출)"
                className="w-full rounded-lg border border-border bg-background px-4 py-3 font-mono text-sm text-foreground placeholder:text-muted/50 transition focus:outline-none focus:ring-2 focus:ring-amber-500/60 disabled:opacity-60"
                autoComplete="off"
                spellCheck={false}
              />
              <p className="mt-2 text-xs text-muted/70">
                대소문자, 앞뒤 공백은 자동으로 무시됩니다.
              </p>
            </div>
          )}

          {current.questionType === "DESCRIPTIVE" && (
            <div className="mt-6">
              <label className="mb-2 block text-xs text-muted">서술형 답안</label>
              <textarea
                value={answerText}
                onChange={(e) => setAnswerText(e.target.value)}
                disabled={revealed}
                rows={6}
                placeholder="개념을 설명하는 답안을 작성하세요. 핵심 키워드를 포함할수록 점수가 올라갑니다."
                className="w-full resize-y rounded-lg border border-border bg-background px-4 py-3 text-sm leading-relaxed text-foreground placeholder:text-muted/50 transition focus:outline-none focus:ring-2 focus:ring-amber-500/60 disabled:opacity-60"
              />
              <p className="mt-2 text-xs text-muted/70">
                {answerText.length}자 · 채점은 모범답안과 키워드 일치율로 이루어집니다.
              </p>
            </div>
          )}
        </div>

        {/* 제출 버튼 (미공개 상태) */}
        {!revealed && (
          <div className="mt-4 flex justify-center">
            <button
              onClick={handleSubmit}
              disabled={!hasAnswer()}
              className="rounded-lg bg-primary px-6 py-2.5 text-sm font-semibold text-zinc-900 transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-primary"
            >
              정답 제출
            </button>
          </div>
        )}

        {/* 정답 확인 후 해설 + 다음 버튼 */}
        {revealed && detail && (
          <div className="mt-4 space-y-4">
            {/* 제출 실패 에러 배너 */}
            {submitError && (
              <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                <div className="flex items-center justify-between gap-3">
                  <span>풀이 기록 저장 실패: {submitError}</span>
                  <button
                    onClick={retrySubmit}
                    className="shrink-0 rounded-md border border-red-500/60 px-3 py-1 text-xs font-medium hover:bg-red-500/20"
                  >
                    재시도
                  </button>
                </div>
              </div>
            )}

            {/* 결과 표시 */}
            <div
              className={`rounded-lg border px-4 py-3 text-sm ${
                isCorrect
                  ? "border-green-500/30 bg-green-500/5 text-green-400"
                  : "border-red-500/30 bg-red-500/5 text-red-400"
              }`}
            >
              {isCorrect
                ? "정답입니다!"
                : detail.questionType === "MCQ"
                ? `오답입니다. 정답은 ${detail.correctOption}번입니다.`
                : `오답입니다. 모범답안: ${detail.answer ?? "(없음)"}`}
            </div>

            {/* 비-MCQ 정답 패널 */}
            {detail.questionType !== "MCQ" && (
              <div className="rounded-lg border border-border bg-surface px-4 py-3">
                <p className="text-xs font-medium text-muted">모범답안</p>
                <p className="mt-1 font-mono text-sm text-foreground">{detail.answer ?? "-"}</p>
                {detail.keywords.length > 0 && (
                  <div className="mt-3">
                    <p className="text-xs font-medium text-muted">
                      {detail.questionType === "SHORT_ANSWER" ? "허용 표기" : "채점 키워드"}
                    </p>
                    <div className="mt-1 flex flex-wrap gap-1.5">
                      {detail.keywords.map((kw, i) => (
                        <span
                          key={i}
                          className="rounded bg-emerald-500/10 px-2 py-0.5 text-[11px] text-emerald-300"
                        >
                          {kw}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* 해설 */}
            {detail.explanation && (
              <div className="rounded-lg border border-border bg-surface px-4 py-3">
                <p className="text-sm font-medium text-amber-400">해설</p>
                <div className="mt-1 text-sm leading-relaxed text-muted">
                  <QuestionContent content={detail.explanation} />
                </div>
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

function QuestionTypeBadge({ type }: { type: Question["questionType"] }) {
  if (type === "MCQ") {
    return <span className="rounded-full border border-border px-2 py-0.5 text-[10px] font-medium text-muted">4지선다</span>;
  }
  if (type === "SHORT_ANSWER") {
    return <span className="rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold text-emerald-300">단답형</span>;
  }
  return <span className="rounded-full border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[10px] font-bold text-cyan-300">서술형</span>;
}
