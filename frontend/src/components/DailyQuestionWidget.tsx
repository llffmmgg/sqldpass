"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getDailyQuestion, type PublicQuestionDetail, type CertSlug } from "@/lib/publicApi";
import { getMyStreak, getLastSolvedCert, type Streak } from "@/lib/streakApi";
import { submitSolve, type SolveResponse } from "@/lib/api";
import { isLoggedIn } from "@/lib/auth";
import { useToast } from "@/components/Toast";
import QuestionContent from "@/components/QuestionContent";
import { parseQuestion } from "@/lib/parseQuestion";

type Tab = { slug: CertSlug; label: string };

const TABS: Tab[] = [
  { slug: "sqld", label: "SQLD" },
  { slug: "engineer-written", label: "정처기 필기" },
  { slug: "engineer", label: "정처기 실기" },
  { slug: "computer-literacy-1", label: "컴활 1급" },
  { slug: "computer-literacy-2", label: "컴활 2급" },
  { slug: "adsp", label: "ADsP" },
];

export default function DailyQuestionWidget() {
  const toast = useToast();
  const [activeCert, setActiveCert] = useState<CertSlug>("sqld");
  const [question, setQuestion] = useState<PublicQuestionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [streak, setStreak] = useState<Streak | null>(null);
  const [logged, setLogged] = useState(false);

  // 풀이 상태
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [answerText, setAnswerText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<SolveResponse | null>(null);

  useEffect(() => {
    const loggedIn = isLoggedIn();
    setLogged(loggedIn);
    if (loggedIn) {
      getMyStreak().then(setStreak).catch(() => {});
      getLastSolvedCert()
        .then((slug) => {
          if (slug && TABS.some((t) => t.slug === slug)) {
            setActiveCert(slug as CertSlug);
          }
        })
        .catch(() => {});
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setQuestion(null);
    setSelectedOption(null);
    setAnswerText("");
    setResult(null);
    getDailyQuestion(activeCert)
      .then((q) => {
        if (!cancelled) setQuestion(q);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeCert]);

  const isMcq = question?.questionType === "MCQ";
  const parsed = question ? parseQuestion(question.content) : null;
  const canSubmit =
    !!question &&
    !submitting &&
    !result &&
    (isMcq ? selectedOption !== null : answerText.trim().length > 0);

  async function handleSubmit() {
    if (!question || !logged) return;
    setSubmitting(true);
    try {
      const res = await submitSolve({
        subjectId: question.categoryId,
        answers: [
          {
            questionId: question.id,
            selectedOption: isMcq ? (selectedOption ?? undefined) : undefined,
            answerText: !isMcq ? answerText : undefined,
          },
        ],
      });
      setResult(res);
      if (res.milestoneReached) {
        toast.show(`🎉 ${res.milestoneReached}일 연속 학습! 잘하고 있어요`, "success");
      } else if (res.currentStreak != null && res.currentStreak > 0) {
        toast.show(`🔥 ${res.currentStreak}일 연속!`, "success");
      }
      getMyStreak().then(setStreak).catch(() => {});
    } catch (e) {
      toast.show(e instanceof Error ? e.message : "제출 실패", "error");
    } finally {
      setSubmitting(false);
    }
  }

  const correct = result?.answers?.[0]?.correct ?? false;

  return (
    <section className="rounded-2xl border border-border bg-surface/60 p-5 sm:p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="text-lg">📆</span>
          <h2 className="text-base font-semibold tracking-tight sm:text-lg">오늘의 문제</h2>
        </div>
        {logged && streak && (
          <div className="flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs text-primary">
            <span>🔥</span>
            <span className="font-semibold">{streak.currentStreak}일 연속</span>
            {streak.solvedToday && <span className="text-[11px] font-medium text-primary">· 오늘 완료</span>}
          </div>
        )}
      </div>

      <div className="mt-4 flex flex-wrap gap-1.5">
        {TABS.map((t) => (
          <button
            key={t.slug}
            type="button"
            onClick={() => setActiveCert(t.slug)}
            className={`rounded-full border px-3 py-1 text-xs font-medium transition ${
              activeCert === t.slug
                ? "border-primary bg-primary/15 text-primary"
                : "border-border text-muted hover:border-foreground/30 hover:text-foreground"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="mt-4 rounded-lg border border-border bg-background p-4 sm:p-5">
        {loading ? (
          <div className="h-24 animate-pulse rounded bg-muted/10" />
        ) : !question ? (
          <p className="text-sm text-muted">문제를 불러오지 못했어요.</p>
        ) : (
          <>
            <div className="text-sm leading-relaxed sm:text-base">
              <QuestionContent content={parsed?.body ?? question.content} />
            </div>

            {isMcq && parsed && parsed.options.length > 0 && (
              <ul className="mt-4 space-y-2">
                {parsed.options.map((optionText, idx) => {
                  const num = idx + 1;
                  const isSelected = selectedOption === num;
                  const isCorrectOption = question.correctOption === num;
                  const revealed = !!result;

                  let style =
                    "border-border hover:border-primary/40 hover:bg-primary/5";
                  if (revealed) {
                    if (isCorrectOption) {
                      style = "border-success/60 bg-success/10 text-success";
                    } else if (isSelected && !isCorrectOption) {
                      style = "border-danger/60 bg-danger/10 text-danger";
                    } else {
                      style = "border-border opacity-50";
                    }
                  } else if (isSelected) {
                    style = "border-primary bg-primary/10 text-primary";
                  }

                  return (
                    <li key={num}>
                      <button
                        type="button"
                        disabled={!logged || revealed}
                        onClick={() => setSelectedOption(num)}
                        className={`flex w-full items-start gap-3 rounded-lg border px-3 py-2.5 text-left text-sm transition ${style} disabled:cursor-default`}
                      >
                        <span className="mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-current text-xs font-semibold">
                          {num}
                        </span>
                        <div className="min-w-0 flex-1">
                          <QuestionContent content={optionText} className="mcq-option" />
                        </div>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}

            <div className="mt-3 text-xs text-muted">
              {question.certName} · {question.categoryName}
            </div>

            {/* 비로그인: 링크로 이동 유도 */}
            {!logged && (
              <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                <p className="text-xs text-muted">
                  로그인하면 <span className="text-foreground font-medium">여기서 바로 풀고 연속 학습 기록</span>이 쌓여요.
                </p>
                <Link
                  href={`/q/${question.id}`}
                  className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-fg hover:bg-primary-hover"
                >
                  문제 풀러 가기 →
                </Link>
              </div>
            )}

            {/* 로그인 & 미제출: 풀이 UI */}
            {logged && !result && (
              <div className="mt-5">
                {!isMcq && (
                  <textarea
                    value={answerText}
                    onChange={(e) => setAnswerText(e.target.value)}
                    rows={3}
                    placeholder="답안을 입력하세요"
                    className="w-full resize-y rounded-lg border border-border bg-background px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                )}
                <div className="mt-4 flex items-center justify-between">
                  <p className="text-xs text-muted">
                    {isMcq ? "정답 번호를 고르세요" : "핵심 키워드를 포함해 간단히"}
                  </p>
                  <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={!canSubmit}
                    className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-fg hover:bg-primary-hover disabled:opacity-40"
                  >
                    {submitting ? "제출 중…" : "제출하기"}
                  </button>
                </div>
              </div>
            )}

            {/* 제출 후 결과 공개 */}
            {logged && result && (
              <div className="mt-5 space-y-3">
                <div
                  className={`rounded-lg border px-4 py-3 text-sm font-semibold ${
                    correct
                      ? "border-success/40 bg-success/10 text-success"
                      : "border-danger/40 bg-danger/10 text-danger"
                  }`}
                >
                  {correct ? "✅ 정답입니다!" : "❌ 오답이에요"}
                  {result.currentStreak != null && (
                    <span className="ml-2 text-xs font-medium text-foreground/80">🔥 {result.currentStreak}일 연속</span>
                  )}
                </div>

                <div className="rounded-lg border border-border bg-muted/5 p-4 text-sm">
                  {isMcq && question.correctOption != null && (
                    <p className="mb-2">
                      <span className="text-muted">정답:</span>{" "}
                      <span className="font-semibold">{question.correctOption}번</span>
                    </p>
                  )}
                  {!isMcq && question.answer && (
                    <div className="mb-2">
                      <span className="text-muted">정답:</span>
                      <QuestionContent content={question.answer} />
                    </div>
                  )}
                  {question.explanation && (
                    <QuestionContent content={question.explanation} />
                  )}
                </div>

                <div className="flex items-center justify-between text-xs">
                  <Link href={`/q/${question.id}`} className="text-primary hover:underline">
                    문제 상세로 이동 →
                  </Link>
                  <span className="text-muted">내일 같은 자리에 새 문제가 올라와요</span>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}
