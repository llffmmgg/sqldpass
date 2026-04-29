import type { Metadata } from "next";
import Link from "next/link";

import { Container } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromRootName,
  type CertKey,
} from "@/lib/cert-tokens";
import {
  getHardestQuestions,
  getPublicCategoriesByCert,
  type HardestQuestions,
  type PublicCategory,
} from "@/lib/publicApi";

export const revalidate = 1800; // 30분 ISR

export const metadata: Metadata = {
  title: "과목별 오답률 best 30 — 어려운 문제 모음",
  description:
    "공부한 학생들 (평균 정답률 50% 이상) 이 자주 틀리는 SQLD·정처기·컴활·ADsP 문제 톱 30. 시험 직전 약점 보강용.",
  alternates: { canonical: "https://www.sqldpass.com/insights/hardest" },
};

type Search = { cert?: string; subjectId?: string };

export default async function HardestQuestionsPage({
  searchParams,
}: {
  searchParams: Promise<Search>;
}) {
  const params = await searchParams;
  const certParam = (params.cert ?? "sqld") as string;
  const cert: CertKey = certFromRootName(certNameFromSlug(certParam));

  // 자격증의 과목(카테고리) 목록 — 사용자가 드롭다운으로 선택
  const categories = await getPublicCategoriesByCert(certParam).catch(() => [] as PublicCategory[]);
  const selectedSubjectId = params.subjectId
    ? Number(params.subjectId)
    : (categories[0]?.id ?? null);

  let data: HardestQuestions | null = null;
  if (selectedSubjectId != null) {
    data = await getHardestQuestions(selectedSubjectId).catch(() => null);
  }

  const token = CERT_TOKENS[cert];

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="default" className="py-12">
        <header className="mb-6">
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
            오답률 best 30 — 어려운 문제 모음
          </h1>
          <p className="mt-2 text-sm text-text-muted">
            공부한 학생들 (평균 정답률 50% 이상 + 풀이 10문제 이상) 이 자주 틀리는 문제를
            과목별로 정리했어요. 시험 직전 약점 보강에 활용해보세요.
          </p>
        </header>

        {/* 자격증 탭 */}
        <div className="mb-4 flex flex-wrap gap-1.5">
          {CERT_LIST.map((c) => {
            const slug = certSlugFromKey(c.key);
            const active = slug === certParam;
            return (
              <Link
                key={c.key}
                href={`/insights/hardest?cert=${slug}`}
                className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                  active
                    ? `${c.tailwind.border} ${c.tailwind.bgSoft} ${c.tailwind.text}`
                    : "border-border bg-surface text-text-muted hover:border-border-strong hover:text-text"
                }`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${c.tailwind.dot}`} aria-hidden />
                {c.label}
              </Link>
            );
          })}
        </div>

        {/* 과목 드롭다운 (chip) */}
        {categories.length > 0 && (
          <div className="mb-6 flex flex-wrap gap-1.5">
            {categories.map((cat) => {
              const active = selectedSubjectId === cat.id;
              return (
                <Link
                  key={cat.id}
                  href={`/insights/hardest?cert=${certParam}&subjectId=${cat.id}`}
                  className={`rounded-md border px-2.5 py-1 text-xs transition-colors ${
                    active
                      ? "border-primary/40 bg-primary/10 text-primary"
                      : "border-border bg-surface text-text-muted hover:border-border-strong hover:text-text"
                  }`}
                >
                  {cat.name}
                </Link>
              );
            })}
          </div>
        )}

        {/* 본문 */}
        {!data || data.items.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-10 text-center">
            <p className="text-base font-semibold">
              {token.label} 의 이 과목에는 아직 통계가 충분하지 않아요
            </p>
            <p className="mt-2 text-sm text-text-muted">
              학생들이 더 풀이해주면 오답률 데이터가 쌓이며 노출됩니다.
            </p>
            <Link
              href={`/solve?cert=${certParam}`}
              className="mt-5 inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-fg hover:bg-primary-hover"
            >
              {token.label} 문제 풀러 가기
            </Link>
          </div>
        ) : (
          <div className="rounded-xl border border-border bg-surface">
            <div className="flex items-baseline justify-between border-b border-border px-5 py-3">
              <h2 className="text-sm font-semibold">
                {data.subjectName}{" "}
                <span className="ml-1 text-xs font-normal text-text-muted">
                  ({data.totalSamples}개 문제)
                </span>
              </h2>
              <span className="text-xs text-text-subtle">오답률 높은 순</span>
            </div>
            <ol className="divide-y divide-border">
              {data.items.map((q, i) => (
                <li key={q.questionId}>
                  <Link
                    href={`/q/${q.questionId}`}
                    className="flex items-start gap-3 px-5 py-3 hover:bg-surface-hover"
                  >
                    <span className="mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-bg-elevated text-[11px] font-bold tabular-nums text-text-muted">
                      {i + 1}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm leading-snug text-text">
                        {q.questionPreview}
                      </p>
                      <div className="mt-1.5 flex flex-wrap items-center gap-2 text-[11px] text-text-muted tabular-nums">
                        <span className={`font-bold ${rateColor(q.wrongRate)}`}>
                          오답률 {q.wrongRate.toFixed(1)}%
                        </span>
                        <span aria-hidden>·</span>
                        <span>
                          {q.wrongCount}/{q.attempts} 틀림
                        </span>
                      </div>
                    </div>
                  </Link>
                </li>
              ))}
            </ol>
          </div>
        )}

        <p className="mt-6 text-xs text-text-subtle">
          * 통계 신뢰도를 위해: 평균 정답률 50%+ 학생들의 답안만 집계 / 5번 이상 풀린 문제만 / 30분마다 갱신
        </p>
      </Container>
    </main>
  );
}

// ---- helpers ----

function certSlugFromKey(key: CertKey): string {
  switch (key) {
    case "SQLD":
      return "sqld";
    case "ENGINEER_PRACTICAL":
      return "engineer";
    case "ENGINEER_WRITTEN":
      return "engineer-written";
    case "COMPUTER_LITERACY_1":
      return "computer-literacy-1";
    case "COMPUTER_LITERACY_2":
      return "computer-literacy-2";
    case "ADSP":
      return "adsp";
  }
}

function certNameFromSlug(slug: string): string {
  switch (slug) {
    case "sqld":
      return "SQLD";
    case "engineer":
      return "정보처리기사 실기";
    case "engineer-written":
      return "정보처리기사 필기";
    case "computer-literacy-1":
      return "컴퓨터활용능력 1급 필기";
    case "computer-literacy-2":
      return "컴퓨터활용능력 2급 필기";
    case "adsp":
      return "데이터분석 준전문가(ADsP)";
    default:
      return "SQLD";
  }
}

function rateColor(rate: number): string {
  if (rate >= 70) return "text-red-400";
  if (rate >= 50) return "text-amber-400";
  return "text-text-muted";
}
