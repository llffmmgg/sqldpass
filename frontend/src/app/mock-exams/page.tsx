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
import { isLoggedIn } from "@/lib/auth";
import { getGoogleLoginUrl } from "@/lib/oauth";
import { Badge, Card, Container } from "@/components/ui";
import { CERT_LIST, CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";

export default function MockExamsPage() {
  const [authed, setAuthed] = useState(false);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    setAuthed(isLoggedIn());
    setAuthChecked(true);
  }, []);

  if (authChecked && authed) {
    return (
      <Suspense fallback={null}>
        <MockExamsListContent />
      </Suspense>
    );
  }
  return <MockExamsGuestPreview />;
}

function MockExamsGuestPreview() {
  function startLogin() {
    window.location.href = getGoogleLoginUrl();
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-16">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">모의고사</h1>
        <p className="mt-2 text-sm text-text-muted">
          SQLD 50문항 · 정보처리기사 실기 20문항 — 실제 시험과 동일한 환경의 무료 CBT
        </p>

        <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <GuestExamCard
            cert="SQLD"
            title="SQLD 50문항 모의고사"
            desc="데이터 모델링 + SQL 활용. 매번 새로 추가되는 기출 50문제를 제한 시간 안에."
            onStart={startLogin}
          />
          <GuestExamCard
            cert="ENGINEER_PRACTICAL"
            title="정보처리기사 실기 20문항"
            desc="알고리즘·SQL·디자인패턴·네트워크. 단답/약술형 20문제로 실전 감각 점검."
            onStart={startLogin}
          />
        </div>

        <Card padding="lg" className="mt-10">
          <h2 className="text-base font-semibold">로그인하면 이런 게 가능해요</h2>
          <ul className="mt-3 space-y-2 text-sm text-text-muted">
            <li className="flex gap-2"><span className="text-primary">✓</span> 회차별 점수와 풀이 시간이 자동 기록됩니다</li>
            <li className="flex gap-2"><span className="text-primary">✓</span> 틀린 문제만 모은 오답 노트가 자격증별로 누적됩니다</li>
            <li className="flex gap-2"><span className="text-primary">✓</span> 매 응시마다 새로운 문제 세트가 준비됩니다</li>
          </ul>
          <button
            onClick={startLogin}
            className="mt-5 inline-flex h-11 items-center gap-3 rounded-lg border border-border bg-surface px-5 text-sm font-semibold text-text transition-all hover:border-primary/40 hover:bg-surface-hover"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            Google로 모의고사 시작하기
          </button>
        </Card>

        <Card padding="md" className="mt-6">
          <p className="text-sm text-text-muted">
            로그인 없이 개별 기출문제를 먼저 보고 싶다면{" "}
            <Link href="/learn" className="text-primary underline-offset-2 hover:underline">
              학습 허브에서 문제 둘러보기
            </Link>
          </p>
        </Card>
      </Container>
    </main>
  );
}

function GuestExamCard({
  cert,
  title,
  desc,
  onStart,
}: {
  cert: CertKey;
  title: string;
  desc: string;
  onStart: () => void;
}) {
  const token = CERT_TOKENS[cert];
  return (
    <button onClick={onStart} className="text-left">
      <Card variant="interactive" padding="md" accent={cert} className="h-full">
        <Badge cert={cert} variant="soft" size="xs" dot>
          {token.label}
        </Badge>
        <h2 className="mt-3 text-base font-semibold leading-tight">{title}</h2>
        <p className="mt-2 text-sm leading-relaxed text-text-muted">{desc}</p>
        <p className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-primary">
          로그인하고 시작하기 →
        </p>
      </Card>
    </button>
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

  useEffect(() => {
    if (isExamType(certParam)) setFilter(certParam);
  }, [certParam]);

  useEffect(() => {
    getMockExams()
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
              <MockExamCard key={exam.id} exam={exam} />
            ))}
          </div>
        )}
      </Container>
    </main>
  );
}

function MockExamCard({ exam }: { exam: MockExamSummary }) {
  const cert = certFromExamType(exam.examType);
  const isPremium = exam.visibility === "PREMIUM";

  return (
    <Link href={`/mock-exams/${exam.id}`} className="group relative block">
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
