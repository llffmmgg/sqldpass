"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";

import Spinner from "@/components/Spinner";
import { Badge, Card, Container } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromExamType,
  type CertKey,
} from "@/lib/cert-tokens";
import { isLoggedIn } from "@/lib/auth";
import {
  getMockExams,
  type EngineerTemplateKey,
  type ExamType,
  type MockExamSummary,
} from "@/lib/mockExamApi";
import { isExamNew } from "@/lib/mockExamNew";
import { getPublicMockExams } from "@/lib/publicApi";

export default function MockExamsClient() {
  return (
    <Suspense fallback={null}>
      <MockExamsContent />
    </Suspense>
  );
}

function MockExamsContent() {
  const searchParams = useSearchParams();
  const certParam = searchParams?.get("cert");
  const activeCert: CertKey =
    (certParam && certParam in CERT_TOKENS ? (certParam as CertKey) : null) ?? "SQLD";

  const [exams, setExams] = useState<MockExamSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    const loggedIn = isLoggedIn();
    setAuthed(loggedIn);
    const fetcher = loggedIn ? getMockExams() : getPublicMockExams();
    fetcher
      .then(setExams)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  const token = CERT_TOKENS[activeCert];

  const filtered = useMemo(() => {
    if (!exams) return null;
    return exams
      .filter((e) => e.examType === activeCert)
      .slice()
      .sort((a, b) => b.sequence - a.sequence);
  }, [exams, activeCert]);

  return (
    <Container size="narrow" className="py-16">
      <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">모의고사</h2>
      <p className="mt-2 text-sm text-text-muted">
        {token.labelLong} · 실전 타이머와 함께 무료 CBT 로 응시해보세요. 점수 기록·오답 노트는 로그인 후 자동 저장됩니다.
      </p>

      {/* 자격증 탭 — past-exams 와 동일한 단일 pill + cert dot + count + NEW 카운트 */}
      <div className="mt-6 -mx-1 flex gap-1 overflow-x-auto rounded-lg border border-border bg-surface p-1 text-sm">
        {CERT_LIST.map((c) => {
          const active = c.key === activeCert;
          const count = exams ? exams.filter((e) => e.examType === c.key).length : 0;
          const newCount = exams
            ? exams.filter((e) => e.examType === c.key && isExamNew(e)).length
            : 0;
          return (
            <Link
              key={c.key}
              href={`/mock-exams?cert=${c.key}`}
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
                  className="inline-flex items-center rounded-full bg-emerald-500 px-1.5 text-[9px] font-bold leading-4 text-white"
                  aria-label={`새로운 회차 ${newCount}개`}
                >
                  +{newCount}
                </span>
              )}
            </Link>
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
            <Spinner message="모의고사 목록 불러오는 중..." />
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
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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

function MockExamCard({ exam }: { exam: MockExamSummary }) {
  const cert = certFromExamType(exam.examType);
  const isPremium = exam.visibility === "PREMIUM";
  const isNew = isExamNew(exam);
  // PREMIUM 이면서 아직 결제로 잠금 해제하지 않았으면 결제 페이지로 유도.
  // (화이트리스트 닉네임은 백엔드 가드에서 자동 통과되므로 결제 페이지를 일부러 보고 싶을 때만 진입.)
  const href =
    isPremium && !exam.purchased
      ? `/checkout?examId=${exam.id}`
      : `/mock-exams/${exam.id}`;

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
          {exam.kind === "PAST_EXAM" && exam.examYear != null && exam.examRound != null && (
            <span className="inline-flex items-center rounded-full border border-border bg-surface px-2 py-0.5 text-[10px] font-semibold text-text-muted">
              기출 {exam.examYear} {exam.examRound}회
            </span>
          )}
          {exam.templateKey && exam.templateLabel && (
            <TemplateBadge templateKey={exam.templateKey} label={exam.templateLabel} />
          )}
          {isPremium && (
            <Badge variant="soft" size="xs" className="border-amber-500/50 bg-amber-500/15 text-amber-600 dark:text-amber-300">
              🔒 프리미엄
            </Badge>
          )}
        </div>

        {isNew && (
          /* eslint-disable-next-line @next/next/no-img-element -- 정적 배지 이미지, next/image 의 추가 최적화가 비용 대비 의미 없음 */
          <img
            src="/badges/new-logo.webp"
            alt="NEW"
            aria-label="새로 추가된 회차"
            className="pointer-events-none absolute -bottom-1 -right-1 z-10 h-16 w-16 select-none rotate-[8deg] object-contain drop-shadow-[0_4px_10px_rgba(16,185,129,0.35)] transition-transform duration-300 ease-out group-hover:-rotate-3 group-hover:scale-110 sm:h-20 sm:w-20"
          />
        )}

        <div className="mt-2 flex items-center gap-2">
          <DifficultyBadge label={exam.difficultyLabel} />
          <span className="text-xs text-text-muted tabular-nums">#{exam.sequence}</span>
        </div>

        <h2 className="mt-3 text-lg font-semibold leading-tight">{exam.name}</h2>
        <div className="mt-2 text-sm text-text-muted">총 {exam.totalQuestions}문항</div>

        <div className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-primary transition-transform group-hover:translate-x-1">
          응시하기 →
        </div>
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
