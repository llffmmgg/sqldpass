"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getDailyQuestion, type PublicQuestionDetail, type CertSlug } from "@/lib/publicApi";
import { getMyStreak, getLastSolvedCert, type Streak } from "@/lib/streakApi";
import { isLoggedIn } from "@/lib/auth";

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
  const [activeCert, setActiveCert] = useState<CertSlug>("sqld");
  const [question, setQuestion] = useState<PublicQuestionDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [streak, setStreak] = useState<Streak | null>(null);
  const [logged, setLogged] = useState(false);

  // 로그인 상태 + 마지막 풀이 자격증 → 기본 탭 결정
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

  // 탭 변경 시 해당 cert 의 오늘의 문제 로드
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setQuestion(null);
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
            {streak.solvedToday && <span className="text-[10px] text-primary/70">오늘 완료</span>}
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
            <p className="whitespace-pre-wrap break-words text-sm leading-relaxed sm:text-base line-clamp-6">
              {question.content}
            </p>
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <div className="text-xs text-muted">
                {question.certName} · {question.categoryName}
              </div>
              <Link
                href={`/q/${question.id}`}
                className="inline-flex items-center gap-1 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-zinc-900 hover:bg-primary-hover"
              >
                풀어보기 →
              </Link>
            </div>
          </>
        )}
      </div>

      {!logged && (
        <p className="mt-3 text-xs text-muted">
          로그인하면 하루 1문제씩 풀고 <span className="text-foreground font-medium">연속 학습 기록</span>이 쌓여요.
        </p>
      )}
    </section>
  );
}
