"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import Spinner from "@/components/Spinner";
import { getMockExams, type ExamType, type MockExamSummary } from "@/lib/mockExamApi";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";

export default function MockExamsPage() {
  // 초기 렌더는 항상 게스트 미리보기 (SEO + 로그인 전 콘텐츠 노출).
  // 마운트 후 로그인 감지되면 실제 회차 목록을 불러와 교체.
  const [authed, setAuthed] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    setAuthed(isLoggedIn());
    setAuthChecked(true);
  }, []);

  if (authChecked && authed) {
    return <MockExamsListContent />;
  }
  return <MockExamsGuestPreview />;
}

function MockExamsGuestPreview() {
  function startLogin() {
    window.location.href = getGoogleLoginUrl();
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">모의고사</h1>
        <p className="mt-2 text-sm text-muted">
          SQLD 50문항 · 정보처리기사 실기 20문항 — 실제 시험과 동일한 환경의 무료 CBT
        </p>

        <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <GuestExamCard
            badge="SQLD"
            accent="amber"
            title="SQLD 50문항 모의고사"
            desc="데이터 모델링 + SQL 활용. 매번 새로 추가되는 기출 50문제를 제한 시간 안에."
            onStart={startLogin}
          />
          <GuestExamCard
            badge="정처기 실기"
            accent="emerald"
            title="정보처리기사 실기 20문항"
            desc="알고리즘·SQL·디자인패턴·네트워크. 단답/약술형 20문제로 실전 감각 점검."
            onStart={startLogin}
          />
        </div>

        <section className="mt-10 rounded-xl border border-border bg-surface p-6">
          <h2 className="text-base font-semibold">로그인하면 이런 게 가능해요</h2>
          <ul className="mt-3 space-y-2 text-sm text-muted">
            <li className="flex gap-2"><span className="text-amber-400">✓</span> 회차별 점수와 풀이 시간이 자동 기록됩니다</li>
            <li className="flex gap-2"><span className="text-amber-400">✓</span> 틀린 문제만 모은 오답 노트가 자격증별로 누적됩니다</li>
            <li className="flex gap-2"><span className="text-amber-400">✓</span> 매 응시마다 새로운 문제 세트가 준비됩니다</li>
          </ul>
          <button
            onClick={startLogin}
            className="mt-5 inline-flex items-center gap-3 rounded-xl border border-border bg-white px-6 py-3 text-sm font-semibold text-zinc-800 shadow-sm transition-all duration-200 hover:shadow-lg hover:scale-[1.02] active:scale-[0.98]"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            Google로 모의고사 시작하기
          </button>
        </section>

        <section className="mt-8 rounded-xl border border-border bg-surface/50 p-6">
          <p className="text-sm text-muted">
            로그인 없이 개별 기출문제를 먼저 보고 싶다면{" "}
            <Link href="/learn" className="text-amber-400 underline-offset-2 hover:underline">
              학습 허브에서 문제 둘러보기
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
}

function GuestExamCard({
  badge,
  accent,
  title,
  desc,
  onStart,
}: {
  badge: string;
  accent: "amber" | "emerald";
  title: string;
  desc: string;
  onStart: () => void;
}) {
  const border = accent === "emerald" ? "hover:border-emerald-500/40" : "hover:border-amber-500/40";
  const glow =
    accent === "emerald"
      ? "hover:shadow-[0_0_20px_rgba(16,185,129,0.2)]"
      : "hover:shadow-[0_0_16px_var(--glow)]";
  const badgeCls =
    accent === "emerald"
      ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
      : "border-amber-500/40 bg-amber-500/10 text-amber-300";
  const dot = accent === "emerald" ? "bg-emerald-400" : "bg-amber-400";

  return (
    <button
      onClick={onStart}
      className={`text-left rounded-xl border border-border bg-surface p-5 transition-all hover:-translate-y-0.5 ${border} ${glow}`}
    >
      <span
        className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${badgeCls}`}
      >
        <span className={`h-1.5 w-1.5 rounded-full ${dot}`} />
        {badge}
      </span>
      <h2 className="mt-3 text-lg font-semibold leading-tight">{title}</h2>
      <p className="mt-2 text-sm text-muted leading-relaxed">{desc}</p>
      <p className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-amber-400">
        로그인하고 시작하기 →
      </p>
    </button>
  );
}

type Filter = ExamType;

function MockExamsListContent() {
  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>("SQLD");

  useEffect(() => {
    getMockExams()
      .then(setExams)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  const filtered = useMemo(() => {
    if (!exams) return null;
    // 1) examType 필터 → 2) 미풀이 우선 → 3) 같은 그룹은 sequence 내림차순 (최신순)
    return exams
      .filter((e) => e.examType === filter)
      .slice()
      .sort((a, b) => {
        if (a.solved !== b.solved) return a.solved ? 1 : -1;
        return b.sequence - a.sequence;
      });
  }, [exams, filter]);

  if (error) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <p className="text-red-400">{error}</p>
      </main>
    );
  }

  if (!exams || !filtered) {
    return (
      <main className="min-h-screen bg-background text-foreground flex items-center justify-center">
        <Spinner message="모의고사 목록 불러오는 중..." />
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6">
        <h1 className="text-2xl font-bold sm:text-3xl">모의고사</h1>
        <p className="mt-2 text-sm text-muted">
          SQLD 50문항 · 정보처리기사 실기 20문항 모의고사 모두 지원
        </p>

        {/* 필터 탭 */}
        <div className="mt-6 flex gap-2 rounded-lg border border-border bg-surface p-1 text-sm">
          <FilterTab
            label="SQLD"
            count={exams.filter((e) => e.examType === "SQLD").length}
            active={filter === "SQLD"}
            onClick={() => setFilter("SQLD")}
            accent="amber"
          />
          <FilterTab
            label="정처기 실기"
            count={exams.filter((e) => e.examType === "ENGINEER_PRACTICAL").length}
            active={filter === "ENGINEER_PRACTICAL"}
            onClick={() => setFilter("ENGINEER_PRACTICAL")}
            accent="emerald"
          />
        </div>

        {filtered.length === 0 ? (
          <div className="mt-12 rounded-xl border border-border bg-surface p-8 text-center text-muted">
            해당 시험의 모의고사가 아직 없습니다.
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {filtered.map((exam) => (
              <MockExamCard key={exam.id} exam={exam} />
            ))}
          </div>
        )}
      </div>
    </main>
  );
}

function FilterTab({
  label,
  count,
  active,
  onClick,
  accent,
}: {
  label: string;
  count: number;
  active: boolean;
  onClick: () => void;
  accent?: "amber" | "emerald";
}) {
  const activeClass =
    accent === "emerald"
      ? "bg-emerald-500/15 text-emerald-300 ring-1 ring-emerald-500/30"
      : accent === "amber"
      ? "bg-amber-500/15 text-amber-300 ring-1 ring-amber-500/30"
      : "bg-border text-foreground";
  return (
    <button
      onClick={onClick}
      className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition ${
        active ? activeClass : "text-muted hover:text-foreground"
      }`}
    >
      {label} <span className="ml-1 text-xs opacity-60 tabular-nums">{count}</span>
    </button>
  );
}

