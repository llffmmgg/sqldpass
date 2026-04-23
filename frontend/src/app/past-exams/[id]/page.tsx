"use client";

import Link from "next/link";
import dynamic from "next/dynamic";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import Spinner from "@/components/Spinner";
import QuestionContent from "@/components/QuestionContent";
import { GradingDisclaimerModal } from "@/components/GradingDisclaimerModal";
import { Badge, Button, Card, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromExamType,
  type CertKey,
} from "@/lib/cert-tokens";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  getPastExam,
  gradePastExam,
  type PastExamAnswerPayload,
  type PastExamDetail,
  type PastExamGradeResponse,
  type PastExamQuestion,
} from "@/lib/pastExamApi";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";

const CountdownCircleTimer = dynamic(
  () => import("react-countdown-circle-timer").then((m) => m.CountdownCircleTimer),
  { ssr: false },
);

/** 자격증별 실제 시험 시간 (분) */
const EXAM_TIME_MINUTES: Record<string, number> = {
  SQLD: 90,
  ENGINEER_PRACTICAL: 150,
  COMPUTER_LITERACY_1: 60,
  COMPUTER_LITERACY_2: 40,
  ENGINEER_WRITTEN: 150,
  ADSP: 90,
};

interface AnswerState {
  option?: number;
  text?: string;
}

function hasAnswer(a?: AnswerState): boolean {
  if (!a) return false;
  if (a.option != null) return true;
  if (a.text != null && a.text.trim() !== "") return true;
  return false;
}

export default function PastExamDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id);
  if (!Number.isFinite(id)) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <p className="text-sm text-text-muted">잘못된 회차 ID 입니다.</p>
      </main>
    );
  }
  return <PastExamRunner id={id} />;
}

