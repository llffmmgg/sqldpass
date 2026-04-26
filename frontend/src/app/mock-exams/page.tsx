"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import Spinner from "@/components/Spinner";
import {
  getMockExams,
  type EngineerTemplateKey,
  type ExamType,
  type MockExamSummary,
} from "@/lib/mockExamApi";
import { getPublicMockExams } from "@/lib/publicApi";
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { Badge, Card, Container } from "@/components/ui";
import { CERT_LIST, CERT_TOKENS, certFromExamType } from "@/lib/cert-tokens";

export default function MockExamsPage() {
  return (
    <Suspense fallback={null}>
      <MockExamsListContent />
    </Suspense>
  );
}

type Filter = ExamType;
type DifficultyFilter = "ALL" | NonNullable<MockExamSummary["difficultyLabel"]>;
type TemplateFilter = "ALL" | EngineerTemplateKey;

const DIFFICULTY_OPTIONS: { value: DifficultyFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "쉬움", label: "쉬움" },
  { value: "보통", label: "보통" },
  { value: "어려움", label: "어려움" },
  { value: "매우 어려움", label: "매우 어려움" },
];

const TEMPLATE_OPTIONS: { value: TemplateFilter; label: string }[] = [
  { value: "ALL", label: "전체 유형" },
  { value: "PROGRAMMING_HEAVY", label: "프로그래밍 편중형" },
  { value: "THEORY_HEAVY", label: "이론 편중형" },
  { value: "BALANCED", label: "균형형" },
  { value: "DB_HEAVY", label: "DB 강조형" },
];

function isExamType(v: string | null): v is ExamType {
  return (
    v === "SQLD" ||
    v === "ENGINEER_PRACTICAL" ||
    v === "ENGINEER_WRITTEN" ||
    v === "COMPUTER_LITERACY_1" ||
    v === "COMPUTER_LITERACY_2" ||
    v === "ADSP"
  );
}