function MockExamCard({ exam }: { exam: MockExamSummary }) {
  const isEngineer = exam.examType === "ENGINEER_PRACTICAL";
  const hoverBorder = isEngineer ? "hover:border-emerald-500/40" : "hover:border-amber-500/40";
  const glow = isEngineer
    ? "hover:shadow-[0_0_20px_rgba(16,185,129,0.2)]"
    : "hover:shadow-[0_0_16px_var(--glow)]";

  return (
    <Link
      href={`/mock-exams/${exam.id}`}
      className={`block rounded-xl border border-border bg-surface p-5 transition-all hover:-translate-y-0.5 ${hoverBorder} ${glow}`}
    >
      <div className="flex items-center justify-between gap-2">
        <ExamBadge examType={exam.examType} />
        <div className="flex items-center gap-2">
          <DifficultyBadge label={exam.difficultyLabel} />
          <span className="text-xs text-muted tabular-nums">#{exam.sequence}</span>
        </div>
      </div>
      <h2 className="mt-3 text-lg font-semibold leading-tight">{exam.name}</h2>
      <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-muted">
        <span>총 {exam.totalQuestions}문항</span>
        {exam.solved && exam.bestCorrectCount != null && exam.bestTotalCount != null && (
          <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2 py-0.5 text-[11px] font-medium text-emerald-300">
            ✓ 풀이 완료
            <span className="opacity-80 tabular-nums">
              · 최고 {exam.bestCorrectCount}/{exam.bestTotalCount}
            </span>
          </span>
        )}
      </div>
      <p className="mt-1 text-xs text-muted/70">
        {new Date(exam.createdAt).toLocaleDateString("ko-KR")}
      </p>
    </Link>
  );
}

export function DifficultyBadge({ label }: { label: MockExamSummary["difficultyLabel"] }) {
  if (!label) return null;
  const cls = difficultyBadgeClass(label);
  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-bold ${cls}`}>
      {label}
    </span>
  );
}

function difficultyBadgeClass(label: NonNullable<MockExamSummary["difficultyLabel"]>): string {
  switch (label) {
    case "쉬움":
      return "border-emerald-500/40 bg-emerald-500/10 text-emerald-300";
    case "보통":
      return "border-amber-500/40 bg-amber-500/10 text-amber-300";
    case "어려움":
      return "border-orange-500/40 bg-orange-500/10 text-orange-300";
    case "매우 어려움":
      return "border-red-500/40 bg-red-500/10 text-red-300";
  }
}

export function ExamBadge({ examType }: { examType: ExamType }) {
  if (examType === "ENGINEER_PRACTICAL") {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-emerald-300">
        <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
        정처기 실기
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/40 bg-amber-500/10 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-amber-300">
      <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
      SQLD
    </span>
  );
}
