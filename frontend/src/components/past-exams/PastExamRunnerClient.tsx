"use client";

import Link from "next/link";
import dynamic from "next/dynamic";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import Spinner from "@/components/Spinner";
import QuestionContent from "@/components/QuestionContent";
import { GradingDisclaimerModal } from "@/components/GradingDisclaimerModal";
import { Badge, Button, Card, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromExamType,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  gradePastExam,
  type PastExamAnswerPayload,
  type PastExamDetail,
  type PastExamGradedItem,
  type PastExamGradeResponse,
  type PastExamQuestion,
} from "@/lib/pastExamApi";
import { getSolves, type SolveSummaryResponse } from "@/lib/api";
import { getToken } from "@/lib/auth";
import MockExamAttemptsView from "@/components/MockExamAttemptsView";
import { hapticError, hapticLight, hapticSuccess } from "@/lib/haptic";
import QuestionJumpPanel, {
  type QuestionJumpGroup,
} from "@/components/exam/QuestionJumpPanel";

const CountdownCircleTimer = dynamic(
  () => import("react-countdown-circle-timer").then((m) => m.CountdownCircleTimer),
  { ssr: false },
);

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

function hasAnswer(answer?: AnswerState): boolean {
  if (!answer) return false;
  if (answer.option != null) return true;
  if (answer.text != null && answer.text.trim() !== "") return true;
  return false;
}

