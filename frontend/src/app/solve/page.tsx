"use client";

import Link from "next/link";
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
import ReportQuestionButton from "@/components/ReportQuestionButton";
import { GradingDisclaimerModal } from "@/components/GradingDisclaimerModal";

type Phase = "select" | "solve" | "session-complete";

const SET_SIZE = 10;

/** root subject 이름으로 자격증 종류와 시각 톤을 판정 */
type CertTone = {
  certLabel: string;
  certBadge: string; // 짧은 약어
  /** Tailwind 색 — 좌측 컬러 바, 카드 hover 액센트, 뱃지 */
  bar: string;
  badge: string;
  hover: string;
};

const SQLD_TONE: CertTone = {
  certLabel: "SQLD",
  certBadge: "SQLD",
  bar: "bg-amber-500/60 group-hover:bg-amber-400",
  badge: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  hover: "hover:border-amber-500/40 hover:bg-amber-500/[0.04]",
};

const ENGINEER_TONE: CertTone = {
  certLabel: "정보처리기사 실기",
  certBadge: "정처기 실기",
  bar: "bg-emerald-500/60 group-hover:bg-emerald-400",
  badge: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  hover: "hover:border-emerald-500/40 hover:bg-emerald-500/[0.04]",
};

const COMPUTER_LITERACY_TONE: CertTone = {
  certLabel: "컴퓨터활용능력 1급",
  certBadge: "컴활 1급",
  bar: "bg-sky-500/60 group-hover:bg-sky-400",
  badge: "border-sky-500/40 bg-sky-500/10 text-sky-300",
  hover: "hover:border-sky-500/40 hover:bg-sky-500/[0.04]",
};

