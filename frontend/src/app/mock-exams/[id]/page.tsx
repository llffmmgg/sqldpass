"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import dynamic from "next/dynamic";

const CountdownCircleTimer = dynamic(
  () => import("react-countdown-circle-timer").then((m) => m.CountdownCircleTimer),
  { ssr: false },
);
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import QuestionContent from "@/components/QuestionContent";
import { Container } from "@/components/ui";
import ReportQuestionButton from "@/components/ReportQuestionButton";
import BookmarkButton from "@/components/BookmarkButton";
import { parseQuestion } from "@/lib/parseQuestion";
import {
  getMockExam,
  type MockExamDetail,
  type MockExamQuestion,
} from "@/lib/mockExamApi";
import {
  getSolves,
  submitSolve,
  type SolveAnswerRequest,
  type SolveResponse,
  type SolveSummaryResponse,
} from "@/lib/api";
import MockExamAttemptsView from "@/components/MockExamAttemptsView";
import { ExamBadge } from "@/app/mock-exams/page";
import { GradingDisclaimerModal } from "@/components/GradingDisclaimerModal";
import AdInfeed from "@/components/AdInfeed";
import AdDisplay from "@/components/AdDisplay";
import { useToast } from "@/components/Toast";
import { trackEvent } from "@/lib/gtag";
import { hapticError, hapticLight, hapticSuccess } from "@/lib/haptic";

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
  const toast = useToast();
  const id = Number(params?.id);

  const [exam, setExam] = useState<MockExamDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [answers, setAnswers] = useState<Map<number, AnswerState>>(new Map());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SolveResponse | null>(null);
  const [attempts, setAttempts] = useState<SolveSummaryResponse[] | null>(null);
  const [started, setStarted] = useState(false);

  // ── 타이머 ──
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

  // 제출 시 타이머 정지
  useEffect(() => {
    if (result) pauseTimer();
  }, [result, pauseTimer]);

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  useEffect(() => {
    if (!id) return;
    Promise.all([
      getMockExam(id),
      getSolves({ mockExamId: id }).catch(() => [] as SolveSummaryResponse[]),
    ])
      .then(([data, mySolves]) => {
        setExam(data);
        setAttempts(mySolves);
        // GA4 — 모의고사 시작
        trackEvent("start_exam", {
          exam_id: data.id,
          exam_type: data.examType,
          exam_name: data.name,
        });
      })
      .catch((e) => setError(e instanceof Error ? e.message : "모의고사를 불러올 수 없습니다."));
  }, [id]);

  const answeredCount = useMemo(
    () => Array.from(answers.values()).filter(hasAnswer).length,
    [answers]
  );

  // 시험 진행 중 실수로 탭 닫기/새로고침 → 답안 소실 경고
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
    const isLocked = error.includes("프리미엄");
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center px-4">
        <div className="max-w-md text-center">
          {isLocked ? (
            <>
              <div className="text-6xl">🔒</div>
              <h1 className="mt-4 text-2xl font-bold text-amber-300">프리미엄 모의고사</h1>
              <p className="mt-3 text-sm text-muted leading-relaxed">
                이 회차는 잠금 컨텐츠입니다. 결제 후 잠금을 해제하면 풀이할 수 있습니다.
                <br />
                결제 시스템은 곧 오픈됩니다.
              </p>
            </>
          ) : (
            <p className="text-red-400">{error}</p>
          )}
          <button
            onClick={() => router.push("/mock-exams")}
            className="mt-6 rounded-lg border border-border px-4 py-2 text-sm text-muted hover:text-foreground"
          >
            ← 모의고사 목록으로
          </button>
        </div>
      </main>
    );
  }

  if (!exam || attempts === null) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <Spinner message="모의고사 불러오는 중..." />
      </main>
    );
  }

  // 시도 1개 이상 + 아직 새로 풀기 안 시작 + 결과 화면도 아닐 때 → 인터스티셜
  if (attempts.length > 0 && !started && !result) {
    return (
      <MockExamAttemptsView
        attempts={attempts}
        examTitle={exam.name}
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

  const isEngineer = exam.examType === "ENGINEER_PRACTICAL";
  // 시험 종류에 따른 액센트 색상 클래스
  const accent = isEngineer
    ? {
        bg: "bg-emerald-500 hover:bg-emerald-400",
        text: "text-emerald-300",
        border: "border-emerald-500",
        ring: "focus:ring-emerald-500/30 focus:border-emerald-500",
        progress: "bg-emerald-500",
        progressPartial: "bg-emerald-500/60",
        chipActive: "bg-emerald-500 text-zinc-900",
        chipAnswered: "bg-emerald-500/20 text-foreground",
        hoverBorder: "hover:border-emerald-500/40",
      }
    : {
        bg: "bg-primary hover:bg-primary-hover",
        text: "text-amber-300",
        border: "border-amber-500",
        ring: "focus:ring-amber-500/30 focus:border-amber-500",
        progress: "bg-primary",
        progressPartial: "bg-amber-500/70",
        chipActive: "bg-primary text-primary-fg",
        chipAnswered: "bg-primary/20 text-foreground",
        hoverBorder: "hover:border-amber-500/40",
      };

  // 결과 화면
  if (result) {
    return (
      <main className="min-h-screen bg-background text-foreground">
        <Container size="narrow" className="py-16">
          <ExamBadge examType={exam.examType} />
          <h1 className="mt-3 text-2xl font-bold">{exam.name} 결과</h1>
          <div className="mt-8 rounded-xl border border-border bg-surface p-8 text-center">
            <p className="text-sm text-muted">점수</p>
            <p className={`mt-2 inline-block text-5xl font-bold animate-score-pop ${accent.text}`}>{result.score}점</p>
            <p className="mt-4 text-sm text-muted">
              {result.correctCount} / {result.totalCount} 정답
            </p>
          </div>
          <div className="mt-6 md:hidden">
            <AdInfeed
              adSlot="5227022543"
              adLayoutKey="-h4-h+1c-4h+8p"
            />
          </div>
          <div className="mt-6 hidden md:block">
            <AdDisplay adSlot="3622084801" />
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
              className={`flex-1 rounded-lg ${accent.bg} py-3 text-sm font-semibold text-zinc-900`}
            >
              상세 보기
            </button>
          </div>
        </Container>
      </main>
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
    if (!exam) return;
    if (answeredCount === 0) return;
    if (answeredCount < total) {
      const ok = confirm(
        `미답 ${total - answeredCount}문항은 오답으로 처리됩니다. 제출하시겠습니까?`
      );
      if (!ok) return;
    }

    setSubmitting(true);
    try {
      const payload = {
        mockExamId: exam.id,
        answers: exam.questions.map<SolveAnswerRequest>((q) => {
          const a = answers.get(q.id);
          if (q.questionType === "MCQ") {
            // 미답 MCQ 는 selectedOption 을 보내지 않는다 (백엔드는 NULL 저장).
            return a?.option != null
              ? { questionId: q.id, selectedOption: a.option }
              : { questionId: q.id };
          }
          return { questionId: q.id, answerText: a?.text ?? "" };
        }),
      };
      const res = await submitSolve(payload);
      setResult(res);
      if (res.totalCount > 0 && res.correctCount * 2 >= res.totalCount) {
        hapticSuccess();
      } else {
        hapticError();
      }
      if (res.milestoneReached) {
        toast.show(`🎉 ${res.milestoneReached}일 연속 학습! 잘하고 있어요`, "success");
      }
      // GA4 — 모의고사 완료
      trackEvent("complete_exam", {
        exam_id: exam.id,
        exam_type: exam.examType,
        score: res.score,
        correct_count: res.correctCount,
        total_count: res.totalCount,
      });
    } catch (e) {
      alert(e instanceof Error ? e.message : "제출에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  // 빠른 이동 그리드 — 20문항 이하면 5열, 이상이면 10열
  const jumpGridCols = total <= 20 ? "grid-cols-5 sm:grid-cols-10" : "grid-cols-10";

  return (
    <main className="min-h-screen bg-background text-foreground">
      <GradingDisclaimerModal />
      <Container size="default" className="py-12">
      <div className="flex gap-4 items-stretch">
      {/* 타이머 패널 — 데스크톱에서 왼쪽 sticky, 모바일에서 하단 fixed */}
      <div className="hidden lg:block">
        <div className="sticky top-20 w-32">
          <div className="flex flex-col items-center gap-3 rounded-xl border border-border bg-surface/80 px-3 py-4 shadow-lg backdrop-blur">
            <ExamTimer
              seconds={timerSeconds}
              limit={timerLimit}
              running={timerRunning}
              onStart={startTimer}
              onPause={pauseTimer}
              onReset={resetTimer}
              accent={accent}
            />
          </div>
        </div>
      </div>

      {/* 메인 콘텐츠 */}
      <div className="min-w-0 flex-1 max-w-3xl mx-auto">
        {/* 상단 상태 바 */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <ExamBadge examType={exam.examType} />
              <p className="truncate text-xs text-muted">{exam.name}</p>
            </div>
            <p className="mt-1 text-lg font-bold tabular-nums">
              {currentIdx + 1} <span className="text-muted">/</span> {total}
              <span className="ml-2 text-xs font-medium text-muted">
                답안 {answeredCount}/{total}
              </span>
            </p>
          </div>
          <button
            onClick={handleSubmit}
            disabled={submitting || answeredCount === 0}
            className={`shrink-0 rounded-lg ${accent.bg} px-5 py-2.5 text-sm font-semibold text-zinc-900 shadow-sm transition disabled:opacity-40`}
          >
            {submitting ? "제출 중..." : `제출하기 (${answeredCount}/${total})`}
          </button>
        </div>
        <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-border">
          <div
            className={`h-full transition-all ${
              answeredCount === total ? accent.progress : accent.progressPartial
            }`}
            style={{ width: `${(answeredCount / total) * 100}%` }}
          />
        </div>

        {/* 문제 */}
        <div className="mt-6 rounded-xl border border-border bg-surface p-6">
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-medium text-muted">{current.subjectName}</p>
            <div className="flex items-center gap-3">
              <BookmarkButton questionId={current.id} />
              <ReportQuestionButton questionId={current.id} />
              <QuestionTypeBadge type={current.questionType} />
            </div>
          </div>
          <h2 className="mt-2 text-lg font-semibold">문항 {currentIdx + 1}</h2>
          <div className="mt-4">
            <QuestionContent content={parsed.body} />
          </div>

          {/* 입력 UI 분기 */}
          <div className="mt-6">
            {current.questionType === "MCQ" && parsed.options.length > 0 && (
              <MCQOptions
                options={parsed.options}
                selected={currentAnswer?.option ?? null}
                onSelect={selectOption}
                accent={accent}
              />
            )}

            {current.questionType === "SHORT_ANSWER" && (
              <ShortAnswerInput
                value={currentAnswer?.text ?? ""}
                onChange={setAnswerText}
                onEnter={goNext}
                accent={accent}
              />
            )}

            {current.questionType === "DESCRIPTIVE" && (
              <DescriptiveInput
                value={currentAnswer?.text ?? ""}
                onChange={setAnswerText}
                accent={accent}
              />
            )}
          </div>
        </div>

        {/* 이동 */}
        <div className="mt-6 flex gap-3">
          <button
            onClick={goPrev}
            disabled={currentIdx === 0}
            className="flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-muted disabled:opacity-30 hover:text-foreground"
          >
            ← 이전
          </button>
          <button
            onClick={goNext}
            disabled={currentIdx >= total - 1}
            className={`flex-1 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-foreground disabled:opacity-30 ${accent.hoverBorder}`}
          >
            다음 →
          </button>
        </div>

        {/* 빠른 이동 */}
        <div className="mt-6">
          <p className="text-xs text-muted mb-2">빠른 이동</p>
          <div className={`grid gap-1.5 ${jumpGridCols}`}>
            {exam.questions.map((q, i) => {
              const answered = hasAnswer(answers.get(q.id));
              return (
                <button
                  key={q.id}
                  onClick={() => setCurrentIdx(i)}
                  className={`h-9 rounded text-xs font-medium transition ${
                    i === currentIdx
                      ? accent.chipActive
                      : answered
                      ? accent.chipAnswered
                      : "bg-surface text-muted hover:bg-border"
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
          <button
            onClick={handleSubmit}
            disabled={submitting || answeredCount === 0}
            className={`w-full rounded-xl ${accent.bg} py-4 text-base font-bold text-zinc-900 shadow-sm transition disabled:opacity-40`}
          >
            {submitting ? "제출 중..." : "제출하기"}
          </button>
          {answeredCount < total && answeredCount > 0 && (
            <p className="mt-2 text-center text-xs text-muted">
              미답 {total - answeredCount}문항은 오답으로 처리됩니다
            </p>
          )}
          {answeredCount === 0 && (
            <p className="mt-2 text-center text-xs text-muted">
              최소 1문항 이상 답안을 작성해야 제출할 수 있습니다
            </p>
          )}
        </div>
      </div>

      {/* 모바일 타이머 — 우하단 고정 */}
      <div className="fixed bottom-4 right-4 z-40 lg:hidden">
        <div className="flex flex-col items-center gap-2 rounded-xl border border-border bg-surface/95 px-3 py-3 shadow-xl backdrop-blur">
          <ExamTimer
            seconds={timerSeconds}
            limit={timerLimit}
            running={timerRunning}
            onStart={startTimer}
            onPause={pauseTimer}
            onReset={resetTimer}
            accent={accent}
          />
        </div>
      </div>

      </div>
      </Container>
    </main>
  );
}

// ================= 하위 컴포넌트 =================

type Accent = {
  bg: string;
  text: string;
  border: string;
  ring: string;
  progress: string;
  progressPartial: string;
  chipActive: string;
  chipAnswered: string;
  hoverBorder: string;
};

function QuestionTypeBadge({ type }: { type: MockExamQuestion["questionType"] }) {
  if (type === "MCQ") {
    return <span className="rounded-full border border-border px-2 py-0.5 text-[10px] font-medium text-muted">4지선다</span>;
  }
  if (type === "SHORT_ANSWER") {
    return <span className="rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold text-emerald-300">단답형</span>;
  }
  return <span className="rounded-full border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[10px] font-bold text-cyan-300">서술형</span>;
}

function MCQOptions({
  options,
  selected,
  onSelect,
  accent,
}: {
  options: string[];
  selected: number | null;
  onSelect: (n: number) => void;
  accent: Accent;
}) {
  return (
    <ul className="space-y-2">
      {options.map((optionText, idx) => {
        const num = idx + 1;
        const isSelected = selected === num;
        return (
          <li key={num}>
            <button
              onClick={() => onSelect(num)}
              className={`w-full rounded-lg border px-4 py-3 text-left text-base transition-all duration-150 ease-out ${
                isSelected
                  ? `${accent.border} bg-amber-500/10 text-foreground animate-tap-bounce`
                  : `border-border ${accent.hoverBorder} hover:bg-amber-500/5 hover:-translate-y-[1px] hover:shadow-sm hover:scale-[1.01]`
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
  accent,
}: {
  value: string;
  onChange: (v: string) => void;
  onEnter: () => void;
  accent: Accent;
}) {
  return (
    <div>
      <label className="mb-2 block text-xs text-muted">정답 입력</label>
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
        className={`w-full rounded-lg border border-border bg-background px-4 py-3 font-mono text-base text-foreground placeholder:text-muted/70 transition focus:outline-none focus:ring-2 ${accent.ring}`}
        autoComplete="off"
        spellCheck={false}
      />
      <p className="mt-2 text-xs text-muted/70">
        대소문자, 앞뒤 공백은 자동으로 무시됩니다. 동의어/영문 표기도 정답으로 인정됩니다.
      </p>
    </div>
  );
}

function DescriptiveInput({
  value,
  onChange,
  accent,
}: {
  value: string;
  onChange: (v: string) => void;
  accent: Accent;
}) {
  return (
    <div>
      <label className="mb-2 block text-xs text-muted">서술형 답안</label>
      <div className="relative">
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          rows={8}
          placeholder="개념을 설명하는 답안을 작성하세요. 핵심 키워드를 포함할수록 점수가 올라갑니다."
          className={`w-full resize-y rounded-lg border border-border bg-background px-4 py-3 text-base leading-relaxed text-foreground placeholder:text-muted/70 transition focus:outline-none focus:ring-2 ${accent.ring}`}
        />
        <span className="pointer-events-none absolute bottom-2 right-3 text-xs tabular-nums text-muted/60">
          {value.length} 자
        </span>
      </div>
      <p className="mt-2 text-xs text-muted/70">
        채점 방식: 핵심 키워드 포함률 70% 이상 + 충분한 분량 → 정답, 40% 이상 → 부분점수.
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

function ExamTimer({
  seconds,
  limit,
  running,
  onStart,
  onPause,
  onReset,
  accent,
}: {
  seconds: number;
  limit: number;
  running: boolean;
  onStart: () => void;
  onPause: () => void;
  onReset: () => void;
  accent: Accent;
}) {
  const remaining = Math.max(limit - seconds, 0);
  const isOvertime = seconds > limit;
  const isUrgent = remaining <= 300 && remaining > 0;
  const size = 110;

  const accentHex = accent.text === "text-amber-400" ? "#f59e0b"
    : accent.text === "text-emerald-400" ? "#34d399"
    : accent.text === "text-sky-400" ? "#38bdf8"
    : accent.text === "text-rose-400" ? "#fb7185"
    : "#f59e0b";

  const timerColor = isOvertime ? "#ef4444" : isUrgent ? "#f87171" : accentHex;
  const trailColor = "rgba(128,128,128,0.15)";

  // 시작 전
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
            <svg className="h-7 w-7 text-muted" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}
        </CountdownCircleTimer>
        <span className="text-xs font-medium text-muted">{Math.floor(limit / 60)}분</span>
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
                ? "text-red-400"
                : isUrgent
                ? "text-amber-400 animate-pulse"
                : accent.text
            }`}
          >
            {isOvertime ? `+${formatTime(seconds - limit)}` : formatTime(remaining)}
          </span>
        )}
      </CountdownCircleTimer>
      {/* 컨트롤 버튼 */}
      <div className="flex items-center gap-2">
        <button
          onClick={running ? onPause : onStart}
          className="rounded-md p-1.5 text-muted transition-colors hover:text-foreground hover:bg-border/50"
          title={running ? "일시정지" : "재개"}
        >
          {running ? (
            <svg className="h-4.5 w-4.5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z" />
            </svg>
          ) : (
            <svg className="h-4.5 w-4.5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" />
            </svg>
          )}
        </button>
        <button
          onClick={onReset}
          className="rounded-md p-1.5 text-muted transition-colors hover:text-foreground hover:bg-border/50"
          title="리셋"
        >
          <svg className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182" />
          </svg>
        </button>
      </div>
    </div>
  );
}