export default function PastExamRunnerClient({
  initialExam,
}: {
  initialExam: PastExamDetail;
}) {
  const exam = initialExam;

  const [currentIdx, setCurrentIdx] = useState(0);
  const [answers, setAnswers] = useState<Map<number, AnswerState>>(new Map());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<PastExamGradeResponse | null>(null);
  const [timerRunning, setTimerRunning] = useState(false);
  const [timerSeconds, setTimerSeconds] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const [attempts, setAttempts] = useState<SolveSummaryResponse[]>([]);
  const [attemptsLoaded, setAttemptsLoaded] = useState(false);
  const [started, setStarted] = useState(false);

  const timerLimit = useMemo(
    () => (EXAM_TIME_MINUTES[exam.examType] ?? 90) * 60,
    [exam.examType],
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
  }, [pauseTimer, result]);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  // 비로그인은 호출 자체를 스킵. fetchApi 가 401 만나면 "/" 로 강제 이동시키므로
  // 비로그인 풀이 허용 정책과 충돌함.
  useEffect(() => {
    if (!getToken()) {
      setAttempts([]);
      setAttemptsLoaded(true);
      return;
    }
    let cancelled = false;
    getSolves({ mockExamId: exam.id })
      .then((rows) => {
        if (!cancelled) setAttempts(rows);
      })
      .catch(() => {
        if (!cancelled) setAttempts([]);
      })
      .finally(() => {
        if (!cancelled) setAttemptsLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [exam.id]);

  const answeredCount = useMemo(
    () => Array.from(answers.values()).filter(hasAnswer).length,
    [answers],
  );

  // QuestionJumpPanel — 응답 완료 인덱스 (0-based). exam 로드 전엔 빈 Set.
  // hooks 호출 위치는 무조건 early return 이전이어야 함 (React error #310 회피).
  const answeredIndices = useMemo(() => {
    const s = new Set<number>();
    if (!exam) return s;
    exam.questions.forEach((q, i) => {
      if (hasAnswer(answers.get(q.id))) s.add(i);
    });
    return s;
  }, [exam, answers]);

  // QuestionJumpPanel — 인접한 같은 subjectName 묶음을 그룹으로
  const jumpGroups = useMemo<QuestionJumpGroup[]>(() => {
    if (!exam) return [];
    const qs = exam.questions;
    if (qs.length === 0) return [];
    const groups: QuestionJumpGroup[] = [];
    let from = 0;
    let label = qs[0].subjectName ?? "";
    for (let i = 1; i < qs.length; i++) {
      const name = qs[i].subjectName ?? "";
      if (name !== label) {
        groups.push({ label, from, to: i - 1 });
        from = i;
        label = name;
      }
    }
    groups.push({ label, from, to: qs.length - 1 });
    return groups.length <= 1 ? [] : groups;
  }, [exam]);

  useEffect(() => {
    if (result) return;
    if (answeredCount === 0) return;

    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [answeredCount, result]);

  const cert: CertKey = certFromExamType(exam.examType) ?? "SQLD";
  const token = CERT_TOKENS[cert];

  if (!exam) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <Spinner message="기출 회차를 불러오는 중..." />
      </main>
    );
  }

  // 시도 1개 이상 + 아직 새로 풀기 안 시작 + 결과 화면도 아닐 때 → 인터스티셜
  if (attemptsLoaded && attempts.length > 0 && !started && !result) {
    const roundLabel = buildRoundLabel(exam);
    const examTitle = roundLabel === exam.name
      ? `[${token.label}] ${exam.name} 기출 복원`
      : `[${token.label}] ${roundLabel} 기출 복원`;
    return (
      <MockExamAttemptsView
        attempts={attempts}
        examTitle={examTitle}
        examType={exam.examType}
        meta={
          <span className="text-text-muted tabular-nums">
            {exam.questions.length}문항
          </span>
        }
        onStartNew={() => setStarted(true)}
      />
    );
  }

  if (result) {
    return (
      <PastExamResultView
        exam={exam}
        result={result}
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
  const roundLabel = buildRoundLabel(exam);

  function updateAnswer(questionId: number, updater: (prev: AnswerState) => AnswerState) {
    setAnswers((prev) => {
      const next = new Map(prev);
      next.set(questionId, updater(prev.get(questionId) ?? {}));
      return next;
    });
  }

  function selectOption(option: number) {
    updateAnswer(current.id, (prev) => ({ ...prev, option }));
    hapticLight();
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
    if (answeredCount === 0) return;

    if (answeredCount < total) {
      const confirmed = confirm(
        `미답 ${total - answeredCount}문항은 오답으로 처리됩니다. 제출하시겠습니까?`,
      );
      if (!confirmed) return;
    }

    setSubmitting(true);
    try {
      const payload: PastExamAnswerPayload[] = exam.questions.map((question) => {
        const answer = answers.get(question.id);
        if (question.questionType === "MCQ") {
          return answer?.option != null
            ? { questionId: question.id, selectedOption: answer.option }
            : { questionId: question.id };
        }

        return { questionId: question.id, answerText: answer?.text ?? "" };
      });

      const graded = await gradePastExam(exam.id, payload);
      setResult(graded);

      if (graded.totalCount > 0 && graded.correctCount * 2 >= graded.totalCount) {
        hapticSuccess();
      } else {
        hapticError();
      }
    } catch (error) {
      alert(error instanceof Error ? error.message : "채점에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <GradingDisclaimerModal />
      <Container size="default" className="py-10">
        <div className="mb-6 rounded-2xl border border-border bg-surface/70 p-5">
          <div className="flex flex-wrap items-center gap-2">
            <Badge cert={cert} variant="soft" size="xs" dot>
              {token.label}
            </Badge>
            <span className="text-xs text-text-muted">{roundLabel}</span>
          </div>
          <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
            {exam.name}
          </h1>
          <p className="mt-2 text-sm text-text-muted">
            로그인 없이 풀이·채점이 가능합니다. 로그인하면 회차별 최고 점수와 풀이 기록이 자동 저장돼요.
          </p>
        </div>

        <div className="flex items-stretch gap-4">
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
                className={cnProgress(token.tailwind.bg, answeredCount === total)}
                style={{ width: `${(answeredCount / total) * 100}%` }}
              />
            </div>

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
                  미답 {total - answeredCount}문항은 오답으로 처리됩니다.
                </p>
              )}
              {answeredCount === 0 && (
                <p className="mt-2 text-center text-xs text-text-muted">
                  최소 1문항 이상 답안을 작성해야 제출할 수 있습니다.
                </p>
              )}
            </div>
          </div>

          {/* 문제 번호 빠른 이동 — 데스크탑 우측 sticky / 모바일 floating + drawer */}
          <QuestionJumpPanel
            total={total}
            currentIdx={currentIdx}
            answered={answeredIndices}
            onJump={(i) => setCurrentIdx(i)}
            groups={jumpGroups}
            accent={{
              bg: token.tailwind.bg,
              text: "text-white",
              border: token.tailwind.border,
            }}
          />

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
  if (exam.examRound) parts.push(`${exam.examRound}회`);
  if (parts.length === 0) parts.push(exam.name);
  return parts.join(" ");
}

function PastExamResultView({
  exam,
  result,
  onRestart,
}: {
  exam: PastExamDetail;
  result: PastExamGradeResponse;
  onRestart: () => void;
}) {
  const router = useRouter();
  const cert: CertKey = certFromExamType(exam.examType) ?? "SQLD";
  const token = CERT_TOKENS[cert];
  const roundLabel = buildRoundLabel(exam);
  const backHref = `/past-exams/${slugFromCert(cert)}`;

  const correctRate =
    result.totalCount > 0
      ? Math.round((result.correctCount / result.totalCount) * 100)
      : 0;
  const rateTone =
    correctRate >= 80
      ? "text-success"
      : correctRate >= 60
        ? "text-warning"
        : "text-danger";

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-14">
        <Badge cert={cert} variant="soft" size="sm" dot>
          기출 복원 결과
        </Badge>
        <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
          {roundLabel} <span className="text-text-subtle">· 채점 완료</span>
        </h1>

        {/* 합격/불합격 배너 — 자격증별 공식 기준 (백엔드 PassFailCriteria 적용) */}
        <div
          className={`mt-6 rounded-xl border p-5 ${
            result.passed
              ? "border-success/40 bg-success/10"
              : "border-danger/40 bg-danger/10"
          }`}
          role="status"
        >
          <div className="flex items-center gap-3">
            <span
              className={`inline-flex items-center rounded-md px-2.5 py-1 text-sm font-bold text-white ${
                result.passed ? "bg-success" : "bg-danger"
              }`}
            >
              {result.passed ? "합격" : "불합격"}
            </span>
            <p className="text-sm text-text">{result.passReason}</p>
          </div>
        </div>

        <div className="mt-6 rounded-2xl border border-border bg-gradient-to-br from-surface via-surface to-transparent p-8 text-center">
          <p className="text-xs font-semibold uppercase tracking-wider text-text-muted">
            점수
          </p>
          <p
            className={`mt-2 inline-block text-6xl font-bold tabular-nums animate-score-pop ${rateTone}`}
          >
            {result.score}
            <span className="text-3xl text-text-subtle">점</span>
          </p>
          <p className="mt-3 text-sm text-text-muted">
            {result.correctCount} / {result.totalCount} 정답 ·{" "}
            <span className={rateTone}>{correctRate}%</span>
          </p>
        </div>

        {/* 과목별 점수 — 단일 과목 자격증(정처기 실기 등)도 학습 진단으로 표시 */}
        {result.subjectScores && result.subjectScores.length > 0 && (
          <div className="mt-6 rounded-xl border border-border bg-surface p-5">
            <p className="text-sm font-semibold text-text">
              {exam.examType === "ENGINEER_PRACTICAL"
                ? "카테고리별 학습 진단"
                : "과목별 점수"}
            </p>
            <ul className="mt-3 space-y-2">
              {result.subjectScores.map((s) => (
                <li key={s.subjectName} className="flex items-center gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-2 text-sm">
                      <span className="truncate text-text">{s.subjectName}</span>
                      <span className="tabular-nums text-text-muted">
                        {s.correct}/{s.total} · {s.weighted}점
                      </span>
                    </div>
                    <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-border">
                      <div
                        className={`h-full transition-all ${
                          s.failed ? "bg-danger" : "bg-primary"
                        }`}
                        style={{ width: `${Math.max(2, s.weighted)}%` }}
                      />
                    </div>
                  </div>
                  {s.failed && (
                    <span className="shrink-0 rounded-md bg-danger/15 px-2 py-0.5 text-[11px] font-bold text-danger">
                      과락
                    </span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        <PastExamReviewList exam={exam} items={result.items} />

        <div className="mt-8 flex flex-wrap gap-3">
          <button
            onClick={onRestart}
            className="flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-text-muted transition hover:text-text"
          >
            다시 응시
          </button>
          {result.solveId != null ? (
            <button
              onClick={() => router.push(`/history/${result.solveId}`)}
              className={`flex-1 rounded-lg py-3 text-sm font-semibold text-white transition ${token.tailwind.bg} hover:opacity-90`}
            >
              상세 보기
            </button>
          ) : (
            <Link
              href={backHref}
              className={`flex-1 rounded-lg py-3 text-center text-sm font-semibold text-white transition ${token.tailwind.bg} hover:opacity-90`}
            >
              다른 회차 보기
            </Link>
          )}
        </div>

        <div className="mt-3 text-center">
          <Link
            href={backHref}
            className="text-xs text-text-muted transition hover:text-text"
          >
            ← 기출 복원 목록으로
          </Link>
        </div>
      </Container>
    </main>
  );
}

function QuestionTypeBadge({ type }: { type: PastExamQuestion["questionType"] }) {
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

function MCQOptions({
  options,
  selected,
  onSelect,
  cert,
}: {
  options: string[];
  selected: number | null;
  onSelect: (value: number) => void;
  cert: CertKey;
}) {
  const token = CERT_TOKENS[cert];

  return (
    <ul className="space-y-2">
      {options.map((optionText, index) => {
        const num = index + 1;
        const isSelected = selected === num;

        return (
          <li key={num}>
            <button
              onClick={() => onSelect(num)}
              className={`w-full rounded-lg border px-4 py-3 text-left text-base transition-all duration-150 ease-out ${
                isSelected
                  ? `${token.tailwind.border} ${token.tailwind.bgSoft} text-text animate-tap-bounce`
                  : `border-border text-text ${token.tailwind.borderHover} hover:-translate-y-[1px] hover:shadow-sm hover:scale-[1.01]`
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
  onChange: (value: string) => void;
  onEnter: () => void;
}) {
  return (
    <div>
      <label className="mb-2 block text-xs text-text-muted">정답 입력</label>
      <input
        type="text"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            onEnter();
          }
        }}
        placeholder="정답을 입력하세요. (엔터: 다음 문항)"
        className="w-full rounded-lg border border-border bg-bg px-4 py-3 font-mono text-base text-text placeholder:text-text-subtle transition focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60"
        autoComplete="off"
        spellCheck={false}
      />
      <p className="mt-2 text-xs text-text-subtle">
        대소문자와 공백은 자동으로 정규화해서 채점합니다.
      </p>
    </div>
  );
}

function DescriptiveInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div>
      <label className="mb-2 block text-xs text-text-muted">서술형 답안</label>
      <div className="relative">
        <textarea
          value={value}
          onChange={(event) => onChange(event.target.value)}
          rows={8}
          placeholder="핵심 키워드와 설명이 드러나도록 답안을 작성하세요."
          className="w-full resize-y rounded-lg border border-border bg-bg px-4 py-3 text-base leading-relaxed text-text placeholder:text-text-subtle transition focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60"
        />
        <span className="pointer-events-none absolute bottom-2 right-3 text-xs tabular-nums text-text-subtle">
          {value.length}자
        </span>
      </div>
      <p className="mt-2 text-xs text-text-subtle">
        핵심 키워드 포함 비율에 따라 부분 점수가 반영될 수 있습니다.
      </p>
    </div>
  );
}

function formatTime(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
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
          title={running ? "일시정지" : "시작"}
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

function cnProgress(bgClass: string, done: boolean): string {
  return `h-full transition-all ${done ? bgClass : `${bgClass} opacity-60`}`;
}

// ── 결과 화면 문제별 상세 아코디언 ─────────────────────────────
// /solve 의 SessionReviewList 와 동일한 패턴.
// 데이터는 모두 클라이언트에 와 있으므로 비로그인·로그인 동일 동작.

function PastExamReviewList({
  exam,
  items,
}: {
  exam: PastExamDetail;
  items: PastExamGradedItem[];
}) {
  const [openIdx, setOpenIdx] = useState<number | null>(null);

  const itemMap = useMemo(() => {
    const map = new Map<number, PastExamGradedItem>();
    for (const it of items) map.set(it.questionId, it);
    return map;
  }, [items]);

  if (exam.questions.length === 0) return null;

  return (
    <section className="mt-10">
      <h2 className="text-lg font-semibold tracking-tight">문제별 상세</h2>
      <p className="mt-1 text-xs text-text-muted">
        오답·정답 모두 다시 보고 해설을 확인하세요. 카드를 클릭하면 펼쳐집니다.
      </p>
      <div className="mt-4 space-y-2">
        {exam.questions.map((question, index) => {
          const item = itemMap.get(question.id);
          if (!item) return null;
          return (
            <PastExamReviewCard
              key={question.id}
              index={index}
              question={question}
              item={item}
              open={openIdx === index}
              onToggle={() => setOpenIdx(openIdx === index ? null : index)}
            />
          );
        })}
      </div>
    </section>
  );
}

function PastExamReviewCard({
  index,
  question,
  item,
  open,
  onToggle,
}: {
  index: number;
  question: PastExamQuestion;
  item: PastExamGradedItem;
  open: boolean;
  onToggle: () => void;
}) {
  const parsed = parseQuestion(question.content);
  const preview = parsed.body.replace(/\s+/g, " ").trim();
  const previewShort = preview.length > 40 ? preview.slice(0, 40) + "…" : preview;
  const answered =
    item.selectedOption != null ||
    (item.submittedAnswerText != null && item.submittedAnswerText.trim() !== "");

  return (
    <Card padding="none" className="overflow-hidden">
      <button
        onClick={onToggle}
        className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-surface-hover"
      >
        <span
          className={`inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
            !answered
              ? "bg-surface-hover text-text-subtle"
              : item.correct
                ? "bg-success/15 text-success"
                : "bg-danger/15 text-danger"
          }`}
        >
          {!answered ? "–" : item.correct ? "✓" : "✗"}
        </span>
        <span className="text-xs font-semibold tabular-nums text-text-muted">
          문제 {index + 1}
        </span>
        <span className="flex-1 truncate text-sm text-text">{previewShort}</span>
        <span
          className={`text-xs text-text-subtle transition-transform ${open ? "rotate-180" : ""}`}
        >
          ▾
        </span>
      </button>

      {open && (
        <div className="border-t border-border bg-surface-subtle/50 px-4 py-4">
          <QuestionContent content={parsed.body} />

          {question.questionType === "MCQ" && parsed.options.length > 0 && (
            <ul className="mt-3 space-y-1.5">
              {parsed.options.map((opt, i) => {
                const num = i + 1;
                const isAnswer = num === item.correctOption;
                const isMine = num === item.selectedOption;
                return (
                  <li
                    key={num}
                    className={`rounded-md border px-3 py-2 text-sm ${
                      isAnswer
                        ? "border-success/40 bg-success/10"
                        : isMine
                          ? "border-danger/40 bg-danger/10"
                          : "border-border bg-bg"
                    }`}
                  >
                    <span className="mr-2 font-semibold tabular-nums">{num}.</span>
                    {opt}
                    {isAnswer && (
                      <span className="ml-2 text-[11px] font-bold text-success">정답</span>
                    )}
                    {isMine && !isAnswer && (
                      <span className="ml-2 text-[11px] font-bold text-danger">내 답</span>
                    )}
                  </li>
                );
              })}
            </ul>
          )}

          {question.questionType !== "MCQ" && (
            <div className="mt-3 rounded-md border border-border bg-bg p-3 text-sm">
              <p className="text-xs font-semibold text-text-muted">내 답</p>
              <p className="mt-1">
                {item.submittedAnswerText && item.submittedAnswerText.trim() !== "" ? (
                  item.submittedAnswerText
                ) : (
                  <span className="text-text-subtle">(미응답)</span>
                )}
              </p>
              {item.answer && (
                <>
                  <p className="mt-3 text-xs font-semibold text-text-muted">모범답안</p>
                  <p className="mt-1">{item.answer}</p>
                </>
              )}
              {item.keywords.length > 0 && (
                <p className="mt-2 text-xs text-text-muted">
                  키워드: {item.keywords.join(", ")}
                </p>
              )}
            </div>
          )}

          {item.explanation && (
            <div className="mt-3 rounded-md border border-primary/20 bg-primary/[0.04] p-3">
              <p className="text-[11px] font-bold uppercase tracking-wider text-primary">
                해설
              </p>
              <div className="mt-2 text-sm leading-relaxed">
                <QuestionContent content={item.explanation} />
              </div>
            </div>
          )}
        </div>
      )}
    </Card>
  );
}