function detectCertTone(rootName: string): CertTone {
  if (rootName === "정보처리기사 실기") return ENGINEER_TONE;
  if (rootName === "컴퓨터활용능력 1급 필기") return COMPUTER_LITERACY_TONE;
  return SQLD_TONE; // SQLD 는 "1과목/2과목" 두 root 모두 포함
}

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
  /** 이번 세션에서 풀고 있는 10문제 — 종료 후 "같은 10문제 다시 풀기"용 */
  const [sessionQuestions, setSessionQuestions] = useState<Question[]>([]);

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
        handleReset(false);
      }
    }
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  });

  // 키보드 단축키 — 1~4 옵션 선택 / Enter 제출 또는 다음
  // input/textarea 포커스 시에는 동작 안 함 (입력 필드 자체 핸들러 우선)
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

  async function fetchQuestions(subjectId: number): Promise<Question[]> {
    return getQuestions(subjectId, SET_SIZE);
  }

  function resetCurrentInput() {
    setSelectedOption(null);
    setAnswerText("");
    setRevealed(false);
    setDetail(null);
  }

  function startSessionWithQuestions(qs: Question[]) {
    setSessionQuestions(qs);
    setQueue(qs.slice(1));
    setCurrent(qs[0] || null);
    resetCurrentInput();
    setSolvedCount(0);
    setCorrectCount(0);
    setPastEntries([]);
    setPhase("solve");
    if (typeof window !== "undefined") {
      window.history.pushState({ solve: 0 }, "");
    }
  }

  async function handleSelectSubject(subject: Subject) {
    setSelectedSubject(subject);
    setLoading(true);
    const qs = await fetchQuestions(subject.id);
    setLoading(false);
    startSessionWithQuestions(qs);
  }

  /** 같은 10문제를 그대로 다시 풀기 — fetch 없이 상태만 리셋 */
  function replaySameSession() {
    if (sessionQuestions.length === 0) return;
    startSessionWithQuestions(sessionQuestions);
  }

  /** 같은 과목에서 새 랜덤 10문제 fetch */
  async function newRandomSession() {
    if (!selectedSubject) return;
    setLoading(true);
    const qs = await fetchQuestions(selectedSubject.id);
    setLoading(false);
    startSessionWithQuestions(qs);
  }

  /** 이전 문제로 복원 — popstate 핸들러에서 호출 */
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
      return prev.slice(0, -1);
    });
  }

  function handleSelect(option: number) {
    if (revealed || !current) return;
    setSelectedOption(option);
  }

  function hasAnswer(): boolean {
    if (!current) return false;
    if (current.questionType === "MCQ") return selectedOption !== null;
    return answerText.trim().length > 0;
  }

  function isClientSideCorrect(d: QuestionDetail): boolean {
    if (d.questionType === "MCQ") {
      return selectedOption === d.correctOption;
    }
    const norm = (s: string) => s.trim().toLowerCase().replace(/\s+/g, " ");
    const submitted = norm(answerText);
    if (!submitted) return false;
    if (d.answer && norm(d.answer) === submitted) return true;
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
        answers: [
          {
            questionId: current.id,
            selectedOption: current.questionType === "MCQ" ? selectedOption ?? undefined : undefined,
            answerText: current.questionType !== "MCQ" ? answerText : undefined,
          },
        ],
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
        answers: [
          {
            questionId: current.id,
            selectedOption: current.questionType === "MCQ" ? selectedOption ?? undefined : undefined,
            answerText: current.questionType !== "MCQ" ? answerText : undefined,
          },
        ],
      });
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : "다시 실패했습니다.");
    }
  }

  async function handleNext() {
    if (!selectedSubject || !current) return;

    // 세션 종료 (10문제 풀이 완료)
    if (solvedCount >= SET_SIZE) {
      setPhase("session-complete");
      return;
    }

    let nextQueue = [...queue];
    if (nextQueue.length === 0) {
      setLoading(true);
      nextQueue = await fetchQuestions(selectedSubject.id);
      setLoading(false);
    }

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

    if (typeof window !== "undefined") {
      window.history.pushState({ solve: pastEntries.length + 1 }, "");
    }
  }

  function handleReset(popHistory: boolean = true) {
    if (popHistory && typeof window !== "undefined") {
      const pushed = pastEntries.length + 1;
      if (pushed > 0) {
        window.history.go(-pushed);
      }
    }
    setPhase("select");
    setSelectedSubject(null);
    setQueue([]);
    setCurrent(null);
    setPastEntries([]);
    setSessionQuestions([]);
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

  // ── 1. 과목 선택 ─────────────────────────────────────────
  if (phase === "select") {
    // 자격증별로 root 묶기 (SQLD는 "1과목/2과목" 두 root를 한 그룹으로 합침)
    const certGroups = new Map<string, { tone: CertTone; roots: Subject[] }>();
    for (const root of subjects) {
      const tone = detectCertTone(root.name);
      if (!certGroups.has(tone.certLabel)) {
        certGroups.set(tone.certLabel, { tone, roots: [] });
      }
      certGroups.get(tone.certLabel)!.roots.push(root);
    }

    return (
      <main className="min-h-screen bg-background text-foreground">
        <GradingDisclaimerModal />
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <h1 className="text-2xl font-bold sm:text-3xl">과목 선택</h1>
          <p className="mt-2 text-sm text-muted">
            과목 하나를 골라 {SET_SIZE}문제 한 세트를 풀어보세요.
          </p>

          <div className="mt-10 space-y-10">
            {Array.from(certGroups.values()).map(({ tone, roots }) => (
              <section key={tone.certLabel}>
                {/* 자격증 헤더 — 색상 뱃지로 강한 시각 구분 */}
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${tone.badge}`}
                  >
                    {tone.certBadge}
                  </span>
                  <span className="h-px flex-1 bg-border/60" />
                </div>

                {/* root 별로 세부 그룹 */}
                <div className="mt-4 space-y-5">
                  {roots.map((root) => {
                    const items = root.children.length > 0 ? root.children : [root];
                    // SQLD 만 root가 2개 (1과목/2과목)라 sub-heading 노출
                    // 정처기/컴활은 root가 1개라 sub-heading 생략 (중복 정보)
                    const showSubHeading =
                      tone === SQLD_TONE && root.children.length > 0;
                    return (
                      <div key={root.id}>
                        {showSubHeading && (
                          <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted/70">
                            {root.name}
                          </h3>
                        )}
                        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                          {items.map((child) => (
                            <button
                              key={child.id}
                              onClick={() => handleSelectSubject(child)}
                              className={`group flex items-center gap-3 rounded-lg border border-border bg-surface px-4 py-3 text-left transition-all duration-200 ${tone.hover}`}
                            >
                              <span
                                className={`h-9 w-1 shrink-0 rounded-full transition-colors ${tone.bar}`}
                              />
                              <span className="flex-1 text-sm font-medium">{child.name}</span>
                              <span className="text-xs text-muted/60 transition-transform group-hover:translate-x-0.5">
                                →
                              </span>
                            </button>
                          ))}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>
            ))}
          </div>

          {subjects.length === 0 && (
            <p className="mt-8 text-center text-muted">과목 데이터를 불러오는 중...</p>
          )}

          {/* 보조 액션 */}
          <div className="mt-12 flex items-center justify-center gap-6 text-sm text-muted">
            <Link href="/wrong-answers" className="transition-colors hover:text-foreground">
              오답 노트 →
            </Link>
            <span className="text-border">·</span>
            <Link href="/mock-exams" className="transition-colors hover:text-foreground">
              모의고사 →
            </Link>
          </div>
        </div>
      </main>
    );
  }

  // ── 3. 세션 종료 카드 ────────────────────────────────────
  if (phase === "session-complete") {
    const rate = SET_SIZE > 0 ? Math.round((correctCount / SET_SIZE) * 100) : 0;
    const ment =
      rate >= 90
        ? "완벽해요! 같은 과목을 더 풀어볼까요?"
        : rate >= 70
        ? "잘하고 있어요. 한 세트 더 풀면 손에 더 익을 거예요."
        : "괜찮아요. 약한 문제부터 다시 한 번 풀어보세요.";
    const rateColor = rate >= 90 ? "text-green-300" : rate >= 70 ? "text-amber-300" : "text-rose-300";

    return (
      <main className="min-h-screen bg-background text-foreground">
        <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
          <div className="rounded-2xl border border-amber-500/20 bg-gradient-to-br from-amber-500/[0.08] via-amber-500/[0.04] to-transparent p-8 sm:p-10">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-amber-300">
              세션 완료
            </p>
            <h1 className="mt-2 text-3xl font-bold sm:text-4xl">
              {selectedSubject?.name}
            </h1>

            <div className="mt-6 flex items-end gap-4">
              <span className={`text-6xl font-bold tabular-nums sm:text-7xl ${rateColor}`}>
                {correctCount}
                <span className="text-3xl text-muted/60">/{SET_SIZE}</span>
              </span>
              <span className={`mb-2 text-2xl font-bold tabular-nums ${rateColor}`}>{rate}%</span>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-muted">{ment}</p>

            {/* 핵심 분기 — 같은 10문제 vs 새 10문제 */}
            <div className="mt-8 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <button
                onClick={replaySameSession}
                className="group flex items-center justify-between gap-3 rounded-xl border border-amber-500/40 bg-amber-500/[0.08] px-5 py-4 text-left transition-all hover:border-amber-500/60 hover:bg-amber-500/[0.12]"
              >
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-amber-200">같은 10문제 다시</p>
                  <p className="mt-0.5 text-xs text-muted">방금 푼 문제로 약점 굳히기</p>
                </div>
                <svg className="h-5 w-5 shrink-0 text-amber-300 transition-transform group-hover:rotate-180" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
              <button
                onClick={newRandomSession}
                className="group flex items-center justify-between gap-3 rounded-xl bg-primary px-5 py-4 text-left text-zinc-900 transition-all hover:bg-primary-hover hover:scale-[1.01]"
              >
                <div className="min-w-0">
                  <p className="text-sm font-semibold">새 10문제</p>
                  <p className="mt-0.5 text-xs text-zinc-800/80">다른 랜덤 문제로 한 세트 더</p>
                </div>
                <svg className="h-5 w-5 shrink-0 transition-transform group-hover:translate-x-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              </button>
            </div>

            {/* 보조 액션 — 텍스트 링크 */}
            <div className="mt-5 flex flex-wrap items-center gap-5 text-sm text-muted">
              <Link
                href={selectedSubject ? `/wrong-answers?subjectId=${selectedSubject.id}` : "/wrong-answers"}
                className="transition-colors hover:text-foreground"
              >
                약한 문제 복습 →
              </Link>
              <button
                onClick={() => handleReset()}
                className="transition-colors hover:text-foreground"
              >
                다른 과목 선택 →
              </button>
            </div>
          </div>
        </div>
      </main>
    );
  }

  // ── 2. 문제 풀이 ─────────────────────────────────────────
  if (!current) return null;

  const parsed = parseQuestion(current.content);
  const isCorrect = detail
    ? detail.questionType === "MCQ"
      ? selectedOption === detail.correctOption
      : isClientSideCorrect(detail)
    : null;

  // 진행 바: 현재 N번째 = solvedCount + (revealed ? 0 : 1) — 풀이 중일 땐 그 문제까지 카운트
  const currentNumber = Math.min(solvedCount + 1, SET_SIZE);
  const progressPct = Math.min((solvedCount / SET_SIZE) * 100, 100);

  return (
    <main className="min-h-screen bg-background text-foreground pb-24">
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
        {/* 상단 바 */}
        <div className="flex items-center justify-between gap-3">
          <button
            onClick={() => handleReset()}
            className="text-sm text-muted transition-colors hover:text-foreground"
          >
            ← 과목 선택
          </button>
          <span className="truncate text-sm text-muted">{selectedSubject?.name}</span>
          <span className="rounded-full border border-border bg-surface/60 px-2.5 py-0.5 text-xs tabular-nums text-muted">
            {correctCount}/{solvedCount} 정답
          </span>
        </div>

        {/* 진행 바 */}
        <div className="mt-5">
          <div className="flex items-center justify-between text-[11px] text-muted">
            <span className="tabular-nums">
              <span className="font-semibold text-foreground">{currentNumber}</span>
              <span className="text-muted/60"> / {SET_SIZE}</span>
            </span>
            <span className="tabular-nums">{Math.round(progressPct)}%</span>
          </div>
          <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-border/40">
            <div
              className="h-full rounded-full bg-amber-500 transition-all duration-500 ease-out"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>

        {/* 문제 카드 */}
        <div className="mt-5 rounded-xl border border-border bg-surface p-6 sm:p-7">
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-medium text-muted">{selectedSubject?.name}</p>
            <div className="flex items-center gap-3">
              <ReportQuestionButton questionId={current.id} />
              <QuestionTypeBadge type={current.questionType} />
            </div>
          </div>

          <div className="mt-5 text-base leading-relaxed">
            <QuestionContent content={parsed.body} />
          </div>

          {/* 입력 UI */}
          {current.questionType === "MCQ" && (
            <ul className="mt-6 space-y-2">
              {parsed.options.map((optionText, idx) => {
                const num = idx + 1;
                const isSelected = selectedOption === num;
                const isCorrectOption = detail?.correctOption === num;

                let style = "border-border hover:border-amber-500/40 hover:bg-amber-500/5";
                if (revealed) {
                  if (isCorrectOption) {
                    style = "border-green-500/60 bg-green-500/10 text-green-300";
                  } else if (isSelected && !isCorrectOption) {
                    style = "border-red-500/60 bg-red-500/10 text-red-300";
                  } else {
                    style = "border-border opacity-50";
                  }
                } else if (isSelected) {
                  style = "border-amber-500/60 bg-amber-500/10 text-foreground ring-1 ring-amber-400/40";
                }

                return (
                  <li key={num}>
                    <button
                      onClick={() => handleSelect(num)}
                      disabled={revealed}
                      className={`flex w-full items-start gap-3 rounded-lg border px-4 py-3 text-left text-sm leading-relaxed transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 ${style} disabled:cursor-default`}
                    >
                      <span
                        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                          isSelected && !revealed
                            ? "bg-amber-500/30 text-amber-100"
                            : revealed && isCorrectOption
                            ? "bg-green-500/30 text-green-100"
                            : revealed && isSelected
                            ? "bg-red-500/30 text-red-100"
                            : "border border-current text-current"
                        }`}
                      >
                        {num}
                      </span>
                      <span className="flex-1">{optionText}</span>
                      {revealed && isCorrectOption && <span className="text-green-400">✓</span>}
                      {revealed && isSelected && !isCorrectOption && <span className="text-red-400">✗</span>}
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
              <p className="mt-2 text-xs text-muted/70">대소문자, 앞뒤 공백은 자동으로 무시됩니다.</p>
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
          <div className="mt-5 flex justify-center">
            <button
              onClick={handleSubmit}
              disabled={!hasAnswer()}
              className={`rounded-lg bg-primary px-7 py-2.5 text-sm font-semibold text-zinc-900 transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-primary ${
                hasAnswer() ? "hover:bg-primary-hover hover:scale-[1.02] shadow-[0_0_18px_rgba(245,158,11,0.25)]" : ""
              }`}
            >
              정답 제출
            </button>
          </div>
        )}

        {/* 정답 확인 후: 결과 띠 + 해설 */}
        {revealed && detail && (
          <div className="mt-5 space-y-4">
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

            {/* 결과 띠 + 인라인 다음 버튼 */}
            <div
              className={`flex flex-wrap items-center justify-between gap-3 rounded-lg border px-5 py-4 ${
                isCorrect
                  ? "border-green-500/40 bg-green-500/[0.08]"
                  : "border-red-500/40 bg-red-500/[0.08]"
              }`}
            >
              <div className={`flex items-center gap-2 text-sm font-semibold ${isCorrect ? "text-green-300" : "text-red-300"}`}>
                <span className="text-lg">{isCorrect ? "✓" : "✗"}</span>
                {isCorrect
                  ? "정답입니다!"
                  : detail.questionType === "MCQ"
                  ? `오답 — 정답은 ${detail.correctOption}번입니다.`
                  : `오답 — 모범답안: ${detail.answer ?? "(없음)"}`}
              </div>
              <button
                onClick={handleNext}
                className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-xs font-semibold text-zinc-900 transition-all hover:bg-primary-hover hover:scale-[1.02]"
              >
                {solvedCount >= SET_SIZE ? "결과 보기" : "다음 문제"}
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              </button>
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
          </div>
        )}
      </div>

      {/* 화면 하단 fixed 다음 버튼 — 해설 길어도 한 번에 누르기 */}
      {revealed && detail && (
        <button
          onClick={handleNext}
          className="fixed bottom-6 right-6 z-50 inline-flex items-center gap-1.5 rounded-full bg-primary px-5 py-3 text-sm font-semibold text-zinc-900 shadow-[0_0_24px_rgba(245,158,11,0.45)] transition-all hover:bg-primary-hover hover:scale-[1.05]"
          aria-label={solvedCount >= SET_SIZE ? "결과 보기" : "다음 문제"}
        >
          {solvedCount >= SET_SIZE ? "결과 보기" : "다음 문제"}
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
          </svg>
        </button>
      )}
    </main>
  );
}

function QuestionTypeBadge({ type }: { type: Question["questionType"] }) {
  if (type === "MCQ") {
    return (
      <span className="rounded-full border border-border px-2 py-0.5 text-[10px] font-medium text-muted">
        4지선다
      </span>
    );
  }
  if (type === "SHORT_ANSWER") {
    return (
      <span className="rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-bold text-emerald-300">
        단답형
      </span>
    );
  }
  return (
    <span className="rounded-full border border-cyan-500/40 bg-cyan-500/10 px-2 py-0.5 text-[10px] font-bold text-cyan-300">
      서술형
    </span>
  );
}
