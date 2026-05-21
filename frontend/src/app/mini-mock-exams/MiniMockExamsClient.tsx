"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";

import Spinner from "@/components/Spinner";
import { Card, Container } from "@/components/ui";
import QuotaBadge from "@/components/QuotaBadge";
import { CERT_LIST, CERT_TOKENS, type CertKey } from "@/lib/cert-tokens";
import { isLoggedIn } from "@/lib/auth";
import {
  getMiniMockExams,
  type MockExamSummary,
} from "@/lib/mockExamApi";
import { isExamNew } from "@/lib/mockExamNew";
import { getPublicMiniMockExams } from "@/lib/publicApi";
import {
  DIFFICULTY_OPTIONS,
  MockExamCard,
  type DifficultyFilter,
} from "../mock-exams/MockExamsClient";

export default function MiniMockExamsClient() {
  return (
    <Suspense fallback={null}>
      <MiniMockExamsContent />
    </Suspense>
  );
}

function MiniMockExamsContent() {
  const searchParams = useSearchParams();
  const certParam = searchParams?.get("cert");
  const activeCert: CertKey =
    (certParam && certParam in CERT_TOKENS ? (certParam as CertKey) : null) ?? "SQLD";

  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("ALL");

  useEffect(() => {
    let alive = true;
    const fetcher = isLoggedIn() ? getMiniMockExams() : getPublicMiniMockExams();
    fetcher
      .then((next) => {
        if (alive) setExams(next);
      })
      .catch((e) => {
        if (!alive) return;
        setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다.");
      });
    return () => {
      alive = false;
    };
  }, []);

  const token = CERT_TOKENS[activeCert];

  // 정렬 정책은 정규(MockExamsClient)와 동일하게 premiumRank → sequence DESC.
  // 미니는 모두 PREMIUM 이라 premiumRank 가 무차별이지만 정책 일관성을 위해 동일 함수.
  const filtered = useMemo(() => {
    if (!exams) return null;
    const premiumRank = (e: MockExamSummary) =>
      (e.isPremium ?? e.visibility === "PREMIUM") ? 0 : 1;
    return exams
      .filter((e) => e.kind === "MINI" && e.examType === activeCert)
      .filter((e) => difficulty === "ALL" || e.difficultyLabel === difficulty)
      .slice()
      .sort((a, b) => {
        const r = premiumRank(a) - premiumRank(b);
        if (r !== 0) return r;
        return b.sequence - a.sequence;
      });
  }, [exams, activeCert, difficulty]);

  // 자격증 칩별 카운트 — MINI 만.
  const countsByCert = useMemo(() => {
    const map: Record<string, number> = {};
    (exams ?? []).forEach((e) => {
      if (e.kind !== "MINI") return;
      map[e.examType] = (map[e.examType] ?? 0) + 1;
    });
    return map;
  }, [exams]);

  // 자격증별 난이도 카운트 — 정규와 동일 패턴.
  const difficultyCounts = useMemo(() => {
    if (!exams) return {} as Record<string, number>;
    const counts: Record<string, number> = {
      ALL: 0,
      "쉬움": 0,
      "보통": 0,
      "어려움": 0,
      "매우 어려움": 0,
    };
    exams
      .filter((e) => e.kind === "MINI" && e.examType === activeCert)
      .forEach((e) => {
        counts.ALL++;
        if (e.difficultyLabel) counts[e.difficultyLabel] = (counts[e.difficultyLabel] ?? 0) + 1;
      });
    return counts;
  }, [exams, activeCert]);

  return (
    <Container size="narrow" className="py-16">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">미니 모의고사</h2>
          <p className="mt-2 text-sm text-text-muted">
            {token.labelLong} · 미니 모의고사로 짧고 굵게 시험 대비해보세요
          </p>
        </div>
        {/* mini 도 정규 모의고사와 합산되는 일일 1회 한도(mockUsed/mockLimit) 적용 — 백엔드 정책 */}
        <QuotaBadge kind="mock" className="mt-1 shrink-0" />
      </div>

      {/* 자격증 탭 */}
      <div className="mt-6 -mx-1 flex gap-1 overflow-x-auto rounded-lg border border-border bg-surface p-1 text-sm">
        {CERT_LIST.map((c) => {
          const active = c.key === activeCert;
          const count = countsByCert[c.key] ?? 0;
          const newCount = exams
            ? exams.filter((e) => e.kind === "MINI" && e.examType === c.key && isExamNew(e))
                .length
            : 0;
          return (
            <Link
              key={c.key}
              href={`/mini-mock-exams?cert=${c.key}`}
              scroll={false}
              className={`flex shrink-0 items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                active
                  ? "bg-primary/10 text-primary ring-1 ring-primary/20"
                  : "text-text-muted hover:bg-surface-hover hover:text-text"
              }`}
            >
              <span className={`h-1.5 w-1.5 rounded-full ${c.tailwind.dot}`} />
              {c.label}
              <span className="text-xs opacity-60 tabular-nums">{count}</span>
              {newCount > 0 && (
                <span
                  className="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400"
                  aria-label={`새로운 미니 회차 ${newCount}개`}
                >
                  NEW
                </span>
              )}
            </Link>
          );
        })}
      </div>

      {/* 난이도 필터 칩 — 정규 페이지와 동일 */}
      <div className="mt-3 flex flex-wrap gap-1.5">
        {DIFFICULTY_OPTIONS.map((opt) => {
          const active = difficulty === opt.value;
          const count = difficultyCounts[opt.value] ?? 0;
          if (opt.value !== "ALL" && count === 0) return null;
          return (
            <button
              key={opt.value}
              onClick={() => setDifficulty(opt.value)}
              className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors ${
                active
                  ? `${opt.activeClass} ring-1 ring-current/30`
                  : "border-border bg-surface text-text-muted hover:border-primary/40 hover:text-text"
              }`}
            >
              {opt.label}
              <span className="text-[10px] opacity-60 tabular-nums">{count}</span>
            </button>
          );
        })}
      </div>

      {/* 목록 */}
      <div className="mt-8">
        {error && (
          <Card padding="md" className="border-danger/40 bg-danger/10 text-sm text-danger">
            {error}
          </Card>
        )}

        {!error && exams === null && (
          <div className="flex justify-center py-16">
            <Spinner message="미니 모의고사 목록 불러오는 중..." />
          </div>
        )}

        {!error && filtered && filtered.length === 0 && (
          <Card padding="lg" className="text-center">
            <p className="text-base font-semibold">아직 준비 중입니다</p>
            <p className="mt-2 text-sm text-text-muted">
              {token.label} 모의고사가 아직 등록되지 않았습니다.
            </p>
          </Card>
        )}

        {filtered && filtered.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {filtered.map((exam) => (
              <MockExamCard key={exam.id} exam={exam} />
            ))}
          </div>
        )}
      </div>

      <div className="mt-14 flex items-center justify-center gap-6 text-sm text-text-muted">
        <Link href="/solve" className="transition-colors hover:text-text">
          무한 문제 풀이 →
        </Link>
        <span className="text-border">·</span>
        <Link href="/past-exams" className="transition-colors hover:text-text">
          기출 복원 →
        </Link>
      </div>
    </Container>
  );
}
