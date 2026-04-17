"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  getWrongAnswers,
  getWrongAnswerStats,
  getSubjects,
  getQuestionDetail,
  retryWrongAnswer,
  type WrongAnswerResponse,
  type WrongAnswerStatsResponse,
  type Subject,
  type QuestionDetail,
  type WrongAnswerRetryResponse,
} from "@/lib/api";
import { formatRelativeDate } from "@/lib/format";
import { parseQuestion } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import AuthGuard from "@/components/AuthGuard";
import Spinner from "@/components/Spinner";
import Image from "next/image";
import { trackEvent } from "@/lib/gtag";
import { Button, ButtonLink, Container } from "@/components/ui";
import { CERT_TOKENS, certFromRootName, type CertKey } from "@/lib/cert-tokens";

function buildCertLookupById(rawSubjects: Subject[]): Map<number, CertKey> {
  const map = new Map<number, CertKey>();
  for (const root of rawSubjects) {
    const cert = certFromRootName(root.name);
    map.set(root.id, cert);
    for (const child of root.children) {
      map.set(child.id, cert);
    }
  }
  return map;
}

interface RetryState {
  selectedOption?: number;
  answerText?: string;
  result?: WrongAnswerRetryResponse;
  submitting?: boolean;
  error?: string;
}

type SortMode = "priority" | "recent";

export default function WrongAnswersPage() {
  return (
    <AuthGuard>
      <Suspense fallback={<main className="min-h-screen bg-background flex items-center justify-center"><Spinner /></main>}>
        <WrongAnswersPageContent />
      </Suspense>
    </AuthGuard>
  );
}

