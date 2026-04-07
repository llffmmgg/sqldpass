import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import {
  getPublicCategoriesByCert,
  getPublicQuestionsByCategory,
  parseCategorySlug,
} from "@/lib/publicApi";

const CERT_NAMES: Record<string, string> = {
  sqld: "SQLD",
  engineer: "정처기 실기",
};

type Params = { cert: string; category: string };

async function resolveCategory(cert: string, categorySlug: string) {
  const catId = parseCategorySlug(categorySlug);
  if (catId == null) return null;
  try {
    const categories = await getPublicCategoriesByCert(cert);
    return categories.find((c) => c.id === catId) ?? null;
  } catch {
    return null;
  }
}

export async function generateMetadata(
  { params }: { params: Promise<Params> },
): Promise<Metadata> {
  const { cert, category } = await params;
  const certName = CERT_NAMES[cert];
  if (!certName) return { title: "자격증을 찾을 수 없습니다" };

  const cat = await resolveCategory(cert, category);
  if (!cat) return { title: "카테고리를 찾을 수 없습니다" };

  const title = `${certName} ${cat.name} 기출문제`;
  const description = `${certName} ${cat.name} 과목의 기출 유형과 해설 ${cat.questionCount}문제. 로그인 없이 바로 확인하세요.`;
  return {
    title,
    description,
    alternates: {
      canonical: `https://www.sqldpass.com/learn/${cert}/${category}`,
    },
    openGraph: {
      title: `${title} | SQLD Pass`,
      description,
      url: `https://www.sqldpass.com/learn/${cert}/${category}`,
    },
  };
}

export default async function CategoryPage(
  { params }: { params: Promise<Params> },
) {
  const { cert, category } = await params;
  const certName = CERT_NAMES[cert];
  if (!certName) notFound();

  const cat = await resolveCategory(cert, category);
  if (!cat) notFound();

  let questions: Awaited<
    ReturnType<typeof getPublicQuestionsByCategory>
  > | null = null;
  try {
    questions = await getPublicQuestionsByCategory(cat.id, 0, 50);
  } catch {
    /* 실패 */
  }

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
        name: certName,
        item: `https://www.sqldpass.com/learn/${cert}`,
      },
      {
        "@type": "ListItem",
        position: 3,
        name: cat.name,
        item: `https://www.sqldpass.com/learn/${cert}/${category}`,
      },
    ],
  };

  return (
    <main className="mx-auto max-w-4xl px-4 py-16 sm:px-6 lg:px-8">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />

      <nav className="text-sm text-muted">
        <Link href="/learn" className="hover:text-foreground">
          자격증
        </Link>
        <span className="mx-2">/</span>
        <Link href={`/learn/${cert}`} className="hover:text-foreground">
          {certName}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-foreground">{cat.name}</span>
      </nav>

      <header className="mt-4 mb-10">
        <h1 className="text-3xl font-bold sm:text-4xl">
          {certName} <span className="text-amber-300">{cat.name}</span> 기출문제
        </h1>
        <p className="mt-3 text-muted">
          {cat.parentName} · 총 {cat.questionCount}문제
        </p>
      </header>

      {!questions || questions.questions.length === 0 ? (
        <p className="text-muted">
          이 카테고리의 공개 문제가 아직 없습니다.
        </p>
      ) : (
        <ul className="space-y-3">
          {questions.questions.map((q, idx) => (
            <li key={q.id}>
              <Link
                href={`/q/${q.id}`}
                className="group flex items-start gap-4 rounded-lg border border-border bg-surface p-5 transition-all hover:border-amber-500/30 hover:bg-surface/80"
              >
                <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-amber-500/10 font-mono text-xs text-amber-300">
                  {String(idx + 1).padStart(2, "0")}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 text-xs text-muted">
                    {q.topic && (
                      <span className="rounded bg-violet-500/10 px-1.5 py-0.5 text-violet-300">
                        {q.topic}
                      </span>
                    )}
                    {q.difficulty != null && (
                      <span className="rounded bg-amber-500/10 px-1.5 py-0.5 text-amber-300">
                        난이도 {q.difficulty}
                      </span>
                    )}
                    <span className="font-mono text-muted/60">
                      {q.questionType}
                    </span>
                  </div>
                  <p className="mt-2 text-sm leading-relaxed text-foreground/90 group-hover:text-foreground">
                    {q.contentPreview}
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <section className="mt-16 rounded-xl border border-border bg-surface/50 p-6 text-center">
        <h2 className="text-lg font-semibold">
          {cat.name} 전체를 모의고사로 풀어보기
        </h2>
        <p className="mt-2 text-sm text-muted">
          AI가 매번 새로 만드는 실전 세트로 연습하세요.
        </p>
        <Link
          href="/mock-exams"
          className="btn-glow mt-4 inline-flex items-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-zinc-900 transition-all hover:bg-primary-hover"
        >
          모의고사 풀러 가기
        </Link>
      </section>
    </main>
  );
}
