import type { Metadata } from "next";
import Link from "next/link";
import { getPublicCerts } from "@/lib/publicApi";
import { Container } from "@/components/ui";

export const metadata: Metadata = {
  title: "자격증 기출문제 한눈에 보기",
  description:
    "SQLD, 정보처리기사 실기 등 IT 자격증의 기출 유형과 해설을 과목별로 확인하세요. 로그인 없이 바로 문제와 해설을 볼 수 있습니다.",
  alternates: { canonical: "https://www.sqldpass.com/learn" },
  openGraph: {
    title: "자격증 기출문제 한눈에 보기 | 문어CBT",
    description:
      "SQLD, 정보처리기사 실기 등 IT 자격증의 기출 유형과 해설을 과목별로.",
    url: "https://www.sqldpass.com/learn",
  },
};

export default async function LearnPage() {
  let certs: Awaited<ReturnType<typeof getPublicCerts>> = [];
  try {
    certs = await getPublicCerts();
  } catch {
    /* API 실패 시 빈 목록 */
  }

  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: "https://www.sqldpass.com" },
      { "@type": "ListItem", position: 2, name: "자격증 기출문제", item: "https://www.sqldpass.com/learn" },
    ],
  };

  const collectionLd = {
    "@context": "https://schema.org",
    "@type": "CollectionPage",
    name: "IT 자격증 기출문제",
    url: "https://www.sqldpass.com/learn",
    mainEntity: {
      "@type": "ItemList",
      itemListElement: certs.map((cert, i) => ({
        "@type": "ListItem",
        position: i + 1,
        name: cert.name,
        url: `https://www.sqldpass.com/learn/${cert.slug}`,
      })),
    },
  };

  return (
    <main className="py-16">
      <Container size="default">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(collectionLd) }}
      />
      <header className="mb-12">
        <p className="text-sm text-muted">Learn</p>
        <h1 className="mt-2 text-3xl font-bold sm:text-4xl">
          IT 자격증 기출문제
        </h1>
        <p className="mt-3 max-w-2xl text-muted">
          과목별 실제 기출 유형과 해설을 로그인 없이 바로 확인하세요. 실전
          모의고사는 가입 후 무료로 이용할 수 있습니다.
        </p>
      </header>

      {certs.length === 0 ? (
        <p className="text-muted">자격증 목록을 불러올 수 없습니다.</p>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {certs.map((cert) => (
            <Link
              key={cert.slug}
              href={`/learn/${cert.slug}`}
              className="group relative overflow-hidden rounded-xl border border-border bg-surface p-6 transition-all duration-300 hover:scale-[1.02] active:scale-[0.98] hover:border-amber-500/40 hover:shadow-[0_0_24px_var(--glow)]"
            >
              <div className="flex items-center justify-between">
                <span
                  className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-medium ${
                    cert.slug === "sqld"
                      ? "border-amber-500/30 bg-amber-500/10 text-amber-300"
                      : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
                  }`}
                >
                  {cert.slug === "sqld" ? "SQLD" : "정처기 실기"}
                </span>
                <span className="font-mono text-xs text-muted">
                  {cert.questionCount}문제 · {cert.categoryCount}과목
                </span>
              </div>
              <h2 className="mt-4 text-xl font-bold">{cert.name}</h2>
              <p className="mt-2 text-base leading-relaxed text-muted">
                {cert.description}
              </p>
              <div className="mt-6 flex items-center gap-1.5 text-sm font-medium text-amber-300">
                과목별 기출 보기
                <svg
                  className="h-4 w-4 transition-transform group-hover:translate-x-0.5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2.5}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M13 7l5 5m0 0l-5 5m5-5H6"
                  />
                </svg>
              </div>
            </Link>
          ))}
        </div>
      )}

      <section className="mt-16 rounded-xl border border-border bg-surface/50 p-6">
        <h2 className="text-xl font-semibold">
          실전 모의고사로 합격을 준비하세요
        </h2>
        <p className="mt-2 text-base text-muted">
          매번 새로 추가되는 실전형 세트, 오답 자동 복습, 회차별 실력
          추적을 무료로 이용할 수 있습니다.
        </p>
        <Link
          href="/solve"
          className="btn-glow mt-4 inline-flex items-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover"
        >
          무료로 시작하기
        </Link>
      </section>
      </Container>
    </main>
  );
}