function WrongAnswersPageContent() {
  const searchParams = useSearchParams();

  useEffect(() => {
    trackEvent("review_wrong");
  }, []);

  const [stats, setStats] = useState<WrongAnswerStatsResponse[]>([]);
  const [wrongAnswers, setWrongAnswers] = useState<WrongAnswerResponse[]>([]);
  const [rawSubjects, setRawSubjects] = useState<Subject[]>([]);
  const initialSubject = (() => {
    const v = searchParams?.get("subjectId");
    const n = v ? Number(v) : NaN;
    return Number.isFinite(n) && n > 0 ? n : null;
  })();
  const [selectedCert, setSelectedCert] = useState<CertKey | null>(null);
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<SortMode>("priority");
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [details, setDetails] = useState<Record<number, QuestionDetail>>({});
  const [retryState, setRetryState] = useState<Record<number, RetryState>>({});
  const [masteredIds, setMasteredIds] = useState<Set<number>>(new Set());

  function reloadList() {
    return Promise.all([
      getWrongAnswers(initialSubject ?? undefined),
      getWrongAnswerStats(),
    ]).then(([wrongData, statsData]) => {
      setWrongAnswers(wrongData);
      setStats(statsData);
    });
  }

  useEffect(() => {
    Promise.all([
      getWrongAnswerStats(),
      getWrongAnswers(initialSubject ?? undefined),
      getSubjects(),
    ])
      .then(([statsData, wrongData, subjectsData]) => {
        setStats(statsData);
        setWrongAnswers(wrongData);
        setRawSubjects(subjectsData);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleExpand(questionId: number) {
    if (expandedId === questionId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(questionId);
    if (!details[questionId]) {
      getQuestionDetail(questionId)
        .then((detail) => {
          setDetails((prev) => ({ ...prev, [questionId]: detail }));
        })
        .catch(() => {
          setRetryState((prev) => ({
            ...prev,
            [questionId]: { ...prev[questionId], error: "문제를 불러오지 못했습니다." },
          }));
        });
    }
  }

  function setRetryField(questionId: number, patch: Partial<RetryState>) {
    setRetryState((prev) => ({
      ...prev,
      [questionId]: { ...prev[questionId], ...patch },
    }));
  }

  async function handleSubmitRetry(questionId: number) {
    const state = retryState[questionId] ?? {};
    const detail = details[questionId];
    if (!detail) return;

    if (detail.questionType === "MCQ") {
      if (!state.selectedOption) {
        setRetryField(questionId, { error: "답을 선택해주세요." });
        return;
      }
    } else {
      if (!state.answerText || state.answerText.trim().length === 0) {
        setRetryField(questionId, { error: "답을 입력해주세요." });
        return;
      }
    }

    setRetryField(questionId, { submitting: true, error: undefined });
    try {
      const res = await retryWrongAnswer(questionId, {
        selectedOption: state.selectedOption,
        answerText: state.answerText,
      });
      setRetryField(questionId, { result: res, submitting: false });

      if (res.correct) {
        setMasteredIds((prev) => new Set(prev).add(questionId));
        setTimeout(() => {
          reloadList().then(() => {
            setMasteredIds((prev) => {
              const next = new Set(prev);
              next.delete(questionId);
              return next;
            });
            setExpandedId(null);
            setRetryState((prev) => {
              const next = { ...prev };
              delete next[questionId];
              return next;
            });
          });
        }, 1500);
      }
    } catch (e) {
      setRetryField(questionId, {
        submitting: false,
        error: e instanceof Error ? e.message : "제출에 실패했습니다.",
      });
    }
  }

  function handleRetryAgain(questionId: number) {
    setRetryState((prev) => ({
      ...prev,
      [questionId]: { submitting: false },
    }));
  }

  // 정렬 적용
  const sortedAnswers = useMemo(() => {
    const arr = [...wrongAnswers];
    if (sortMode === "priority") {
      arr.sort((a, b) => {
        if (b.wrongCount !== a.wrongCount) return b.wrongCount - a.wrongCount;
        return new Date(b.lastWrongAt).getTime() - new Date(a.lastWrongAt).getTime();
      });
    } else {
      arr.sort(
        (a, b) => new Date(b.lastWrongAt).getTime() - new Date(a.lastWrongAt).getTime()
      );
    }
    return arr;
  }, [wrongAnswers, sortMode]);

  // 자격증 → 토픽(leaf) → 문제 그룹화. 정렬은 sortedAnswers의 순서를 보존.
  const certLookupById = useMemo(() => buildCertLookupById(rawSubjects), [rawSubjects]);
  const groupedAnswers = useMemo(() => {
    type Topic = { topicName: string; items: WrongAnswerResponse[] };
    type CertGroup = { certKey: CertKey; topics: Map<string, Topic> };
    const certs = new Map<CertKey, CertGroup>();
    for (const wa of sortedAnswers) {
      const cert = certLookupById.get(wa.subjectId) ?? "SQLD";
      if (!certs.has(cert)) certs.set(cert, { certKey: cert, topics: new Map() });
      const group = certs.get(cert)!;
      if (!group.topics.has(wa.subjectName)) {
        group.topics.set(wa.subjectName, { topicName: wa.subjectName, items: [] });
      }
      group.topics.get(wa.subjectName)!.items.push(wa);
    }
    return Array.from(certs.values())
      .sort((a, b) => CERT_TOKENS[a.certKey].order - CERT_TOKENS[b.certKey].order)
      .map((g) => ({
        certKey: g.certKey,
        topics: Array.from(g.topics.values()).sort((a, b) => b.items.length - a.items.length),
        total: Array.from(g.topics.values()).reduce((sum, t) => sum + t.items.length, 0),
      }));
  }, [sortedAnswers, certLookupById]);

  // 상단 한 줄 요약용
  const totalUnresolved = wrongAnswers.length;
  const topWeak = useMemo(
    () => [...stats].filter((s) => s.wrongCount > 0).sort((a, b) => b.wrongRate - a.wrongRate)[0] ?? null,
    [stats]
  );

  function getFirstLine(text: string): string {
    // 코드블록/마크다운 제거 후 첫 줄
    const cleaned = text
      .replace(/```[\s\S]*?```/g, "")
      .replace(/\*\*/g, "")
      .replace(/\n+/g, " ")
      .trim();
    return cleaned.length > 80 ? cleaned.slice(0, 80) + "…" : cleaned;
  }

  // 카드 렌더러 — 그룹화된 컬렉션에서 재사용
  function renderWrongAnswerCard(wa: WrongAnswerResponse, num?: number) {
    const isExpanded = expandedId === wa.questionId;
    const isMastered = masteredIds.has(wa.questionId);
    const detail = details[wa.questionId];
    const state = retryState[wa.questionId] ?? {};
    const parsed = detail ? parseQuestion(detail.content) : null;

    const priority: "high" | "mid" | "low" =
      wa.wrongCount >= 3 ? "high" : wa.wrongCount === 2 ? "mid" : "low";

    const borderClass = isMastered
      ? "border-green-500/60 bg-green-500/10 opacity-60 scale-95 border"
      : priority === "high"
      ? "border border-border border-l-4 border-l-red-500/70"
      : priority === "mid"
      ? "border border-border border-l-4 border-l-amber-500/70"
      : "border border-border";

    const priorityBg = priority === "high" ? "bg-red-500" : priority === "mid" ? "bg-amber-500" : "bg-zinc-500";

    return (
      <div
        key={wa.questionId}
        className={`rounded-lg bg-surface transition-all duration-500 ${borderClass}`}
      >
        <div className="cursor-pointer flex items-center gap-3 px-4 py-3" onClick={() => handleExpand(wa.questionId)}>
          {/* 순번 + 우선순위 뱃지 */}
          <div className="flex flex-col items-center gap-1 shrink-0 w-8">
            {num && <span className="text-[10px] text-muted/50 tabular-nums">#{num}</span>}
            <span className={`flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-bold text-white ${priorityBg}`}>
              {wa.wrongCount}
            </span>
          </div>

          {/* 문제 요약 */}
          <div className="flex-1 min-w-0">
            <p className="text-sm leading-snug text-foreground truncate">
              {getFirstLine(wa.questionContent)}
            </p>
            <div className="mt-1 flex items-center gap-2 text-[11px] text-muted">
              <span>{wa.wrongCount}회 틀림</span>
              <span className="text-muted/30">·</span>
              <span>{formatRelativeDate(wa.lastWrongAt)}</span>
            </div>
          </div>

          {/* 화살표 */}
          <svg
            className={`h-4 w-4 shrink-0 text-muted/50 transition-transform ${isExpanded ? "rotate-180" : ""}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </div>

        <div
          className={`grid transition-all duration-300 ease-in-out ${
            isExpanded ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
          }`}
        >
          <div className="overflow-hidden px-4 pb-4">
            {!detail ? (
              <div className="py-4 text-center text-sm text-muted">로딩 중...</div>
            ) : (
              <div className="space-y-3">
                <div className="rounded-lg border border-border px-3 py-3">
                  <QuestionContent content={parsed?.body ?? ""} />
                </div>

                {isMastered && (
                  <div className="rounded-lg border border-green-500/40 bg-green-500/10 px-4 py-3 text-center">
                    <p className="text-base font-semibold text-green-300">🎉 정답! 마스터 완료</p>
                    <p className="mt-1 text-xs text-green-400/80">잠시 후 목록에서 사라집니다</p>
                  </div>
                )}

                {!isMastered && !state.result?.correct && (
                  <RetrySolverInline
                    detail={detail}
                    parsedOptions={parsed?.options ?? []}
                    state={state}
                    onChange={(patch) => setRetryField(wa.questionId, patch)}
                    onSubmit={() => handleSubmitRetry(wa.questionId)}
                  />
                )}

                {state.result && !state.result.correct && (
                  <div className="space-y-3">
                    <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3">
                      <p className="text-sm font-semibold text-red-300">❌ 다시 도전!</p>
                      {detail.questionType === "MCQ" ? (
                        <p className="mt-1 text-sm text-red-200/90">
                          정답: <span className="font-bold">{state.result.correctOption}번</span>
                        </p>
                      ) : (
                        <p className="mt-1 text-sm text-red-200/90">
                          모범답안:{" "}
                          <span className="font-mono">{state.result.correctAnswer}</span>
                        </p>
                      )}
                    </div>
                    <div className="rounded-lg border border-border px-3 py-3 text-sm">
                      <p className="font-medium text-amber-400">해설</p>
                      <div className="mt-1 leading-relaxed text-muted">
                        <QuestionContent content={state.result.explanation ?? ""} />
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      size="md"
                      onClick={() => handleRetryAgain(wa.questionId)}
                      className="w-full"
                    >
                      🔁 다시 풀어보기
                    </Button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="default" className="py-16">
        <div className="flex items-center gap-5">
          <Image
            src="/wrong-answer-mascot.webp"
            alt="오답노트 마스코트"
            width={220}
            height={220}
            className="shrink-0"
            priority
          />
          <div>
            <h1 className="text-3xl font-bold sm:text-4xl">오답 노트</h1>
            <p className="mt-2 text-base text-muted">
              틀린 문제를 다시 풀어 마스터하세요. 다시 맞히면 목록에서 자동으로 사라집니다.
            </p>
          </div>
        </div>

        {/* 요약 카드 */}
        {totalUnresolved > 0 && (() => {
          const highCount = wrongAnswers.filter((w) => w.wrongCount >= 3).length;
          const midCount = wrongAnswers.filter((w) => w.wrongCount === 2).length;
          const lowCount = wrongAnswers.filter((w) => w.wrongCount === 1).length;
          return (
            <div className="mt-4 flex flex-wrap items-center gap-3 rounded-xl border border-border bg-surface px-5 py-3">
              <div className="flex items-center gap-2">
                <span className="text-2xl font-bold text-foreground">{totalUnresolved}</span>
                <span className="text-sm text-muted">문제 남음</span>
              </div>
              <div className="ml-auto flex items-center gap-4 text-xs">
                {highCount > 0 && (
                  <span className="flex items-center gap-1.5">
                    <span className="h-2.5 w-2.5 rounded-full bg-red-500" />
                    <span className="text-red-400 font-semibold">고위험 {highCount}</span>
                  </span>
                )}
                {midCount > 0 && (
                  <span className="flex items-center gap-1.5">
                    <span className="h-2.5 w-2.5 rounded-full bg-amber-500" />
                    <span className="text-amber-400 font-semibold">주의 {midCount}</span>
                  </span>
                )}
                {lowCount > 0 && (
                  <span className="flex items-center gap-1.5">
                    <span className="h-2.5 w-2.5 rounded-full bg-zinc-500" />
                    <span className="text-muted">1회 {lowCount}</span>
                  </span>
                )}
              </div>
              {topWeak && (
                <div className="w-full border-t border-border pt-2 mt-1 text-xs text-muted">
                  가장 취약: <span className="font-semibold text-red-300">{topWeak.subjectName}</span> (오답률 {topWeak.wrongRate}%)
                </div>
              )}
            </div>
          );
        })()}

        {/* Wrong answer list */}
        <section className="mt-8">
          {loading && <Spinner />}

          {!loading && sortedAnswers.length === 0 && (
            <div className="py-16 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 border border-green-500/20">
                <svg className="h-8 w-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                </svg>
              </div>
              <p className="mt-4 text-muted">오답이 없습니다. 완벽해요!</p>
              <ButtonLink href="/solve" variant="primary" size="md" className="mt-6">
                문제 풀러 가기
              </ButtonLink>
            </div>
          )}

          {!loading && sortedAnswers.length > 0 && (() => {
            // 자격증이 1개면 자동 선택
            const activeCert = selectedCert ?? (groupedAnswers.length === 1 ? groupedAnswers[0].certKey : null);
            const activeGroup = activeCert ? groupedAnswers.find((g) => g.certKey === activeCert) : null;

            // 활성 그룹의 토픽 필터링
            const visibleTopics = activeGroup
              ? (selectedTopic
                  ? activeGroup.topics.filter((t) => t.topicName === selectedTopic)
                  : activeGroup.topics)
              : [];

            return (
            <div className="space-y-4">
              {/* 1단계: 자격증 탭 — underline style */}
              <div className="flex flex-wrap items-center gap-2 border-b border-border">
                {groupedAnswers.map((group) => {
                  const isActive = activeCert === group.certKey;
                  const meta = CERT_TOKENS[group.certKey];
                  return (
                    <button
                      key={group.certKey}
                      onClick={() => {
                        setSelectedCert(group.certKey);
                        setSelectedTopic(null);
                        setExpandedId(null);
                      }}
                      className={`-mb-px flex items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition-all ${
                        isActive
                          ? "border-primary text-primary"
                          : "border-transparent text-text-muted hover:text-text"
                      }`}
                    >
                      <span className={`h-1.5 w-1.5 rounded-full ${meta.tailwind.dot}`} />
                      {meta.label}
                      <span className="text-xs tabular-nums opacity-70">{group.total}</span>
                    </button>
                  );
                })}

                {/* 정렬 */}
                <div className="ml-auto flex items-center gap-1 text-xs">
                  <button
                    onClick={() => setSortMode("priority")}
                    className={`rounded px-2 py-1 transition-colors ${
                      sortMode === "priority" ? "text-foreground font-semibold" : "text-muted hover:text-foreground"
                    }`}
                  >
                    오답 많은 순
                  </button>
                  <span className="text-muted/40">·</span>
                  <button
                    onClick={() => setSortMode("recent")}
                    className={`rounded px-2 py-1 transition-colors ${
                      sortMode === "recent" ? "text-foreground font-semibold" : "text-muted hover:text-foreground"
                    }`}
                  >
                    최근순
                  </button>
                </div>
              </div>

              {/* 자격증 미선택 시 안내 */}
              {!activeCert && (
                <div className="py-12 text-center text-muted">
                  위에서 자격증을 선택해주세요.
                </div>
              )}

              {/* 2단계: 토픽 필터 + 문제 목록 */}
              {activeGroup && (
                <div className="rounded-xl border border-border overflow-hidden">
                  {/* 토픽 필터 */}
                  {activeGroup.topics.length > 1 && (
                    <div className="flex flex-wrap items-center gap-1.5 px-5 py-3 bg-surface border-b border-border">
                      <button
                        onClick={() => { setSelectedTopic(null); setExpandedId(null); }}
                        className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                          selectedTopic === null
                            ? "bg-primary text-[var(--primary-fg)]"
                            : "border border-border text-text-muted hover:border-primary/40 hover:text-text"
                        }`}
                      >
                        전체 {activeGroup.total}
                      </button>
                      {activeGroup.topics.map((topic) => {
                        const isActive = selectedTopic === topic.topicName;
                        return (
                          <button
                            key={topic.topicName}
                            onClick={() => { setSelectedTopic(isActive ? null : topic.topicName); setExpandedId(null); }}
                            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                              isActive
                                ? "bg-primary text-[var(--primary-fg)]"
                                : "border border-border text-text-muted hover:border-primary/40 hover:text-text"
                            }`}
                          >
                            {topic.topicName} {topic.items.length}
                          </button>
                        );
                      })}
                    </div>
                  )}

                  {/* 문제 목록 */}
                  <div className="divide-y divide-border">
                    {visibleTopics.map((topic) => {
                      const tHigh = topic.items.filter((w) => w.wrongCount >= 3).length;
                      const tMid = topic.items.filter((w) => w.wrongCount === 2).length;
                      return (
                      <div key={topic.topicName} className="px-5 py-4">
                        <h3 className="mb-3 flex items-center gap-2 text-xs font-semibold tracking-wide text-muted">
                          <span>{topic.topicName}</span>
                          <span className="rounded-full bg-border px-1.5 py-0.5 text-[10px] tabular-nums text-muted/80">
                            {topic.items.length}
                          </span>
                          {tHigh > 0 && <span className="flex items-center gap-1 text-[10px] text-red-400"><span className="h-1.5 w-1.5 rounded-full bg-red-500" />{tHigh}</span>}
                          {tMid > 0 && <span className="flex items-center gap-1 text-[10px] text-amber-400"><span className="h-1.5 w-1.5 rounded-full bg-amber-500" />{tMid}</span>}
                        </h3>
                        <div className="space-y-2">
                          {topic.items.map((wa, idx) => renderWrongAnswerCard(wa, idx + 1))}
                        </div>
                      </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
            );
          })()}
        </section>
      </Container>
    </main>
  );
}

// ----------------------------------------------------------
// 인라인 풀이 컴포넌트
// ----------------------------------------------------------
function RetrySolverInline({
  detail,
  parsedOptions,
  state,
  onChange,
  onSubmit,
}: {
  detail: QuestionDetail;
  parsedOptions: string[];
  state: RetryState;
  onChange: (patch: Partial<RetryState>) => void;
  onSubmit: () => void;
}) {
  const isMcq = detail.questionType === "MCQ";

  return (
    <div className="space-y-3">
      {isMcq ? (
        <div className="space-y-2">
          {parsedOptions.map((opt, idx) => {
            const optionNumber = idx + 1;
            const isSelected = state.selectedOption === optionNumber;
            return (
              <button
                key={optionNumber}
                type="button"
                onClick={() => onChange({ selectedOption: optionNumber })}
                disabled={state.submitting}
                className={`flex w-full items-start gap-3 rounded-lg border px-4 py-3 text-left text-sm transition ${
                  isSelected
                    ? "border-primary/60 bg-primary/10 text-text ring-1 ring-primary/40"
                    : "border-border bg-bg text-text hover:border-primary/30"
                } disabled:cursor-not-allowed disabled:opacity-50`}
              >
                <span
                  className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                    isSelected ? "bg-primary/30 text-primary" : "bg-border text-text-muted"
                  }`}
                >
                  {optionNumber}
                </span>
                <span className="flex-1 leading-relaxed">{opt}</span>
              </button>
            );
          })}
        </div>
      ) : (
        <textarea
          value={state.answerText ?? ""}
          onChange={(e) => onChange({ answerText: e.target.value })}
          disabled={state.submitting}
          rows={3}
          placeholder="답을 입력하세요"
          className="block w-full resize-none rounded-lg border border-border bg-bg px-4 py-3 text-sm leading-relaxed text-text placeholder:text-text-subtle focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/60 disabled:opacity-50"
        />
      )}

      {state.error && <p className="text-xs text-danger">{state.error}</p>}

      <Button
        variant="primary"
        size="md"
        onClick={onSubmit}
        loading={state.submitting}
        className="w-full"
      >
        {state.submitting ? "채점 중..." : "정답 제출"}
      </Button>
    </div>
  );
}