function PastExamRunner({ id }: { id: number }) {
  const router = useRouter();

  const [exam, setExam] = useState<PastExamDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [answers, setAnswers] = useState<Map<number, AnswerState>>(new Map());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<PastExamGradeResponse | null>(null);
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
  }, []);

  // 타이머
  const [timerRunning, setTimerRunning] = useState(false);
  const [timerSeconds, setTimerSeconds] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const timerLimit = useMemo(
    () => (exam ? (EXAM_TIME_MINUTES[exam.examType] ?? 90) * 60 : 0),
    [exam],
  );

  const startTimer = useCallback(() => {
    if (timerRef.current) return;
    setTimerRunning(true);
    timerRef.current = setInterval(() => {
      setTimerSeconds((prev) => prev + 1);
    }, 1000);
  }, []);

  const pauseTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setTimerRunning(false);
  }, []);

  const resetTimer = useCallback(() => {
    pauseTimer();
    setTimerSeconds(0);
  }, [pauseTimer]);

  useEffect(() => {
    if (result) pauseTimer();
  }, [result, pauseTimer]);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    getPastExam(id)
      .then((data) => {
        if (!cancelled) setExam(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "불러오기 실패");
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const answeredCount = useMemo(
    () => Array.from(answers.values()).filter(hasAnswer).length,
    [answers],
  );

  // 탭 닫기/새로고침 방지 (응시 중이고 답안 있으면)
  useEffect(() => {
    if (result) return;
    if (answeredCount === 0) return;
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [answeredCount, result]);

  if (error) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg px-4 text-text">
        <div className="max-w-md text-center">
          <p className="text-danger">{error}</p>
          <button
            onClick={() => router.push("/past-exams")}
            className="mt-6 rounded-lg border border-border px-4 py-2 text-sm text-text-muted hover:text-text"
          >
            ← 기출 복원 목록으로
          </button>
        </div>
      </main>
    );
  }

  if (!exam) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <Spinner message="기출 회차 불러오는 중..." />
      </main>
    );
  }

  const cert: CertKey = certFromExamType(exam.examType) ?? "SQLD";
  const token = CERT_TOKENS[cert];

  // 결과 화면
  if (result) {
    return (
      <PastExamResultView
        exam={exam}
        result={result}
        answers={answers}
        loggedIn={loggedIn}
        onRestart={() => {
          setResult(null);
          setAnswers(new Map());
          setCurrentIdx(0);
          resetTimer();
        }}
      />
    );
  }

  const total = exam.questions.length;
  const current = exam.questions[currentIdx];
  const parsed = parseQuestion(current.content);
  const currentAnswer = answers.get(current.id);

  function updateAnswer(questionId: number, updater: (prev: AnswerState) => AnswerState) {
    setAnswers((prev) => {
      const next = new Map(prev);
      next.set(questionId, updater(prev.get(questionId) ?? {}));
      return next;
    });
  }

  function selectOption(opt: number) {
    updateAnswer(current.id, (prev) => ({ ...prev, option: opt }));
  }

  function setAnswerText(text: string) {
    updateAnswer(current.id, (prev) => ({ ...prev, text }));
  }

  function goPrev() {
    if (currentIdx > 0) setCurrentIdx(currentIdx - 1);
  }

  function goNext() {
    if (currentIdx < total - 1) setCurrentIdx(currentIdx + 1);
  }

  async function handleSubmit() {
    if (!exam) return;
    if (answeredCount === 0) return;
    if (answeredCount < total) {
      const ok = confirm(
        `미답 ${total - answeredCount}문항은 오답으로 처리됩니다. 제출하시겠습니까?`,
      );
      if (!ok) return;
    }

    setSubmitting(true);
    try {
      const payload: PastExamAnswerPayload[] = exam.questions.map((q) => {
        const a = answers.get(q.id);
        if (q.questionType === "MCQ") {
          return a?.option != null
            ? { questionId: q.id, selectedOption: a.option }
            : { questionId: q.id };
        }
        return { questionId: q.id, answerText: a?.text ?? "" };
      });
      const graded = await gradePastExam(exam.id, payload);
      setResult(graded);
    } catch (e) {
      alert(e instanceof Error ? e.message : "채점에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  const jumpGridCols = total <= 20 ? "grid-cols-5 sm:grid-cols-10" : "grid-cols-10";
  const roundLabel = buildRoundLabel(exam);

  return (
    <main className="min-h-screen bg-bg text-text">
      <GradingDisclaimerModal />
      <Container size="default" className="py-10">
        <div className="flex items-stretch gap-4">
          {/* 타이머 — 데스크톱 좌측 sticky */}
          <div className="hidden lg:block">
            <div className="sticky top-20 w-32">
              <div className="flex flex-col items-center gap-3 rounded-xl border border-border bg-surface/80 px-3 py-4 shadow-lg backdrop-blur">
                <ExamTimer
                  seconds={timerSeconds}
                  limit={timerLimit}
                  running={timerRunning}
                  cert={cert}
                  onStart={startTimer}
                  onPause={pauseTimer}
                  onReset={resetTimer}
                />
              </div>
            </div>
          </div>

          <div className="mx-auto min-w-0 max-w-3xl flex-1">
            {/* 상단 헤더 */}
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <Badge cert={cert} variant="soft" size="xs" dot>
                    {token.label}
                  </Badge>
                  <p className="truncate text-xs text-text-muted">{roundLabel}</p>
                </div>
                <p className="mt-1 text-lg font-bold tabular-nums">
                  {currentIdx + 1} <span className="text-text-subtle">/</span> {total}
                  <span className="ml-2 text-xs font-medium text-text-muted">
                    답안 {answeredCount}/{total}
                  </span>
                </p>
              </div>
              <Button
                variant="primary"
                size="md"
                onClick={handleSubmit}
                disabled={submitting || answeredCount === 0}
              >
                {submitting ? "제출 중..." : `제출하기 (${answeredCount}/${total})`}
              </Button>
            </div>
            <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-border">
              <div
                className={`h-full transition-all ${
                  answeredCount === total ? token.tailwind.bg : `${token.tailwind.bg} opacity-60`
                }`}
                style={{ width: `${(answeredCount / total) * 100}%` }}
              />
            </div>

            {/* 문제 */}
            <Card padding="lg" className="mt-6">
              <div className="flex items-center justify-between gap-2">
                <p className="text-xs font-medium text-text-muted">{current.subjectName}</p>
                <QuestionTypeBadge type={current.questionType} />
              </div>
              <h2 className="mt-2 text-lg font-semibold">문항 {currentIdx + 1}</h2>
              <div className="mt-4">
                <QuestionContent content={parsed.body} />
              </div>

              <div className="mt-6">
                {current.questionType === "MCQ" && parsed.options.length > 0 && (
                  <MCQOptions
                    options={parsed.options}
                    selected={currentAnswer?.option ?? null}
                    onSelect={selectOption}
                    cert={cert}
                  />
                )}
                {current.questionType === "SHORT_ANSWER" && (
                  <ShortAnswerInput
                    value={currentAnswer?.text ?? ""}
                    onChange={setAnswerText}
                    onEnter={goNext}
                  />
                )}
                {current.questionType === "DESCRIPTIVE" && (
                  <DescriptiveInput
                    value={currentAnswer?.text ?? ""}
                    onChange={setAnswerText}
                  />
                )}
              </div>
            </Card>

            {/* 이전/다음 */}
            <div className="mt-6 flex gap-3">
              <button
                onClick={goPrev}
                disabled={currentIdx === 0}
                className="flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-text-muted transition hover:text-text disabled:opacity-30"
              >
                ← 이전
              </button>
              <button
                onClick={goNext}
                disabled={currentIdx >= total - 1}
                className={`flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-text transition disabled:opacity-30 ${token.tailwind.borderHover}`}
              >
                다음 →
              </button>
            </div>

            {/* 빠른 이동 */}
            <div className="mt-6">
              <p className="mb-2 text-xs text-text-muted">빠른 이동</p>
              <div className={`grid gap-1.5 ${jumpGridCols}`}>
                {exam.questions.map((q, i) => {
                  const answered = hasAnswer(answers.get(q.id));
                  const active = i === currentIdx;
                  return (
                    <button
                      key={q.id}
                      onClick={() => setCurrentIdx(i)}
                      className={`h-9 rounded text-xs font-medium transition ${
                        active
                          ? `${token.tailwind.bg} text-white`
                          : answered
                            ? `${token.tailwind.bgSoft} text-text`
                            : "bg-surface text-text-muted hover:bg-border"
                      }`}
                    >
                      {i + 1}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 하단 제출 */}
            <div className="mt-8">
              <Button
                variant="primary"
                size="lg"
                onClick={handleSubmit}
                disabled={submitting || answeredCount === 0}
                className="w-full"
              >
                {submitting ? "제출 중..." : "제출하기"}
              </Button>
              {answeredCount < total && answeredCount > 0 && (
                <p className="mt-2 text-center text-xs text-text-muted">
                  미답 {total - answeredCount}문항은 오답으로 처리됩니다
                </p>
              )}
              {answeredCount === 0 && (
                <p className="mt-2 text-center text-xs text-text-muted">
                  최소 1문항 이상 답안을 작성해야 제출할 수 있습니다
                </p>
              )}
            </div>
          </div>

          {/* 모바일 타이머 */}
          <div className="fixed bottom-4 right-4 z-40 lg:hidden">
            <div className="flex flex-col items-center gap-2 rounded-xl border border-border bg-surface/95 px-3 py-3 shadow-xl backdrop-blur">
              <ExamTimer
                seconds={timerSeconds}
                limit={timerLimit}
                running={timerRunning}
                cert={cert}
                onStart={startTimer}
                onPause={pauseTimer}
                onReset={resetTimer}
              />
            </div>
          </div>
        </div>
      </Container>
    </main>
  );
}

function buildRoundLabel(exam: PastExamDetail): string {
  const parts: string[] = [];
  if (exam.examYear) parts.push(`${exam.examYear}년`);
  if (exam.examRound) parts.push(`제${exam.examRound}회`);
  if (parts.length === 0) parts.push(exam.name);
  return parts.join(" ");
}

// ===========================================================
// 결과 화면 — 문제별 정답/해설 공개 + 로그인 CTA
// ===========================================================

function PastExamResultView({
  exam,
  result,
  answers,
  loggedIn,
  onRestart,
}: {
  exam: PastExamDetail;
  result: PastExamGradeResponse;
  answers: Map<number, AnswerState>;
  loggedIn: boolean;
  onRestart: () => void;
}) {
  const cert: CertKey = certFromExamType(exam.examType) ?? "SQLD";
  const token = CERT_TOKENS[cert];
  const roundLabel = buildRoundLabel(exam);

  const correctRate = result.totalCount > 0
    ? Math.round((result.correctCount / result.totalCount) * 100)
    : 0;
  const rateTone =
    correctRate >= 80 ? "text-success" : correctRate >= 60 ? "text-warning" : "text-danger";

  const itemsById = useMemo(() => {
    const map = new Map<number, PastExamGradeResponse["items"][number]>();
    for (const it of result.items) map.set(it.questionId, it);
    return map;
  }, [result]);

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-14">
        <Badge cert={cert} variant="soft" size="sm" dot>
          기출 복원 결과
        </Badge>
        <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
          {roundLabel} <span className="text-text-subtle">· 채점 완료</span>
        </h1>

        <div className="mt-6 rounded-2xl border border-border bg-gradient-to-br from-surface via-surface to-transparent p-8 text-center">
          <p className="text-xs font-semibold uppercase tracking-wider text-text-muted">점수</p>
          <p className={`mt-2 text-6xl font-bold tabular-nums ${rateTone}`}>
            {result.score}
            <span className="text-3xl text-text-subtle">점</span>
          </p>
          <p className="mt-3 text-sm text-text-muted">
            {result.correctCount} / {result.totalCount} 정답 ·{" "}
            <span className={rateTone}>{correctRate}%</span>
          </p>
        </div>

        {!loggedIn && (
          <Card padding="lg" className="mt-6 border-primary/30 bg-primary/[0.05]">
            <p className="text-xs font-semibold uppercase tracking-wider text-primary">
              로그인하면 이 기능이 더 쓸만해져요
            </p>
            <ul className="mt-3 space-y-1.5 text-sm text-text">
              <li>· 틀린 문제만 모은 오답 노트 자동 저장</li>
              <li>· 회차별 점수 기록이 대시보드에 누적</li>
              <li>· 연속 학습 스트릭 적립</li>
            </ul>
            <a
              href={getGoogleLoginUrl()}
              className="mt-4 inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-text transition-colors hover:border-primary/40"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
              </svg>
              Google로 가입하고 오답노트 저장
            </a>
          </Card>
        )}

        <div className="mt-10">
          <h2 className="text-base font-bold">문제별 정답 확인</h2>
          <div className="mt-4 space-y-4">
            {exam.questions.map((q, idx) => {
              const graded = itemsById.get(q.id);
              if (!graded) return null;
              return (
                <PastExamReviewCard
                  key={q.id}
                  order={idx + 1}
                  question={q}
                  graded={graded}
                  userAnswer={answers.get(q.id)}
                />
              );
            })}
          </div>
        </div>

        <div className="mt-10 flex flex-wrap gap-3">
          <button
            onClick={onRestart}
            className="flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-text-muted transition hover:text-text"
          >
            다시 응시
          </button>
          <Link
            href={`/past-exams?cert=${cert}`}
            className={`flex-1 rounded-lg py-3 text-center text-sm font-semibold transition ${token.tailwind.bg} text-white hover:opacity-90`}
          >
            다른 회차 보기
          </Link>
        </div>
      </Container>
    </main>
  );
}

function PastExamReviewCard({
  order,
  question,
  graded,
  userAnswer,
}: {
  order: number;
  question: PastExamQuestion;
  graded: PastExamGradeResponse["items"][number];
  userAnswer?: AnswerState;
}) {
  const parsed = parseQuestion(question.content);
  const isCorrect = graded.correct;
  const borderTone = isCorrect
    ? "border-success/40 bg-success/[0.04]"
    : graded.partialScore > 0
      ? "border-warning/40 bg-warning/[0.04]"
      : "border-danger/40 bg-danger/[0.04]";

  return (
    <div className={`rounded-xl border p-5 ${borderTone}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-semibold text-text-muted">
          문항 {order} · {question.subjectName}
        </span>
        <span
          className={`rounded-full border px-2 py-0.5 text-[10px] font-bold ${
            isCorrect
              ? "border-success/50 bg-success/10 text-success"
              : graded.partialScore > 0
                ? "border-warning/50 bg-warning/10 text-warning"
                : "border-danger/50 bg-danger/10 text-danger"
          }`}
        >
          {isCorrect ? "정답" : graded.partialScore > 0 ? "부분점수" : "오답"}
        </span>
      </div>
      <div className="mt-3 text-sm leading-relaxed">
        <QuestionContent content={parsed.body} />
      </div>

      {question.questionType === "MCQ" && (
        <ul className="mt-4 space-y-1.5 text-sm">
          {parsed.options.map((opt, i) => {
            const n = i + 1;
            const isCorrectOpt = graded.correctOption === n;
            const isUserPick = userAnswer?.option === n;
            let tone = "border-border text-text-muted";
            if (isCorrectOpt) {
              tone = "border-success/50 bg-success/10 text-success font-medium";
            } else if (isUserPick) {
              tone = "border-danger/50 bg-danger/10 text-danger";
            }
            return (
              <li key={n}>
                <div className={`rounded-md border px-3 py-2 text-sm ${tone}`}>
                  <span className="mr-2 font-bold">{n}.</span>
                  {opt}
                  {isCorrectOpt && <span className="ml-2 text-[11px]">✓ 정답</span>}
                  {!isCorrectOpt && isUserPick && (
                    <span className="ml-2 text-[11px]">내 선택</span>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {question.questionType !== "MCQ" && (
        <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <div className="rounded-md border border-border bg-surface px-3 py-2 text-sm">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
              제출 답안
            </p>
            <p className="mt-1 whitespace-pre-wrap text-text">
              {graded.submittedAnswerText || "-"}
            </p>
          </div>
          <div className="rounded-md border border-success/40 bg-success/[0.04] px-3 py-2 text-sm">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-success">
              정답
            </p>
            <p className="mt-1 whitespace-pre-wrap font-mono text-text">{graded.answer ?? "-"}</p>
            {graded.keywords.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1.5">
                {graded.keywords.map((kw, i) => (
                  <span
                    key={i}
                    className="rounded bg-success/10 px-2 py-0.5 text-[11px] text-success"
                  >
                    {kw}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {graded.explanation && (
        <div className="mt-3 rounded-md border border-border bg-surface px-3 py-2">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-primary">해설</p>
          <div className="mt-1 text-sm leading-relaxed text-text-muted">
            <QuestionContent content={graded.explanation} />
          </div>
        </div>
      )}
    </div>
  );
}

// ===========================================================
// 하위 컴포넌트 — MCQ/단답/서술/타이머
// ===========================================================

function QuestionTypeBadge({ type }: { type: PastExamQuestion["questionType"] }) {
  if (type === "MCQ") {
    return <Badge variant="soft" tone="neutral" size="xs">4지선다</Badge>;
  }
  if (type === "SHORT_ANSWER") {
    return <Badge variant="soft" tone="success" size="xs">단답형</Badge>;
  }
  return <Badge variant="soft" tone="info" size="xs">서술형</Badge>;
}

function MCQOptions({
  options,
  selected,
  onSelect,
  cert,
}: {
  options: string[];
  selected: number | null;
  onSelect: (n: number) => void;
  cert: CertKey;
}) {
  const token = CERT_TOKENS[cert];
  return (
    <ul className="space-y-2">
      {options.map((optionText, idx) => {
        const num = idx + 1;
        const isSelected = selected === num;
        return (
          <li key={num}>
            <button
              onClick={() => onSelect(num)}
              className={`w-full rounded-lg border px-4 py-3 text-left text-base transition ${
                isSelected
                  ? `${token.tailwind.border} ${token.tailwind.bgSoft} text-text`
                  : `border-border text-text ${token.tailwind.borderHover}`
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
  );
}

function ShortAnswerInput({
  value,
  onChange,
  onEnter,
}: {
  value: string;
  onChange: (v: string) => void;
  onEnter: () => void;
}) {
  return (
    <div>
      <label className="mb-2 block text-xs text-text-muted">정답 입력</label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            onEnter();
          }
        }}
        placeholder="정답을 입력하세요 (엔터: 다음 문제)"
        className="w-full rounded-lg border border-border bg-bg px-4 py-3 font-mono text-base text-text placeholder:text-text-subtle transition focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60"
        autoComplete="off"
        spellCheck={false}
      />
      <p className="mt-2 text-xs text-text-subtle">
        대소문자·공백은 자동으로 무시되며, 동의어 표기도 정답으로 인정됩니다.
      </p>
    </div>
  );
}

function DescriptiveInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div>
      <label className="mb-2 block text-xs text-text-muted">서술형 답안</label>
      <div className="relative">
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          rows={8}
          placeholder="개념을 설명하는 답안을 작성하세요. 핵심 키워드를 포함할수록 점수가 올라갑니다."
          className="w-full resize-y rounded-lg border border-border bg-bg px-4 py-3 text-base leading-relaxed text-text placeholder:text-text-subtle transition focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60"
        />
        <span className="pointer-events-none absolute bottom-2 right-3 text-xs tabular-nums text-text-subtle">
          {value.length} 자
        </span>
      </div>
      <p className="mt-2 text-xs text-text-subtle">
        채점 방식: 핵심 키워드 포함률 70% 이상 → 정답, 40% 이상 → 부분점수.
      </p>
    </div>
  );
}

function formatTime(totalSeconds: number): string {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  if (h > 0) return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

function certToHex(cert: CertKey): string {
  return (
    {
      SQLD: "#f59e0b",
      ENGINEER_PRACTICAL: "#10b981",
      ENGINEER_WRITTEN: "#f43f5e",
      COMPUTER_LITERACY_1: "#0ea5e9",
      COMPUTER_LITERACY_2: "#6366f1",
      ADSP: "#14b8a6",
    } satisfies Record<CertKey, string>
  )[cert];
}

function ExamTimer({
  seconds,
  limit,
  running,
  cert,
  onStart,
  onPause,
  onReset,
}: {
  seconds: number;
  limit: number;
  running: boolean;
  cert: CertKey;
  onStart: () => void;
  onPause: () => void;
  onReset: () => void;
}) {
  const remaining = Math.max(limit - seconds, 0);
  const isOvertime = seconds > limit;
  const isUrgent = remaining <= 300 && remaining > 0;
  const size = 110;
  const accentHex = certToHex(cert);
  const trailColor = "rgba(128,128,128,0.15)";

  if (seconds === 0 && !running) {
    return (
      <button
        onClick={onStart}
        className="flex shrink-0 flex-col items-center gap-1"
        title={`제한시간 ${Math.floor(limit / 60)}분`}
      >
        <CountdownCircleTimer
          isPlaying={false}
          duration={limit}
          initialRemainingTime={limit}
          colors={accentHex as `#${string}`}
          trailColor={trailColor}
          size={size}
          strokeWidth={7}
          strokeLinecap="round"
        >
          {() => (
            <svg className="h-7 w-7 text-text-muted" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}
        </CountdownCircleTimer>
        <span className="text-xs font-medium text-text-muted">{Math.floor(limit / 60)}분</span>
      </button>
    );
  }

  return (
    <div className="flex shrink-0 flex-col items-center gap-1">
      <CountdownCircleTimer
        key={`timer-${limit}`}
        isPlaying={running}
        duration={limit}
        initialRemainingTime={remaining}
        colors={[accentHex as `#${string}`, "#fbbf24", "#f87171", "#ef4444"]}
        colorsTime={[limit, Math.floor(limit * 0.3), 300, 0]}
        trailColor={trailColor}
        size={size}
        strokeWidth={7}
        strokeLinecap="round"
        onComplete={() => undefined}
      >
        {() => (
          <span
            className={`font-mono text-sm font-bold tabular-nums leading-none ${
              isOvertime
                ? "text-danger"
                : isUrgent
                  ? "text-warning animate-pulse"
                  : "text-text"
            }`}
          >
            {isOvertime ? `+${formatTime(seconds - limit)}` : formatTime(remaining)}
          </span>
        )}
      </CountdownCircleTimer>
      <div className="flex items-center gap-2">
        <button
          onClick={running ? onPause : onStart}
          className="rounded-md p-1.5 text-text-muted transition-colors hover:bg-border/50 hover:text-text"
          title={running ? "일시정지" : "재개"}
        >
          {running ? (
            <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z" />
            </svg>
          ) : (
            <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}
        </button>
        <button
          onClick={onReset}
          className="rounded-md p-1.5 text-text-muted transition-colors hover:bg-border/50 hover:text-text"
          title="리셋"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182"
            />
          </svg>
        </button>
      </div>
    </div>
  );
}
