"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import {
  getSubjects,
  getQuestions,
  getQuestionDetail,
  submitSolve,
  type Subject,
  type Question,
  type QuestionDetail,
} from "@/lib/api";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import {
  getSolveQuota,
  incrementAnonymousSolve as incrementAnonymousSolveQuota,
  type PublicSolveQuota,
} from "@/lib/publicApi";
import { parseQuestion } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import Spinner from "@/components/Spinner";
import ReportQuestionButton from "@/components/ReportQuestionButton";
import BookmarkButton from "@/components/BookmarkButton";
import { GradingDisclaimerModal } from "@/components/GradingDisclaimerModal";
import AdInfeed from "@/components/AdInfeed";
import AdDisplay from "@/components/AdDisplay";
import { useToast } from "@/components/Toast";
import { Badge, Button, Card, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromRootName,
  type CertKey,
  type CertToken,
} from "@/lib/cert-tokens";
import { hapticError, hapticLight, hapticSuccess } from "@/lib/haptic";

type Phase = "select" | "solve" | "session-complete";

const SET_SIZE = 10;

export default function SolvePage() {
  return (
    <Suspense fallback={null}>
      <SolvePageContent />
    </Suspense>
  );
}

// 비로그인 사용자용 공개 API 호출 (client-side, 토큰 없이 /api/public/*).
async function fetchPublic<T>(path: string): Promise<T> {
  const res = await fetch(`/api/public${path}`);
  if (!res.ok) throw new Error(`public API ${path} failed: ${res.status}`);
  return res.json();
}

type PublicSolveQuestion = { id: number; subjectId: number; content: string; questionType: Question["questionType"] };
type PublicQuestionDetail = {
  id: number;
  categoryId: number;
  content: string;
  questionType: QuestionDetail["questionType"];
  correctOption: number | null;
  answer: string | null;
  keywords: string[];
  explanation: string | null;
};

type PastEntry = {
  question: Question;
  selectedOption: number | null;
  answerText: string;
  revealed: boolean;
  detail: QuestionDetail | null;
};