function MockExamsListContent() {
  const searchParams = useSearchParams();
  const certParam = searchParams?.get("cert") ?? null;
  const initialFilter: Filter = isExamType(certParam) ? certParam : "SQLD";

  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>(initialFilter);
  const [difficultyFilter, setDifficultyFilter] = useState<DifficultyFilter>("ALL");
  const [templateFilter, setTemplateFilter] = useState<TemplateFilter>("ALL");
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    if (isExamType(certParam)) setFilter(certParam);
  }, [certParam]);

  useEffect(() => {
    const loggedIn = isLoggedIn();
    setAuthed(loggedIn);
    const fetcher = loggedIn ? getMockExams() : getPublicMockExams();
    fetcher
      .then(setExams)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  useEffect(() => {
    setTemplateFilter("ALL");
  }, [filter]);

  const filtered = useMemo(() => {
    if (!exams) return null;
    return exams
      .filter((e) => e.examType === filter)
      .filter((e) => difficultyFilter === "ALL" || e.difficultyLabel === difficultyFilter)
      .filter((e) =>
        filter === "ENGINEER_PRACTICAL" && templateFilter !== "ALL"
          ? e.templateKey === templateFilter
          : true,
      )
      .slice()
      .sort((a, b) => b.sequence - a.sequence);
  }, [exams, filter, difficultyFilter, templateFilter]);

  if (error) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <p className="text-danger">{error}</p>
      </main>
    );
  }

  if (!exams || !filtered) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-bg text-text">
        <Spinner message="모의고사 목록 불러오는 중..." />
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-16">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">모의고사</h1>
        <p className="mt-2 text-sm text-text-muted">
          SQLD · 정처기 · 컴활 · ADsP — 실전 타이머와 함께 무료 CBT
        </p>

        {!authed && (
          <Card padding="md" className="mt-4 border-primary/30 bg-primary/[0.04]">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm text-text">
                <span className="font-semibold">미리보기</span> — 모의고사 응시·점수 기록은 로그인 후 가능해요.
              </p>
              <Link
                href={getGoogleLoginUrl()}
                className="inline-flex h-9 shrink-0 items-center justify-center rounded-md border border-primary/40 bg-primary/10 px-4 text-xs font-semibold text-primary hover:bg-primary/15"
              >
                Google 로그인
              </Link>
            </div>
          </Card>
        )}

        {/* 자격증 탭 — 단일 pill + cert dot */}
        <div className="mt-6 -mx-1 flex gap-1 overflow-x-auto rounded-lg border border-border bg-surface p-1 text-sm">
          {CERT_LIST.map((c) => {
            const count = exams.filter((e) => e.examType === c.key).length;
            const active = filter === c.key;
            return (
              <button
                key={c.key}
                onClick={() => setFilter(c.key)}
                className={`flex shrink-0 items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  active
                    ? "bg-primary/10 text-primary ring-1 ring-primary/20"
                    : "text-text-muted hover:bg-surface-hover hover:text-text"
                }`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${c.tailwind.dot}`} />
                {c.label}
                <span className="text-xs opacity-60 tabular-nums">{count}</span>
              </button>
            );
          })}
        </div>

        {/* 부필터 */}
        <div className="mt-3 flex flex-col gap-2">
          <SubFilterRow
            label="난이도"
            options={DIFFICULTY_OPTIONS}
            value={difficultyFilter}
            onChange={(v) => setDifficultyFilter(v as DifficultyFilter)}
          />
          {filter === "ENGINEER_PRACTICAL" && (
            <SubFilterRow
              label="유형"
              options={TEMPLATE_OPTIONS}
              value={templateFilter}
              onChange={(v) => setTemplateFilter(v as TemplateFilter)}
            />
          )}
        </div>

        {filtered.length === 0 ? (
          <Card padding="lg" className="mt-12 text-center text-text-muted">
            해당 시험의 모의고사가 아직 없습니다.
          </Card>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {filtered.map((exam) => (
              <MockExamCard key={exam.id} exam={exam} authed={authed} />
            ))}
          </div>
        )}
      </Container>
    </main>
  );
}

function MockExamCard({ exam, authed }: { exam: MockExamSummary; authed: boolean }) {
  const cert = certFromExamType(exam.examType);
  const isPremium = exam.visibility === "PREMIUM";
  const isNew = isWithinDays(exam.createdAt, 3);
  const href = authed ? `/mock-exams/${exam.id}` : getGoogleLoginUrl();

  return (
    <Link href={href} className="group relative block">
      <Card
        variant="interactive"
        padding="md"
        accent={cert ?? undefined}
        className={
          isPremium
            ? "relative overflow-hidden bg-gradient-to-br from-amber-500/[0.06] to-surface hover:border-amber-500/60"
            : "relative overflow-hidden"
        }
      >
        {exam.expertVerified && (
          <div className="pointer-events-none absolute -left-[38px] top-[18px] z-10 -rotate-45 bg-emerald-600 px-10 py-0.5 text-center text-[9px] font-bold tracking-wide text-white shadow-sm dark:bg-emerald-500">
            전문가 검수
          </div>
        )}

        {exam.solved && exam.bestCorrectCount != null && exam.bestTotalCount != null && (
          <span
            className="pointer-events-none absolute right-2 top-1 select-none font-[family-name:var(--font-caveat)] text-3xl font-bold leading-none text-red-500/90 sm:text-4xl"
            style={{ transform: "rotate(-12deg)" }}
          >
            {exam.bestCorrectCount}
            <span className="text-2xl sm:text-3xl">/</span>
            {exam.bestTotalCount}
          </span>
        )}

        <div className="flex flex-wrap items-center gap-1.5 pr-20">
          <ExamBadge examType={exam.examType} />
          {exam.templateKey && exam.templateLabel && (
            <TemplateBadge templateKey={exam.templateKey} label={exam.templateLabel} />
          )}
          {isPremium && (
            <Badge variant="soft" size="xs" className="border-amber-500/50 bg-amber-500/15 text-amber-600 dark:text-amber-300">
              🔒 프리미엄
            </Badge>
          )}
          {isNew && (
            <span className="inline-flex items-center rounded-full border border-rose-500/40 bg-rose-500/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-rose-500 dark:text-rose-300">
              NEW
            </span>
          )}
        </div>

        <div className="mt-2 flex items-center gap-2">
          <DifficultyBadge label={exam.difficultyLabel} />
          <span className="text-xs text-text-muted tabular-nums">#{exam.sequence}</span>
        </div>

        <h2 className="mt-3 text-lg font-semibold leading-tight">{exam.name}</h2>
        <div className="mt-2 text-sm text-text-muted">총 {exam.totalQuestions}문항</div>
      </Card>
    </Link>
  );
}

const TEMPLATE_BADGE_CLASS: Record<EngineerTemplateKey, string> = {
  PROGRAMMING_HEAVY: "border-blue-500/40 bg-blue-500/10 text-blue-500 dark:text-blue-300",
  THEORY_HEAVY: "border-purple-500/40 bg-purple-500/10 text-purple-500 dark:text-purple-300",
  BALANCED: "border-slate-500/40 bg-slate-500/10 text-slate-500 dark:text-slate-300",
  DB_HEAVY: "border-orange-500/40 bg-orange-500/10 text-orange-500 dark:text-orange-300",
  LATEST: "border-emerald-500/40 bg-emerald-500/10 text-emerald-500 dark:text-emerald-300",
};

function TemplateBadge({
  templateKey,
  label,
}: {
  templateKey: EngineerTemplateKey;
  label: string;
}) {
  const cls = TEMPLATE_BADGE_CLASS[templateKey];
  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold ${cls}`}>
      {label}
    </span>
  );
}

function SubFilterRow({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-[11px] font-medium text-text-muted">{label}</span>
      <div className="flex flex-wrap gap-1">
        {options.map((opt) => (
          <button
            key={opt.value}
            onClick={() => onChange(opt.value)}
            className={`rounded-full border px-2.5 py-0.5 text-[11px] font-medium transition ${
              value === opt.value
                ? "border-primary/40 bg-primary/10 text-primary"
                : "border-border text-text-muted hover:border-border-strong hover:text-text"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function isWithinDays(iso: string | null | undefined, days: number): boolean {
  if (!iso) return false;
  const created = new Date(iso).getTime();
  if (Number.isNaN(created)) return false;
  return Date.now() - created <= days * 24 * 60 * 60 * 1000;
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
      return "border-emerald-500/40 bg-emerald-500/10 text-emerald-600 dark:text-emerald-300";
    case "보통":
      return "border-amber-500/40 bg-amber-500/10 text-amber-600 dark:text-amber-300";
    case "어려움":
      return "border-orange-500/40 bg-orange-500/10 text-orange-600 dark:text-orange-300";
    case "매우 어려움":
      return "border-red-500/40 bg-red-500/10 text-red-600 dark:text-red-300";
  }
}

export function ExamBadge({ examType }: { examType: ExamType }) {
  const cert = certFromExamType(examType);
  if (!cert) return null;
  const t = CERT_TOKENS[cert];
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${t.tailwind.border} ${t.tailwind.bgSoft} ${t.tailwind.text}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${t.tailwind.dot}`} />
      {t.label}
    </span>
  );
}
