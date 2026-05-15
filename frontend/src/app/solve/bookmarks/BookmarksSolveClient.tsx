"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import BookmarkButton from "@/components/BookmarkButton";
import QuestionContent from "@/components/QuestionContent";
import ReportQuestionButton from "@/components/ReportQuestionButton";
import Spinner from "@/components/Spinner";
import { useToast } from "@/components/Toast";
import { Badge, Button, Card, Container } from "@/components/ui";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  getBookmarks,
  getQuestionDetail,
  submitSolve,
  type BookmarkResponse,
  type Question,
  type QuestionDetail,
  type QuestionType,
} from "@/lib/api";
import { hapticError, hapticLight, hapticSuccess } from "@/lib/haptic";

type Phase = "pre" | "solve" | "session-complete";

type PracticeQuestion = {
  questionId: number;
  questionType: QuestionType;
  body: string;
  options: string[];
  subjectName: string;
};

type PastEntry = {
  question: PracticeQuestion;
  selectedOption: number | null;
  answerText: string;
  revealed: boolean;
  detail: QuestionDetail | null;
};

const SET_SIZE = 10;

function shuffle<T>(arr: T[]): T[] {
  const copy = arr.slice();
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function toPracticeQuestion(b: BookmarkResponse): PracticeQuestion {
  const parsed = parseQuestion(b.questionContent);
  return {
    questionId: b.questionId,
    questionType: b.questionType,
    body: parsed.body,
    options: parsed.options,
    subjectName: b.subjectName,
  };
}

function isClientSideCorrectStatic(
  d: QuestionDetail,
  selectedOption: number | null,
  answerText: string,
): boolean {
  if (d.questionType === "MCQ") {
    return selectedOption === d.correctOption;
  }
  const norm = (s: string) => s.trim().toLowerCase().replace(/\s+/g, " ");
  const submitted = norm(answerText);
  if (!submitted) return false;
  if (d.answer && norm(d.answer) === submitted) return true;
  return d.keywords.some((k) => norm(k) === submitted);
}

export default function BookmarksSolveClient() {
  const toast = useToast();

  const [phase, setPhase] = useState<Phase>("pre");
  const [bookmarks, setBookmarks] = useState<BookmarkResponse[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [sessionQuestions, setSessionQuestions] = useState<PracticeQuestion[]>([]);
  const [queue, setQueue] = useState<PracticeQuestion[]>([]);
  const [current, setCurrent] = useState<PracticeQuestion | null>(null);
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [answerText, setAnswerText] = useState("");
  const [detail, setDetail] = useState<QuestionDetail | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [pastEntries, setPastEntries] = useState<PastEntry[]>([]);
  const [solvedCount, setSolvedCount] = useState(0);
  const [correctCount, setCorrectCount] = useState(0);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!isLoggedIn()) {
      try {
        sessionStorage.setItem("postLoginRedirect", "/solve/bookmarks");
      } catch {
        // sessionStorage 사용 불가 환경 — 홈으로 복귀
      }
      window.location.href = getGoogleLoginUrl();
      return;
    }
    getBookmarks()
      .then((res) => setBookmarks(res.items))
      .catch((e) =>
        setLoadError(e instanceof Error ? e.message : "즐겨찾기를 불러오지 못했어요."),
      );
  }, []);

  // 키보드 단축키 (1-4 옵션, Enter 제출/다음)
  useEffect(() => {
    if (phase !== "solve" || !current) return;
    function onKey(e: KeyboardEvent) {
      const tag = (document.activeElement?.tagName || "").toLowerCase();
      if (tag === "input" || tag === "textarea") return;
      if (current?.questionType === "MCQ" && !revealed) {
        if (e.key >= "1" && e.key <= "4") {
          handleSelect(Number(e.key));
          return;
        }
      }
      if (e.key === "Enter") {
        e.preventDefault();
        if (revealed) {
          handleNext();
        } else if (hasAnswer()) {
          handleSubmit();
        }
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, current, revealed, selectedOption, answerText]);

  function startSessionWithQuestions(qs: PracticeQuestion[]) {
    setSessionQuestions(qs);
    setQueue(qs.slice(1));
    setCurrent(qs[0] ?? null);
    setSelectedOption(null);
    setAnswerText("");
    setDetail(null);
    setRevealed(false);
    setPastEntries([]);
    setSolvedCount(0);
    setCorrectCount(0);
    setSubmitError(null);
    setPhase("solve");
  }

  function startSession() {
    if (!bookmarks || bookmarks.length === 0) return;
    const picked = shuffle(bookmarks).slice(0, SET_SIZE).map(toPracticeQuestion);
    startSessionWithQuestions(picked);
  }

  function replaySameSession() {
    if (sessionQuestions.length === 0) return;
    startSessionWithQuestions(sessionQuestions);
  }

  function newRandomSession() {
    if (!bookmarks || bookmarks.length === 0) return;
    const picked = shuffle(bookmarks).slice(0, SET_SIZE).map(toPracticeQuestion);
    startSessionWithQuestions(picked);
  }

  function handleSelect(option: number) {
    if (revealed || !current) return;
    setSelectedOption(option);
    hapticLight();
  }

  function hasAnswer(): boolean {
    if (!current) return false;
    if (current.questionType === "MCQ") return selectedOption !== null;
    return answerText.trim().length > 0;
  }

  function goPrevious() {
    setPastEntries((prev) => {
      if (prev.length === 0) return prev;
      const last = prev[prev.length - 1];
      if (current) {
        setQueue((q) => [current, ...q]);
      }
      setCurrent(last.question);
      setSelectedOption(last.selectedOption);
      setAnswerText(last.answerText);
      setRevealed(last.revealed);
      setDetail(last.detail);
      // solvedCount 는 정답 카운트와 함께 1 감소
      setSolvedCount((c) => Math.max(0, c - 1));
      if (last.detail && isClientSideCorrectStatic(last.detail, last.selectedOption, last.answerText)) {
        setCorrectCount((c) => Math.max(0, c - 1));
      }
      return prev.slice(0, -1);
    });
  }

  async function handleSubmit() {
    if (!current || revealed || !hasAnswer()) return;
    try {
      const d = await getQuestionDetail(current.questionId);
      setDetail(d);
      setRevealed(true);
      setSolvedCount((c) => c + 1);
      const ok = isClientSideCorrectStatic(d, selectedOption, answerText);
      if (ok) {
        setCorrectCount((c) => c + 1);
        hapticSuccess();
      } else {
        hapticError();
      }
    } catch (e) {
      toast.show(
        e instanceof Error ? e.message : "정답 정보를 가져오지 못했어요.",
        "error",
      );
    }
  }

  async function handleNext() {
    if (!current || !detail) return;
    const entry: PastEntry = {
      question: current,
      selectedOption,
      answerText,
      revealed,
      detail,
    };
    const nextPast = [...pastEntries, entry];
    setPastEntries(nextPast);

    if (queue.length === 0) {
      await persistSession(nextPast);
      setPhase("session-complete");
      return;
    }
    const [head, ...rest] = queue;
    setCurrent(head);
    setQueue(rest);
    setSelectedOption(null);
    setAnswerText("");
    setDetail(null);
    setRevealed(false);
  }

  async function persistSession(entries: PastEntry[]) {
    if (entries.length === 0) return;
    try {
      const res = await submitSolve({
        source: "BOOKMARK",
        answers: entries.map((e) => ({
          questionId: e.question.questionId,
          selectedOption:
            e.question.questionType === "MCQ" ? e.selectedOption ?? undefined : undefined,
          answerText:
            e.question.questionType !== "MCQ" ? e.answerText || undefined : undefined,
        })),
      });
      if (res.milestoneReached) {
        toast.show(`🎉 ${res.milestoneReached}일 연속 학습! 잘하고 있어요`, "success");
      }
    } catch (e) {
      setSubmitError(
        e instanceof Error
          ? e.message
          : "풀이 기록 저장에 실패했어요.",
      );
    }
  }

  async function retrySubmit() {
    if (pastEntries.length === 0) return;
    setSubmitError(null);
    try {
      await submitSolve({
        source: "BOOKMARK",
        answers: pastEntries.map((e) => ({
          questionId: e.question.questionId,
          selectedOption:
            e.question.questionType === "MCQ" ? e.selectedOption ?? undefined : undefined,
          answerText:
            e.question.questionType !== "MCQ" ? e.answerText || undefined : undefined,
        })),
      });
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : "다시 실패했어요.");
    }
  }

  // ──────────── 렌더링 ────────────

  if (loadError) {
    return (
      <Container size="narrow" className="py-16">
        <Card padding="lg" className="text-center">
          <h1 className="text-xl font-semibold">즐겨찾기를 불러오지 못했어요</h1>
          <p className="mt-3 text-sm text-text-muted">{loadError}</p>
          <Link
            href="/wrong-answers?tab=bookmark"
            className="mt-6 inline-block rounded-lg border border-border bg-surface px-4 py-2 text-sm transition-colors hover:border-primary/40"
          >
            즐겨찾기로 →
          </Link>
        </Card>
      </Container>
    );
  }

  if (bookmarks === null) {
    return (
      <section className="flex min-h-[40vh] items-center justify-center">
        <Spinner message="즐겨찾기 불러오는 중..." />
      </section>
    );
  }

  // ── Pre-session ────────────────────────────────────
  if (phase === "pre") {
    const count = bookmarks.length;
    const sessionSize = Math.min(count, SET_SIZE);
    return (
      <Container size="narrow" className="py-16">
        <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">즐겨찾기 모아 풀기</h2>
        <p className="mt-2 text-sm text-text-muted">
          즐겨찾기 한 문제들을 매번 다른 순서·다른 조합으로 풀어요. 오답은 오답노트에 자동으로 누적됩니다.
        </p>

        {count === 0 ? (
          <Card padding="lg" className="mt-8 text-center">
            <p className="text-sm text-text-muted">
              아직 즐겨찾기 한 문제가 없어요. 풀이 중 별표를 눌러 즐겨찾기를 추가하면 여기서 모아서 풀 수 있어요.
            </p>
            <div className="mt-6">
              <Link
                href="/solve"
                className="inline-block rounded-lg border border-border bg-surface px-5 py-2.5 text-sm transition-colors hover:border-primary/40"
              >
                문제 풀기로 →
              </Link>
            </div>
          </Card>
        ) : (
          <Card padding="lg" className="mt-8">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-primary">한 세션</p>
            <h3 className="mt-2 text-3xl font-bold tracking-tight tabular-nums">
              {sessionSize}<span className="text-text-subtle">문제</span>
            </h3>
            <p className="mt-2 text-sm text-text-muted">
              즐겨찾기 {count}개에서 무작위로 {sessionSize}문제를 뽑아요.
            </p>
            <div className="mt-6 flex flex-wrap items-center gap-3">
              <Button variant="primary" size="lg" onClick={startSession}>
                시작하기
              </Button>
              <Link
                href="/wrong-answers?tab=bookmark"
                className="text-sm text-text-muted transition-colors hover:text-text"
              >
                즐겨찾기 목록 →
              </Link>
            </div>
          </Card>
        )}
      </Container>
    );
  }

  // ── 세션 종료 카드 ───────────────────────────────────
  if (phase === "session-complete") {
    const totalSolved = solvedCount > 0 ? solvedCount : pastEntries.length;
    const rate = totalSolved > 0 ? Math.round((correctCount / totalSolved) * 100) : 0;
    const ment =
      rate >= 90
        ? "완벽해요! 새 조합으로 한 번 더 굳혀볼까요?"
        : rate >= 70
          ? "잘하고 있어요. 약한 문제만 다시 골라봐도 좋아요."
          : "괜찮아요. 오답은 오답노트에 자동으로 들어갔어요.";
    const rateColor = rate >= 90 ? "text-success" : rate >= 70 ? "text-warning" : "text-danger";

    return (
      <Container size="narrow" className="py-16">
        <div className="rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/[0.08] via-primary/[0.04] to-transparent p-8 sm:p-10">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-primary">세션 완료</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">즐겨찾기 모아 풀기</h2>

          <div className="mt-6 flex items-end gap-4">
            <span className={`text-6xl font-bold tabular-nums sm:text-7xl ${rateColor}`}>
              {correctCount}
              <span className="text-3xl text-text-subtle">/{totalSolved}</span>
            </span>
            <span className={`mb-2 text-2xl font-bold tabular-nums ${rateColor}`}>{rate}%</span>
          </div>

          <p className="mt-4 text-sm leading-relaxed text-text-muted">{ment}</p>

          {submitError && (
            <div className="mt-6 rounded-lg border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-danger">
              <div className="flex items-center justify-between gap-3">
                <span>풀이 기록 저장 실패: {submitError}</span>
                <Button variant="danger" size="sm" onClick={retrySubmit}>
                  재시도
                </Button>
              </div>
            </div>
          )}

          <div className="mt-8 grid grid-cols-1 gap-2 sm:grid-cols-2">
            <Button
              variant="outline"
              size="lg"
              onClick={replaySameSession}
              disabled={sessionQuestions.length === 0}
              className="!justify-between text-left"
              rightIcon={
                <svg
                  className="h-5 w-5 shrink-0 transition-transform group-hover:rotate-180"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2.2}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  />
                </svg>
              }
            >
              <div className="min-w-0 flex-1 text-left">
                <p className="text-sm font-semibold">같은 10문제 다시</p>
                <p className="mt-0.5 text-xs text-text-muted">방금 푼 문제로 약점 굳히기</p>
              </div>
            </Button>
            <Button
              variant="primary"
              size="lg"
              onClick={newRandomSession}
              className="!justify-between text-left"
              rightIcon={
                <svg
                  className="h-5 w-5 shrink-0"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2.5}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M13 7l5 5m0 0l-5 5m5-5H6"
                  />
                </svg>
              }
            >
              <div className="min-w-0 flex-1 text-left">
                <p className="text-sm font-semibold">새 10문제</p>
                <p className="mt-0.5 text-xs opacity-80">다른 랜덤 조합으로 한 세트 더</p>
              </div>
            </Button>
          </div>

          <div className="mt-5 flex flex-wrap items-center gap-5 text-sm text-text-muted">
            <Link href="/wrong-answers?tab=bookmark" className="transition-colors hover:text-text">
              즐겨찾기 목록 →
            </Link>
            <Link href="/wrong-answers" className="transition-colors hover:text-text">
              오답노트 →
            </Link>
          </div>
        </div>

        <SessionReviewList entries={pastEntries} />
      </Container>
    );
  }

  // ── 문제 풀이 ────────────────────────────────────────
  if (!current) return null;

  const isCorrect = detail
    ? isClientSideCorrectStatic(detail, selectedOption, answerText)
    : null;

  const currentNumber = Math.min(solvedCount + (revealed ? 0 : 1), sessionQuestions.length);
  const progressPct = sessionQuestions.length > 0
    ? Math.min((solvedCount / sessionQuestions.length) * 100, 100)
    : 0;

  return (
    <section className="bg-bg pb-40 text-text lg:pb-24">
      <Container size="narrow" className="py-10">
        <div className="flex items-center justify-between gap-3">
          <Link
            href="/wrong-answers?tab=bookmark"
            className="text-sm text-text-muted transition-colors hover:text-text"
          >
            ← 즐겨찾기
          </Link>
          <span className="truncate text-sm text-text-muted">{current.subjectName}</span>
          <span
            key={`${correctCount}-${solvedCount}`}
            className="inline-flex origin-center rounded-full border border-border bg-surface px-2.5 py-0.5 text-xs tabular-nums text-text-muted animate-score-pop"
          >
            {correctCount}/{solvedCount} 정답
          </span>
        </div>

        {/* 진행 바 */}
        <div className="mt-5">
          <div className="flex items-center justify-between text-[11px] text-text-muted">
            <span className="tabular-nums">
              <span className="font-semibold text-text">{currentNumber}</span>
              <span className="text-text-subtle"> / {sessionQuestions.length}</span>
            </span>
            <span className="tabular-nums">{Math.round(progressPct)}%</span>
          </div>
          <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-border">
            <div
              className="h-full rounded-full bg-primary transition-all duration-500 ease-out"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>

        {/* 문제 카드 */}
        <Card padding="lg" className="mt-5">
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-medium text-text-muted">{current.subjectName}</p>
            <div className="flex items-center gap-3">
              <BookmarkButton questionId={current.questionId} initialBookmarked={true} />
              <ReportQuestionButton questionId={current.questionId} />
              <QuestionTypeBadge type={current.questionType} />
            </div>
          </div>

          <div className="mt-5 text-base leading-relaxed">
            <QuestionContent content={current.body} />
          </div>

          {current.questionType === "MCQ" && (
            <ul className="mt-6 space-y-2">
              {current.options.map((optionText, idx) => {
                const num = idx + 1;
                const isSelected = selectedOption === num;
                const isCorrectOption = detail?.correctOption === num;

                let style =
                  "border-border hover:border-primary/40 hover:bg-primary/5 hover:-translate-y-[1px] hover:shadow-sm hover:scale-[1.01]";
                let motion = "";
                if (revealed) {
                  if (isCorrectOption) {
                    style = "border-success/60 bg-success/10 text-success";
                    motion = "animate-correct-reveal";
                  } else if (isSelected && !isCorrectOption) {
                    style = "border-danger/60 bg-danger/10 text-danger";
                    motion = "animate-shake-x";
                  } else {
                    style = "border-border opacity-50";
                  }
                } else if (isSelected) {
                  style = "border-primary/60 bg-primary/10 text-text ring-1 ring-primary/40";
                  motion = "animate-tap-bounce";
                }

                return (
                  <li key={num}>
                    <button
                      onClick={() => handleSelect(num)}
                      disabled={revealed}
                      className={`flex min-h-[52px] w-full items-start gap-3 rounded-lg border px-4 py-3 text-left text-base leading-relaxed transition-all duration-150 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 ${style} ${motion} disabled:cursor-default`}
                    >
                      <span
                        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                          isSelected && !revealed
                            ? "bg-primary/30 text-primary"
                            : revealed && isCorrectOption
                              ? "bg-success/30 text-success"
                              : revealed && isSelected
                                ? "bg-danger/30 text-danger"
                                : "border border-current text-current"
                        }`}
                      >
                        {num}
                      </span>
                      <span className="min-w-0 flex-1">
                        <QuestionContent content={optionText} className="mcq-option" />
                      </span>
                      {revealed && isCorrectOption && (
                        <span className="shrink-0 text-success">✓</span>
                      )}
                      {revealed && isSelected && !isCorrectOption && (
                        <span className="shrink-0 text-danger">✗</span>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {current.questionType === "SHORT_ANSWER" && (
            <div className="mt-6">
              <label htmlFor="bookmark-short-answer" className="mb-2 block text-xs text-text-muted">
                정답 입력
              </label>
              <input
                id="bookmark-short-answer"
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
                className="w-full rounded-lg border border-border bg-bg px-4 py-3 font-mono text-base text-text placeholder:text-text-subtle transition focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60 disabled:opacity-60"
                autoComplete="off"
                spellCheck={false}
              />
              <p className="mt-2 text-xs text-text-subtle">대소문자, 앞뒤 공백은 자동으로 무시됩니다.</p>
            </div>
          )}

          {current.questionType === "DESCRIPTIVE" && (
            <div className="mt-6">
              <label className="mb-2 block text-xs text-text-muted">서술형 답안</label>
              <textarea
                value={answerText}
                onChange={(e) => setAnswerText(e.target.value)}
                disabled={revealed}
                rows={6}
                placeholder="개념을 설명하는 답안을 작성하세요. 핵심 키워드를 포함할수록 점수가 올라갑니다."
                className="w-full resize-y rounded-lg border border-border bg-bg px-4 py-3 text-base leading-relaxed text-text placeholder:text-text-subtle transition focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60 disabled:opacity-60"
              />
              <p className="mt-2 text-xs text-text-subtle">
                {answerText.length}자 · 채점은 모범답안과 키워드 일치율로 이루어집니다.
              </p>
            </div>
          )}
        </Card>

        {!revealed && (
          <div className="mt-5 hidden justify-center lg:flex">
            <Button variant="primary" size="lg" onClick={handleSubmit} disabled={!hasAnswer()}>
              정답 제출
            </Button>
          </div>
        )}

        {revealed && detail && (
          <div className="mt-5 space-y-4">
            <div
              className={`flex flex-wrap items-center justify-between gap-3 rounded-lg border px-5 py-4 ${
                isCorrect
                  ? "border-success/40 bg-success/[0.08]"
                  : "border-danger/40 bg-danger/[0.08]"
              }`}
            >
              <div
                className={`flex items-center gap-2 text-sm font-semibold ${
                  isCorrect ? "text-success" : "text-danger"
                }`}
              >
                <span className="text-lg">{isCorrect ? "✓" : "✗"}</span>
                {isCorrect
                  ? "정답입니다!"
                  : detail.questionType === "MCQ"
                    ? `오답 — 정답은 ${detail.correctOption}번입니다.`
                    : `오답 — 모범답안: ${detail.answer ?? "(없음)"}`}
              </div>
              <div className="hidden items-center gap-2 lg:flex">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={goPrevious}
                  disabled={pastEntries.length === 0}
                  leftIcon={
                    <svg
                      className="h-3.5 w-3.5"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2.5}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M11 17l-5-5m0 0l5-5m-5 5h12"
                      />
                    </svg>
                  }
                >
                  이전
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleNext}
                  rightIcon={
                    <svg
                      className="h-3.5 w-3.5"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2.5}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M13 7l5 5m0 0l-5 5m5-5H6"
                      />
                    </svg>
                  }
                >
                  {queue.length === 0 ? "결과 보기" : "다음 문제"}
                </Button>
              </div>
            </div>

            {detail.questionType !== "MCQ" && (
              <Card padding="sm">
                <p className="text-xs font-medium text-text-muted">모범답안</p>
                <p className="mt-1 font-mono text-base text-text">{detail.answer ?? "-"}</p>
                {detail.keywords.length > 0 && (
                  <div className="mt-3">
                    <p className="text-xs font-medium text-text-muted">
                      {detail.questionType === "SHORT_ANSWER" ? "허용 표기" : "채점 키워드"}
                    </p>
                    <div className="mt-1 flex flex-wrap gap-1.5">
                      {detail.keywords.map((kw, i) => (
                        <span
                          key={i}
                          className="rounded bg-success/10 px-2 py-0.5 text-[11px] text-success"
                        >
                          {kw}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </Card>
            )}

            {detail.explanation && (
              <Card padding="sm">
                <p className="text-base font-medium text-primary">해설</p>
                <div className="mt-1 text-base leading-relaxed text-text-muted">
                  <QuestionContent content={detail.explanation} />
                </div>
              </Card>
            )}
          </div>
        )}
      </Container>

      {/* 모바일 하단 sticky 액션 바 */}
      <div
        className="fixed inset-x-0 z-40 border-t border-border bg-bg/95 px-4 py-3 backdrop-blur-md lg:hidden"
        style={{ bottom: "calc(3rem + env(safe-area-inset-bottom))" }}
      >
        {!revealed ? (
          <div className="grid grid-cols-[auto_1fr] gap-2">
            <Button
              variant="secondary"
              size="lg"
              onClick={goPrevious}
              disabled={pastEntries.length === 0}
              aria-label="이전 문제"
            >
              <svg
                className="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2.5}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M11 17l-5-5m0 0l5-5m-5 5h12"
                />
              </svg>
            </Button>
            <Button
              variant="primary"
              size="lg"
              className="w-full"
              onClick={handleSubmit}
              disabled={!hasAnswer()}
            >
              정답 제출
            </Button>
          </div>
        ) : detail ? (
          <div className="grid grid-cols-[auto_1fr] gap-2">
            <Button
              variant="secondary"
              size="lg"
              onClick={goPrevious}
              disabled={pastEntries.length === 0}
              aria-label="이전 문제"
            >
              <svg
                className="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2.5}
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M11 17l-5-5m0 0l5-5m-5 5h12"
                />
              </svg>
            </Button>
            <Button
              variant="primary"
              size="lg"
              className="w-full"
              onClick={handleNext}
              rightIcon={
                <svg
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2.5}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M13 7l5 5m0 0l-5 5m5-5H6"
                  />
                </svg>
              }
            >
              {queue.length === 0 ? "결과 보기" : "다음 문제"}
            </Button>
          </div>
        ) : null}
      </div>
    </section>
  );
}

// ── QuestionTypeBadge — SolveClient 와 동일 톤
function QuestionTypeBadge({ type }: { type: Question["questionType"] }) {
  if (type === "MCQ") {
    return (
      <Badge variant="soft" tone="neutral" size="xs">
        4지선다
      </Badge>
    );
  }
  if (type === "SHORT_ANSWER") {
    return (
      <Badge variant="soft" tone="success" size="xs">
        단답형
      </Badge>
    );
  }
  return (
    <Badge variant="soft" tone="info" size="xs">
      서술형
    </Badge>
  );
}

// ── 세션 종료 후 문제별 상세 보기 (SolveClient 와 동일 톤, body/options 분리형)
function SessionReviewList({ entries }: { entries: PastEntry[] }) {
  const [openIdx, setOpenIdx] = useState<number | null>(null);
  if (entries.length === 0) return null;
  return (
    <section className="mt-10">
      <h2 className="text-lg font-semibold tracking-tight">문제별 상세</h2>
      <p className="mt-1 text-xs text-text-muted">
        오답·정답 모두 다시 보고 해설을 확인하세요. 카드를 클릭하면 펼쳐집니다.
      </p>
      <div className="mt-4 space-y-2">
        {entries.map((e, i) => (
          <SessionReviewCard
            key={`${e.question.questionId}-${i}`}
            index={i}
            entry={e}
            open={openIdx === i}
            onToggle={() => setOpenIdx(openIdx === i ? null : i)}
          />
        ))}
      </div>
    </section>
  );
}

function SessionReviewCard({
  index,
  entry,
  open,
  onToggle,
}: {
  index: number;
  entry: PastEntry;
  open: boolean;
  onToggle: () => void;
}) {
  const preview = entry.question.body.replace(/\s+/g, " ").trim();
  const previewShort = preview.length > 40 ? preview.slice(0, 40) + "…" : preview;
  const graded = entry.detail !== null;
  const isCorrect = entry.detail
    ? isClientSideCorrectStatic(entry.detail, entry.selectedOption, entry.answerText)
    : false;

  return (
    <Card padding="none" className="overflow-hidden">
      <button
        onClick={onToggle}
        className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-surface-hover"
      >
        <span
          className={`inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
            !graded
              ? "bg-surface-hover text-text-subtle"
              : isCorrect
                ? "bg-success/15 text-success"
                : "bg-danger/15 text-danger"
          }`}
        >
          {!graded ? "–" : isCorrect ? "✓" : "✗"}
        </span>
        <span className="text-xs font-semibold tabular-nums text-text-muted">문제 {index + 1}</span>
        <span className="flex-1 truncate text-sm text-text">{previewShort}</span>
        <span
          className={`text-xs text-text-subtle transition-transform ${open ? "rotate-180" : ""}`}
        >
          ▾
        </span>
      </button>

      {open && (
        <div className="border-t border-border bg-surface-subtle/50 px-4 py-4">
          <QuestionContent content={entry.question.body} />

          {entry.detail?.questionType === "MCQ" && entry.question.options.length > 0 && (
            <ul className="mt-3 space-y-1.5">
              {entry.question.options.map((opt, i) => {
                const num = i + 1;
                const isAnswer = num === entry.detail!.correctOption;
                const isMine = num === entry.selectedOption;
                return (
                  <li
                    key={num}
                    className={`flex items-start gap-2 rounded-md border px-3 py-2 text-sm ${
                      isAnswer
                        ? "border-success/40 bg-success/10"
                        : isMine
                          ? "border-danger/40 bg-danger/10"
                          : "border-border bg-bg"
                    }`}
                  >
                    <span className="font-semibold tabular-nums">{num}.</span>
                    <span className="min-w-0 flex-1">
                      <QuestionContent content={opt} className="mcq-option" />
                    </span>
                    {isAnswer && (
                      <span className="ml-1 shrink-0 text-[11px] font-bold text-success">정답</span>
                    )}
                    {isMine && !isAnswer && (
                      <span className="ml-1 shrink-0 text-[11px] font-bold text-danger">내 답</span>
                    )}
                  </li>
                );
              })}
            </ul>
          )}

          {entry.detail && entry.detail.questionType !== "MCQ" && (
            <div className="mt-3 rounded-md border border-border bg-bg p-3 text-sm">
              <p className="text-xs font-semibold text-text-muted">내 답</p>
              <p className="mt-1">
                {entry.answerText || <span className="text-text-subtle">(미응답)</span>}
              </p>
              {entry.detail.answer && (
                <>
                  <p className="mt-3 text-xs font-semibold text-text-muted">모범답안</p>
                  <p className="mt-1">{entry.detail.answer}</p>
                </>
              )}
              {entry.detail.keywords.length > 0 && (
                <p className="mt-2 text-xs text-text-muted">
                  키워드: {entry.detail.keywords.join(", ")}
                </p>
              )}
            </div>
          )}

          {entry.detail?.explanation && (
            <div className="mt-3 rounded-md border border-primary/20 bg-primary/[0.04] p-3">
              <p className="text-[11px] font-bold uppercase tracking-wider text-primary">해설</p>
              <div className="mt-2 text-sm leading-relaxed">
                <QuestionContent content={entry.detail.explanation} />
              </div>
            </div>
          )}

          {!entry.detail && (
            <p className="mt-3 text-xs text-text-subtle">채점 전 문제입니다.</p>
          )}
        </div>
      )}
    </Card>
  );
}
