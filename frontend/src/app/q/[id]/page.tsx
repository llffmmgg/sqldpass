import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getPublicQuestionDetail } from "@/lib/publicApi";

type Params = { id: string };

async function fetchQuestion(idStr: string) {
  const id = Number(idStr);
  if (!Number.isFinite(id) || id <= 0) return null;
  try {
    return await getPublicQuestionDetail(id);
  } catch {
    return null;
  }
}

function stripCode(text: string) {
  return text
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export async function generateMetadata(
  { params }: { params: Promise<Params> },
): Promise<Metadata> {
  const { id } = await params;
  const q = await fetchQuestion(id);
  if (!q) return { title: "문제를 찾을 수 없습니다" };

  const certTag = q.certSlug === "engineer" ? "정처기 실기" : "SQLD";
  const topic = q.topic ? `${q.topic} ` : "";
  const title = `[${certTag}] ${topic}${q.categoryName} 기출 #${q.id}`;
  const preview = stripCode(q.content);
  const description = preview.length > 155
    ? preview.substring(0, 155) + "..."
    : preview;

  return {
    title,
    description,
    alternates: { canonical: `https://www.sqldpass.com/q/${q.id}` },
    openGraph: {
      type: "article",
      title: `${title} | SQLD Pass`,
      description,
      url: `https://www.sqldpass.com/q/${q.id}`,
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
    },
  };
}

export default async function QuestionPage(
  { params }: { params: Promise<Params> },
) {
  const { id } = await params;
  const q = await fetchQuestion(id);
  if (!q) notFound();

  const certTag = q.certSlug === "engineer" ? "정처기 실기" : "SQLD";
  const categorySlug = `cat-${q.categoryId}`;

  // JSON-LD QAPage 스키마 — Google Rich Result 대상
  const qaPageLd = {
    "@context": "https://schema.org",
    "@type": "QAPage",
    mainEntity: {
      "@type": "Question",
      name: q.topic ? `[${certTag}] ${q.topic}` : `[${certTag}] ${q.categoryName}`,
      text: stripCode(q.content).substring(0, 500),
      answerCount: 1,
      acceptedAnswer: {
        "@type": "Answer",
        text:
          (q.answer ?? (q.correctOption != null ? `${q.correctOption}번` : "")) +
          (q.explanation ? " — " + q.explanation : ""),
      },
    },
  };

  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      {
        "@type": "ListItem",
        position: 1,
        name: "자격증",
        item: "https://www.sqldpass.com/learn",
      },
      {
        "@type": "ListItem",
        position: 2,
        name: certTag,
        item: `https://www.sqldpass.com/learn/${q.certSlug}`,
      },
      {
        "@type": "ListItem",
        position: 3,
        name: q.categoryName,
        item: `https://www.sqldpass.com/learn/${q.certSlug}/${categorySlug}`,
      },
      {
        "@type": "ListItem",
        position: 4,
        name: `#${q.id}`,
        item: `https://www.sqldpass.com/q/${q.id}`,
      },
    ],
  };

  const isMcq = q.questionType === "MCQ";

  return (
    <main className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(qaPageLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />

      <nav className="text-sm text-muted">
        <Link href="/learn" className="hover:text-foreground">
          자격증
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/learn/${q.certSlug}`} className="hover:text-foreground">
          {certTag}
        </Link>
        <span className="mx-2">/</span>
        <Link
          href={`/learn/${q.certSlug}/${categorySlug}`}
          className="hover:text-foreground"
        >
          {q.categoryName}
        </Link>
      </nav>

      <header className="mt-4">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <span
            className={`inline-flex items-center rounded-md border px-2 py-0.5 font-medium ${
              q.certSlug === "sqld"
                ? "border-amber-500/30 bg-amber-500/10 text-amber-300"
                : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
            }`}
          >
            {certTag}
          </span>
          <span className="rounded bg-violet-500/10 px-2 py-0.5 text-violet-300">
            {q.categoryName}
          </span>
          {q.topic && (
            <span className="rounded bg-surface px-2 py-0.5 text-muted">
              {q.topic}
            </span>
          )}
          {q.difficulty != null && (
            <span className="rounded bg-amber-500/10 px-2 py-0.5 text-amber-300">
              난이도 {q.difficulty}
            </span>
          )}
          <span className="font-mono text-muted/60">{q.questionType}</span>
        </div>
        <h1 className="mt-4 text-2xl font-bold sm:text-3xl">
          {certTag} {q.topic ?? q.categoryName} 기출문제 #{q.id}
        </h1>
      </header>

      <article className="mt-8 space-y-6">
        <section className="rounded-xl border border-border bg-surface p-6">
          <h2 className="text-sm font-semibold text-muted">문제</h2>
          <pre className="mt-3 whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-foreground">
            {q.content}
          </pre>
        </section>

        <section className="rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-6">
          <h2 className="text-sm font-semibold text-emerald-300">정답</h2>
          {isMcq ? (
            <p className="mt-2 text-base font-bold text-foreground">
              {q.correctOption ?? "-"}번
            </p>
          ) : (
            <pre className="mt-2 whitespace-pre-wrap break-words font-sans text-base font-semibold text-foreground">
              {q.answer ?? "-"}
            </pre>
          )}
          {q.keywords.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-1.5">
              {q.keywords.map((kw, i) => (
                <span
                  key={i}
                  className="rounded bg-emerald-500/10 px-2 py-0.5 text-[11px] text-emerald-300"
                >
                  {kw}
                </span>
              ))}
            </div>
          )}
        </section>

        {q.explanation && (
          <section className="rounded-xl border border-border bg-surface p-6">
            <h2 className="text-sm font-semibold text-muted">해설</h2>
            <pre className="mt-3 whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-foreground/90">
              {q.explanation}
            </pre>
          </section>
        )}
      </article>

      <section className="mt-10 rounded-xl border border-border bg-surface/50 p-6 text-center">
        <h2 className="text-lg font-semibold">
          이런 문제 20~50개를 한 번에 풀어보세요
        </h2>
        <p className="mt-2 text-sm text-muted">
          매번 새로 생성되는 AI 모의고사 + 오답 자동 복습 + 회차별 실력 추적.
          회원가입 후 무료 이용.
        </p>
        <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/mock-exams"
            className="btn-glow inline-flex items-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-zinc-900 transition-all hover:bg-primary-hover"
          >
            {certTag} 모의고사 풀기
          </Link>
          <Link
            href={`/learn/${q.certSlug}/${categorySlug}`}
            className="inline-flex items-center rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-foreground transition-all hover:border-violet-500/50"
          >
            같은 카테고리 더 보기
          </Link>
        </div>
      </section>
    </main>
  );
}