function SolvePageContent() {
  const searchParams = useSearchParams();
  const certParam = searchParams?.get("cert");
  const toast = useToast();

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
  const [pastEntries, setPastEntries] = useState<PastEntry[]>([]);
  const [sessionQuestions, setSessionQuestions] = useState<Question[]>([]);
  const [loggedIn, setLoggedIn] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);
  const [anonQuota, setAnonQuota] = useState<PublicSolveQuota | null>(null);
  const [quotaExhausted, setQuotaExhausted] = useState(false);

  useEffect(() => {
    const logged = isLoggedIn();
    setLoggedIn(logged);
    setAuthChecked(true);
    const subjectPromise = logged
      ? getSubjects()
      : fetchPublic<Subject[]>("/subjects");
    subjectPromise.then(setSubjects).catch(() => setSubjects([]));
    if (!logged) {
      getSolveQuota()
        .then((q) => {
          setAnonQuota(q);
          if (q.exhausted) setQuotaExhausted(true);
        })
        .catch(() => {
          // 한도 조회 실패 시 풀이 자체는 막지 않는다 — 서버 가드가 최종 방어
        });
    }
  }, []);

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
    if (loggedIn) {
      return getQuestions(subjectId, SET_SIZE);
    }
    const raw = await fetchPublic<PublicSolveQuestion[]>(
      `/subjects/${subjectId}/random-questions?size=${SET_SIZE}`,
    );
    return raw;
  }

  async function fetchDetail(questionId: number): Promise<QuestionDetail> {
    if (loggedIn) {
      return getQuestionDetail(questionId);
    }
    const d = await fetchPublic<PublicQuestionDetail>(`/questions/${questionId}`);
    return {
      id: d.id,
      subjectId: d.categoryId,
      content: d.content,
      questionType: d.questionType,
      correctOption: d.correctOption,
      answer: d.answer,
      keywords: d.keywords ?? [],
      explanation: d.explanation ?? "",
    };
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
    // 비회원이고 이미 한도 소진이면 fetch 호출 자체를 건너뛰고 종료 화면으로
    if (!loggedIn && quotaExhausted) {
      setPhase("session-complete");
      return;
    }
    setLoading(true);
    try {
      const qs = await fetchQuestions(subject.id);
      startSessionWithQuestions(qs);
    } catch {
      // 서버 429 (한도 초과) 등 — 로딩 풀고 종료 화면으로 안내
      if (!loggedIn) setQuotaExhausted(true);
      setPhase("session-complete");
    } finally {
      setLoading(false);
    }
  }

  function replaySameSession() {
    if (!loggedIn && quotaExhausted) {
      setPhase("session-complete");
      return;
    }
    if (sessionQuestions.length === 0) return;
    startSessionWithQuestions(sessionQuestions);
  }

  async function newRandomSession() {
    if (!selectedSubject) return;
    if (!loggedIn && quotaExhausted) {
      setPhase("session-complete");
      return;
    }
    setLoading(true);
    try {
      const qs = await fetchQuestions(selectedSubject.id);
      startSessionWithQuestions(qs);
    } catch {
      if (!loggedIn) setQuotaExhausted(true);
      setPhase("session-complete");
    } finally {
      setLoading(false);
    }
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
      return prev.slice(0, -1);
    });
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

    const d = await fetchDetail(current.id);
    setDetail(d);
    setSolvedCount((c) => c + 1);
    const correct = isClientSideCorrect(d);
    if (correct) {
      setCorrectCount((c) => c + 1);
      hapticSuccess();
    } else {
      hapticError();
    }

    if (selectedSubject && !loggedIn) {
      // 비회원 풀이 카운터 증가 (집계 + IP 단위 일일 한도). 응답으로 받은 잔여 한도를 즉시 UI 에 반영.
      incrementAnonymousSolveQuota(1)
        .then((q) => {
          setAnonQuota(q);
          if (q.exhausted) setQuotaExhausted(true);
        })
        .catch(() => {
          // 집계 실패는 UX 에 영향 없으니 무시 — 서버 가드가 최종 방어
        });
    }

    if (selectedSubject && loggedIn) {
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
      })
        .then((res) => {
          if (res.milestoneReached) {
            toast.show(`🎉 ${res.milestoneReached}일 연속 학습! 잘하고 있어요`, "success");
          }
        })
        .catch((e) => {
          console.error("풀이 제출 실패:", e);
          setSubmitError(
            e instanceof Error ? e.message : "풀이 기록 저장에 실패했습니다. 다음 문제로 넘기기 전에 재시도하세요."
          );
        });
    }
  }

  async function retrySubmit() {
    if (!selectedSubject || !current || !loggedIn) return;
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

    if (solvedCount >= SET_SIZE) {
      setPhase("session-complete");
      return;
    }

    // 비회원 일일 한도 소진 시 다음 문제 fetch 차단 → 즉시 세션 완료 + 로그인 CTA
    if (!loggedIn && quotaExhausted) {
      setPhase("session-complete");
      return;
    }

    let nextQueue = [...queue];
    if (nextQueue.length === 0) {
      setLoading(true);
      try {
        nextQueue = await fetchQuestions(selectedSubject.id);
      } catch {
        // 서버 측 한도 초과(429) 가드에 걸렸을 때
        if (!loggedIn) {
          setQuotaExhausted(true);
          setPhase("session-complete");
        }
        return;
      } finally {
        setLoading(false);
      }
    }

    setPastEntries((prev) => [
      ...prev,
      { question: current, selectedOption, answerText, revealed, detail },
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
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <Spinner message="문제를 불러오는 중..." />
      </main>
    );
  }

  // ── 1. 과목 선택 ─────────────────────────────────────────
  if (phase === "select") {
    const certGroups = new Map<CertKey, { cert: CertToken; roots: Subject[] }>();
    for (const root of subjects) {
      const key = certFromRootName(root.name);
      const cert = CERT_TOKENS[key];
      if (!certGroups.has(key)) {
        certGroups.set(key, { cert, roots: [] });
      }
      certGroups.get(key)!.roots.push(root);
    }

    const certKey = (certParam && certParam in CERT_TOKENS) ? (certParam as CertKey) : null;
    const visibleGroups = certKey
      ? Array.from(certGroups.entries()).filter(([k]) => k === certKey).map(([, v]) => v)
      : Array.from(certGroups.values());

    return (
      <main className="min-h-screen bg-bg text-text">
        <GradingDisclaimerModal />
        <Container size="narrow" className="py-16">
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">과목 선택</h1>
          <p className="mt-2 text-sm text-text-muted">
            과목 하나를 골라 {SET_SIZE}문제 한 세트를 풀어보세요.
          </p>

          <div className="mt-10 space-y-10">
            {visibleGroups.map(({ cert, roots }) => (
              <section key={cert.key}>
                <div className="flex items-center gap-2">
                  <Badge cert={cert.key} variant="soft" size="sm" dot>
                    {cert.label}
                  </Badge>
                  <span className="h-px flex-1 bg-border" />
                </div>

                <div className="mt-4 space-y-5">
                  {roots.map((root) => {
                    const items = root.children.length > 0 ? root.children : [root];
                    const showSubHeading = cert.key === "SQLD" && root.children.length > 0;
                    return (
                      <div key={root.id}>
                        {showSubHeading && (
                          <h3 className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-text-subtle">
                            {root.name}
                          </h3>
                        )}
                        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                          {items.map((child) => (
                            <button
                              key={child.id}
                              onClick={() => handleSelectSubject(child)}
                              className={`group flex items-center gap-3 rounded-lg border border-border bg-surface px-4 py-3 text-left transition-all duration-200 ${cert.tailwind.borderHover} ${cert.tailwind.bgHover}`}
                            >
                              <span className={`h-9 w-1 shrink-0 rounded-full transition-colors ${cert.tailwind.bg} opacity-60 group-hover:opacity-100`} />
                              <span className="flex-1 text-sm font-medium">{child.name}</span>
                              <span className="text-xs text-text-subtle transition-transform group-hover:translate-x-0.5">
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
            <p className="mt-8 text-center text-text-muted">과목 데이터를 불러오는 중...</p>
          )}

          <div className="mt-12 flex items-center justify-center gap-6 text-sm text-text-muted">
            <Link href="/wrong-answers" className="transition-colors hover:text-text">
              오답 노트 →
            </Link>
            <span className="text-border">·</span>
            <Link href="/mock-exams" className="transition-colors hover:text-text">
              모의고사 →
            </Link>
          </div>
        </Container>
      </main>
    );
  }

  // ── 3. 세션 종료 카드 ────────────────────────────────────
  if (phase === "session-complete") {
    const totalSolved = solvedCount > 0 ? solvedCount : SET_SIZE;
    const rate = totalSolved > 0 ? Math.round((correctCount / totalSolved) * 100) : 0;
    const ment = quotaExhausted
      ? "오늘 무료 풀이를 모두 사용했어요. 가입하면 무제한으로 풀고 점수도 자동 저장돼요."
      : rate >= 90
        ? "완벽해요! 같은 과목을 더 풀어볼까요?"
        : rate >= 70
          ? "잘하고 있어요. 한 세트 더 풀면 손에 더 익을 거예요."
          : "괜찮아요. 약한 문제부터 다시 한 번 풀어보세요.";
    const rateColor = rate >= 90 ? "text-success" : rate >= 70 ? "text-warning" : "text-danger";

    return (
      <main className="min-h-screen bg-bg text-text">
        <Container size="narrow" className="py-16">
          <div className="rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/[0.08] via-primary/[0.04] to-transparent p-8 sm:p-10">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-primary">
              {quotaExhausted ? "오늘 무료 풀이 종료" : "세션 완료"}
            </p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">
              {selectedSubject?.name}
            </h1>

            <div className="mt-6 flex items-end gap-4">
              <span className={`text-6xl font-bold tabular-nums sm:text-7xl ${rateColor}`}>
                {correctCount}
                <span className="text-3xl text-text-subtle">/{totalSolved}</span>
              </span>
              <span className={`mb-2 text-2xl font-bold tabular-nums ${rateColor}`}>{rate}%</span>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-text-muted">{ment}</p>

            <div className="mt-8 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <Button
                variant="outline"
                size="lg"
                onClick={replaySameSession}
                disabled={quotaExhausted}
                className="!justify-between text-left"
                rightIcon={
                  <svg className="h-5 w-5 shrink-0 transition-transform group-hover:rotate-180" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
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
                disabled={quotaExhausted}
                className="!justify-between text-left"
                rightIcon={
                  <svg className="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                }
              >
                <div className="min-w-0 flex-1 text-left">
                  <p className="text-sm font-semibold">새 10문제</p>
                  <p className="mt-0.5 text-xs opacity-80">다른 랜덤 문제로 한 세트 더</p>
                </div>
              </Button>
            </div>

            {!loggedIn && authChecked && (
              <div className="mt-6 rounded-xl border border-primary/30 bg-primary/[0.06] p-5">
                <p className="text-xs font-semibold uppercase tracking-wider text-primary">
                  {quotaExhausted ? "가입하면 무제한 풀이" : "로그인 시 가능한 기능"}
                </p>
                <ul className="mt-3 space-y-1.5 text-sm text-text">
                  {quotaExhausted && (
                    <li>· 오늘 한도 없이 무제한 풀이</li>
                  )}
                  <li>· 오답만 모아서 자동 복습</li>
                  <li>· 과목별 정답률·연속 학습 기록 대시보드</li>
                  <li>· 실전 모의고사 응시</li>
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
              </div>
            )}

            <div className="mt-6 md:hidden">
              <AdInfeed
                adSlot="5227022543"
                adLayoutKey="-h4-h+1c-4h+8p"
              />
            </div>
            <div className="mt-6 hidden md:block">
              <AdDisplay adSlot="3622084801" />
            </div>

            <div className="mt-5 flex flex-wrap items-center gap-5 text-sm text-text-muted">
              {loggedIn && (
                <Link
                  href={selectedSubject ? `/wrong-answers?subjectId=${selectedSubject.id}` : "/wrong-answers"}
                  className="transition-colors hover:text-text"
                >
                  약한 문제 복습 →
                </Link>
              )}
              <button
                onClick={() => handleReset()}
                className="transition-colors hover:text-text"
              >
                다른 과목 선택 →
              </button>
            </div>
          </div>
        </Container>
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

  const currentNumber = Math.min(solvedCount + 1, SET_SIZE);
  const progressPct = Math.min((solvedCount / SET_SIZE) * 100, 100);

  return (
    <main className="min-h-screen bg-bg pb-24 text-text">
      <Container size="narrow" className="py-10">
        <div className="flex items-center justify-between gap-3">
          <button
            onClick={() => handleReset()}
            className="text-sm text-text-muted transition-colors hover:text-text"
          >
            ← 과목 선택
          </button>
          <span className="truncate text-sm text-text-muted">{selectedSubject?.name}</span>
          <span
            key={`${correctCount}-${solvedCount}`}
            className="inline-flex origin-center rounded-full border border-border bg-surface px-2.5 py-0.5 text-xs tabular-nums text-text-muted animate-score-pop"
          >
            {correctCount}/{solvedCount} 정답
          </span>
        </div>

        {/* 비회원 일일 풀이 한도 칩 — 자정 리셋 안내 */}
        {!loggedIn && anonQuota && (
          <div className="mt-3 flex items-center justify-center">
            <span
              className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-medium ${
                anonQuota.exhausted
                  ? "border-danger/40 bg-danger/[0.08] text-danger"
                  : anonQuota.remaining <= 3
                    ? "border-warning/40 bg-warning/[0.08] text-warning"
                    : "border-border bg-surface text-text-muted"
              }`}
            >
              무료 풀이 {anonQuota.used}/{anonQuota.limit}
              {anonQuota.exhausted ? (
                <span>· 오늘 한도 도달</span>
              ) : (
                <span className="text-text-subtle">· 자정에 리셋</span>
              )}
            </span>
          </div>
        )}

        {/* 진행 바 */}
        <div className="mt-5">
          <div className="flex items-center justify-between text-[11px] text-text-muted">
            <span className="tabular-nums">
              <span className="font-semibold text-text">{currentNumber}</span>
              <span className="text-text-subtle"> / {SET_SIZE}</span>
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
            <p className="text-xs font-medium text-text-muted">{selectedSubject?.name}</p>
            <div className="flex items-center gap-3">
              <BookmarkButton questionId={current.id} />
              <ReportQuestionButton questionId={current.id} />
              <QuestionTypeBadge type={current.questionType} />
            </div>
          </div>

          <div className="mt-5 text-base leading-relaxed">
            <QuestionContent content={parsed.body} />
          </div>

          {current.questionType === "MCQ" && (
            <ul className="mt-6 space-y-2">
              {parsed.options.map((optionText, idx) => {
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
                      <span className="flex-1">{optionText}</span>
                      {revealed && isCorrectOption && <span className="text-success">✓</span>}
                      {revealed && isSelected && !isCorrectOption && <span className="text-danger">✗</span>}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {current.questionType === "SHORT_ANSWER" && (
            <div className="mt-6">
              <label className="mb-2 block text-xs text-text-muted">정답 입력</label>
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
          <div className="mt-5 flex justify-center">
            <Button
              variant="primary"
              size="lg"
              onClick={handleSubmit}
              disabled={!hasAnswer()}
            >
              정답 제출
            </Button>
          </div>
        )}

        {revealed && detail && (
          <div className="mt-5 space-y-4">
            {submitError && (
              <div className="rounded-lg border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-danger">
                <div className="flex items-center justify-between gap-3">
                  <span>풀이 기록 저장 실패: {submitError}</span>
                  <Button variant="danger" size="sm" onClick={retrySubmit}>
                    재시도
                  </Button>
                </div>
              </div>
            )}

            <div
              className={`flex flex-wrap items-center justify-between gap-3 rounded-lg border px-5 py-4 ${
                isCorrect
                  ? "border-success/40 bg-success/[0.08]"
                  : "border-danger/40 bg-danger/[0.08]"
              }`}
            >
              <div className={`flex items-center gap-2 text-sm font-semibold ${isCorrect ? "text-success" : "text-danger"}`}>
                <span className="text-lg">{isCorrect ? "✓" : "✗"}</span>
                {isCorrect
                  ? "정답입니다!"
                  : detail.questionType === "MCQ"
                    ? `오답 — 정답은 ${detail.correctOption}번입니다.`
                    : `오답 — 모범답안: ${detail.answer ?? "(없음)"}`}
              </div>
              <Button
                variant="primary"
                size="sm"
                onClick={handleNext}
                className="hidden sm:inline-flex"
                rightIcon={
                  <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                }
              >
                {solvedCount >= SET_SIZE ? "결과 보기" : "다음 문제"}
              </Button>
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

      {/* Fixed 하단 다음 버튼 */}
      {revealed && detail && (
        <Button
          variant="primary"
          size="lg"
          className="fixed bottom-6 right-6 z-50 shadow-[var(--shadow-xl)] shadow-primary/30 sm:hidden"
          onClick={handleNext}
          aria-label={solvedCount >= SET_SIZE ? "결과 보기" : "다음 문제"}
          rightIcon={
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
            </svg>
          }
        >
          {solvedCount >= SET_SIZE ? "결과 보기" : "다음 문제"}
        </Button>
      )}
    </main>
  );
}

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
