"use client";

import { useEffect, useState } from "react";
import {
  getWrongAnswers,
  getWrongAnswerStats,
  getSubjects,
  getQuestionDetail,
  type WrongAnswerResponse,
  type WrongAnswerStatsResponse,
  type Subject,
  type QuestionDetail,
} from "@/lib/api";
import { formatDate } from "@/lib/format";
import { parseQuestion } from "@/lib/parseQuestion";
import QuestionContent from "@/components/QuestionContent";
import Spinner from "@/components/Spinner";
import Link from "next/link";

function getLeafSubjects(subjects: Subject[]): { id: number; name: string }[] {
  const leaves: { id: number; name: string }[] = [];
  for (const s of subjects) {
    if (s.children.length > 0) {
      for (const child of s.children) {
        leaves.push({ id: child.id, name: child.name });
      }
    }
  }
  return leaves;
}

function rateColor(rate: number) {
  if (rate > 50) return { bar: "bg-red-500", text: "text-red-400" };
  if (rate > 30) return { bar: "bg-amber-500", text: "text-amber-400" };
  return { bar: "bg-green-500", text: "text-green-400" };
}

export default function WrongAnswersPage() {
  const [stats, setStats] = useState<WrongAnswerStatsResponse[]>([]);
  const [wrongAnswers, setWrongAnswers] = useState<WrongAnswerResponse[]>([]);
  const [subjects, setSubjects] = useState<{ id: number; name: string }[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [explanations, setExplanations] = useState<Record<number, string>>({});

  useEffect(() => {
    Promise.all([
      getWrongAnswerStats(),
      getWrongAnswers(),
      getSubjects(),
    ])
      .then(([statsData, wrongData, subjectsData]) => {
        setStats(statsData);
        setWrongAnswers(wrongData);
        setSubjects(getLeafSubjects(subjectsData));
      })
      .finally(() => setLoading(false));
  }, []);

  function handleSubjectFilter(subjectId: number | null) {
    setSelectedSubject(subjectId);
    setLoading(true);
    getWrongAnswers(subjectId ?? undefined)
      .then(setWrongAnswers)
      .finally(() => setLoading(false));
  }

  function handleExpand(questionId: number) {
    if (expandedId === questionId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(questionId);
    if (!explanations[questionId]) {
      getQuestionDetail(questionId).then((detail) => {
        setExplanations((prev) => ({ ...prev, [questionId]: detail.explanation }));
      });
    }
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">오답 노트</h1>
        <p className="mt-2 text-sm text-muted">취약한 영역을 파악하고 집중 학습하세요.</p>

        {/* Stats */}
        {stats.length > 0 && (
          <section className="mt-8">
            <h2 className="text-lg font-semibold">취약 영역 분석</h2>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
              {stats.map((stat) => {
                const color = rateColor(stat.wrongRate);
                return (
                  <div
                    key={stat.subjectId}
                    className="rounded-xl border border-border bg-surface p-5"
                  >
                    <p className="text-sm font-medium">{stat.subjectName}</p>
                    <div className="mt-3 h-2 rounded-full bg-border">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${color.bar}`}
                        style={{ width: `${stat.wrongRate}%` }}
                      />
                    </div>
                    <p className={`mt-2 text-sm ${color.text}`}>
                      {stat.wrongCount}문제 오답 / {stat.totalSolved}문제 풀이 ({stat.wrongRate}%)
                    </p>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {/* Wrong answer list */}
        <section className="mt-12">
          <h2 className="text-lg font-semibold">오답 문제</h2>

          {/* Subject filter pills */}
          <div className="mt-4 flex flex-wrap gap-2">
            <button
              onClick={() => handleSubjectFilter(null)}
              className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                selectedSubject === null
                  ? "bg-primary text-zinc-900"
                  : "border border-border text-muted hover:border-amber-500/40"
              }`}
            >
              전체
            </button>
            {subjects.map((s) => (
              <button
                key={s.id}
                onClick={() => handleSubjectFilter(s.id)}
                className={`rounded-full px-3 py-1 text-sm font-medium transition-colors ${
                  selectedSubject === s.id
                    ? "bg-primary text-zinc-900"
                    : "border border-border text-muted hover:border-amber-500/40"
                }`}
              >
                {s.name}
              </button>
            ))}
          </div>

          <div className="mt-4 space-y-3">
            {loading && (
              <Spinner />
            )}

            {!loading && wrongAnswers.length === 0 && (
              <div className="py-16 text-center">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10 border border-green-500/20">
                  <svg className="h-8 w-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" />
                  </svg>
                </div>
                <p className="mt-4 text-muted">오답이 없습니다. 완벽해요!</p>
                <Link
                  href="/solve"
                  className="mt-6 inline-block rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-zinc-900 transition-colors hover:bg-primary-hover"
                >
                  문제 풀러 가기
                </Link>
              </div>
            )}

            {!loading &&
              wrongAnswers.map((wa) => (
                <div
                  key={wa.questionId}
                  className="rounded-lg border border-border bg-surface px-5 py-4"
                >
                  <div
                    className="cursor-pointer"
                    onClick={() => handleExpand(wa.questionId)}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <p className="flex-1 text-sm leading-relaxed line-clamp-3 whitespace-pre-line">
                        {wa.questionContent}
                      </p>
                      <svg
                        className={`h-4 w-4 shrink-0 text-muted transition-transform ${
                          expandedId === wa.questionId ? "rotate-180" : ""
                        }`}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth={2}
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                    <div className="mt-2 flex items-center gap-2">
                      <span className="rounded bg-violet-500/10 px-2 py-0.5 text-xs font-medium text-violet-400">
                        {wa.subjectName}
                      </span>
                      <span className="text-xs text-red-400">
                        {wa.wrongCount}회 오답
                      </span>
                      <span className="text-xs text-muted">
                        {formatDate(wa.lastWrongAt)}
                      </span>
                    </div>
                  </div>

                  <div className={`grid transition-all duration-300 ease-in-out ${expandedId === wa.questionId ? "grid-rows-[1fr] opacity-100 mt-3" : "grid-rows-[0fr] opacity-0"}`}>
                    <div className="overflow-hidden">
                      <div className="space-y-3">
                        <div className="rounded-lg border border-border px-3 py-3">
                          <QuestionContent segments={parseQuestion(wa.questionContent).segments} />
                        </div>
                        <div className="rounded-lg border border-border px-3 py-3 text-sm">
                          <p className="font-medium text-amber-400">해설</p>
                          <p className="mt-1 leading-relaxed text-muted">
                            {explanations[wa.questionId] || "로딩 중..."}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
          </div>
        </section>
      </div>
    </main>
  );
}
