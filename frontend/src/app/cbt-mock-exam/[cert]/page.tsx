import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Container } from "@/components/ui";
import {
  CERT_LIST,
  CERT_TOKENS,
  certFromSlug,
  slugFromCert,
  type CertKey,
} from "@/lib/cert-tokens";
import { CBT_CERT_INFO } from "@/lib/cbtCertInfo";
import { getPostsByCategory } from "@/lib/blog";

const SITE_URL = "https://www.sqldpass.com";

type Params = { cert: string };

export function generateStaticParams() {
  return CERT_LIST.map((c) => ({ cert: slugFromCert(c.key) }));
}

export async function generateMetadata({
  params,
}: {
  params: Promise<Params>;
}): Promise<Metadata> {
  const { cert: slug } = await params;
  const certKey = certFromSlug(slug);
  if (!certKey) return { title: "자격증을 찾을 수 없습니다" };

  const info = CBT_CERT_INFO[certKey];
  const canonical = `${SITE_URL}/cbt-mock-exam/${slug}`;

  return {
    title: info.title,
    description: info.description,
    keywords: info.keywords,
    alternates: { canonical },
    openGraph: {
      type: "website",
      url: canonical,
      title: info.title,
      description: info.description,
      siteName: "문어CBT",
      locale: "ko_KR",
    },
    twitter: {
      card: "summary_large_image",
      title: info.title,
      description: info.description,
    },
  };
}

export default async function CertCbtLandingPage({
  params,
}: {
  params: Promise<Params>;
}) {
  const { cert: slug } = await params;
  const certKey: CertKey | null = certFromSlug(slug);
  if (!certKey) notFound();

  const info = CBT_CERT_INFO[certKey];
  const token = CERT_TOKENS[certKey];
  const canonical = `${SITE_URL}/cbt-mock-exam/${slug}`;

  const relatedPosts = getPostsByCategory(token.blogCategory).slice(0, 3);

  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: SITE_URL },
      {
        "@type": "ListItem",
        position: 2,
        name: "CBT 모의고사",
        item: `${SITE_URL}/cbt-mock-exam`,
      },
      { "@type": "ListItem", position: 3, name: info.searchKeyword, item: canonical },
    ],
  };

  const faqLd = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: info.faqs.map((f) => ({
      "@type": "Question",
      name: f.q,
      acceptedAnswer: { "@type": "Answer", text: f.a },
    })),
  };

  const webPageLd = {
    "@context": "https://schema.org",
    "@type": "WebPage",
    name: info.title,
    url: canonical,
    description: info.description,
    inLanguage: "ko-KR",
    publisher: { "@type": "Organization", name: "문어CBT", url: SITE_URL },
  };

  return (
    <main className="min-h-screen bg-bg text-text">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(faqLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(webPageLd) }}
      />

      {/* Hero */}
      <section className="relative overflow-hidden border-b border-border">
        <Container size="default" className="py-20 text-center sm:py-28">
          <nav className="mb-6 text-xs text-text-muted" aria-label="브레드크럼">
            <Link href="/cbt-mock-exam" className="hover:text-text">
              CBT 모의고사
            </Link>
            <span className="mx-2">/</span>
            <span className="text-text">{info.searchKeyword}</span>
          </nav>

          <span
            className={`inline-flex items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-xs font-medium ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.textSoft}`}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${token.tailwind.dot}`} />
            {token.label} · CBT 모의고사 무료
          </span>

          <h1 className="mt-6 text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl">
            {info.searchKeyword}
            <br />
            <span className="bg-gradient-to-r from-primary to-[#5ee0a5] bg-clip-text text-transparent">
              무료 모의고사
            </span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-base leading-relaxed text-text-muted sm:text-lg">
            {info.longKeyword} CBT 모의고사를 무료로 풀어보세요. 실전 타이머·자동 채점·오답
            누적까지, 회차별 기출 복원과 AI 변형 문제로 합격선까지 빠르게 도달합니다.
          </p>

          <div className="mt-10 flex flex-wrap items-center justify-center gap-3">
            <Link
              href={`/mock-exams?cert=${certKey}`}
              className="btn-glow inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02]"
            >
              {info.shortKeyword} CBT 모의고사 풀기 →
            </Link>
            <Link
              href={`/past-exams/${slug}`}
              className="inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-medium text-text transition-all hover:border-primary/40"
            >
              {info.shortKeyword} 기출 복원 →
            </Link>
          </div>
        </Container>
      </section>

      {/* 시험 정보 표 */}
      <section className="border-b border-border">
        <Container size="narrow" className="py-16">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            {info.shortKeyword} 시험 정보 한눈에
          </h2>
          <p className="mt-3 text-sm text-text-muted">
            {info.longKeyword} 시험 형식과 합격 기준입니다. CBT 모의고사도 같은 환경으로 구성돼요.
          </p>
          <div className="mt-6 overflow-hidden rounded-xl border border-border bg-surface">
            <table className="w-full text-sm">
              <tbody className="divide-y divide-border">
                <tr>
                  <th className="w-32 bg-bg-elevated px-4 py-3 text-left font-medium text-text-muted">
                    시험 형식
                  </th>
                  <td className="px-4 py-3 font-semibold">{info.examInfo.examType}</td>
                </tr>
                <tr>
                  <th className="bg-bg-elevated px-4 py-3 text-left font-medium text-text-muted">
                    문항 수
                  </th>
                  <td className="px-4 py-3 font-semibold tabular-nums">
                    {info.examInfo.questions}
                  </td>
                </tr>
                <tr>
                  <th className="bg-bg-elevated px-4 py-3 text-left font-medium text-text-muted">
                    시험 시간
                  </th>
                  <td className="px-4 py-3 font-semibold tabular-nums">
                    {info.examInfo.duration}
                  </td>
                </tr>
                <tr>
                  <th className="bg-bg-elevated px-4 py-3 text-left font-medium text-text-muted">
                    합격 기준
                  </th>
                  <td className="px-4 py-3 font-semibold">{info.examInfo.passLine}</td>
                </tr>
                <tr>
                  <th className="bg-bg-elevated px-4 py-3 text-left font-medium text-text-muted">
                    주관 기관
                  </th>
                  <td className="px-4 py-3 font-semibold">{info.examInfo.organizer}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </Container>
      </section>

      {/* 출제 영역 */}
      <section className="border-b border-border">
        <Container size="narrow" className="py-16">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            {info.shortKeyword} CBT 출제 영역
          </h2>
          <p className="mt-3 text-sm text-text-muted">
            CBT 모의고사도 같은 비중으로 자동 출제됩니다.
          </p>
          <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
            {info.subjects.map((s) => (
              <div
                key={s.name}
                className="rounded-xl border border-border bg-surface p-5"
              >
                <h3 className="text-base font-bold">{s.name}</h3>
                <p className="mt-2 text-sm text-text-muted leading-relaxed">
                  {s.description}
                </p>
              </div>
            ))}
          </div>
        </Container>
      </section>

      {/* 학습 팁 */}
      <section className="border-b border-border">
        <Container size="narrow" className="py-16">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            {info.shortKeyword} CBT 합격 학습 팁
          </h2>
          <p className="mt-4 text-base leading-relaxed text-text-muted">{info.studyTip}</p>
          <div className="mt-8">
            <Link
              href={`/mock-exams?cert=${certKey}`}
              className="btn-glow inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02]"
            >
              {info.shortKeyword} CBT 모의고사 시작 →
            </Link>
          </div>
        </Container>
      </section>

      {/* 관련 블로그 */}
      {relatedPosts.length > 0 && (
        <section className="border-b border-border">
          <Container size="narrow" className="py-16">
            <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
              {info.shortKeyword} 시험 준비 가이드
            </h2>
            <p className="mt-3 text-sm text-text-muted">
              {info.shortKeyword} 합격에 필요한 핵심 개념과 학습 전략 정리.
            </p>
            <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
              {relatedPosts.map((post) => (
                <Link
                  key={post.slug}
                  href={`/blog/${post.slug}`}
                  className="group rounded-xl border border-border bg-surface p-5 transition-all hover:border-primary/40"
                >
                  <p className="text-xs text-text-muted">{post.category}</p>
                  <h3 className="mt-2 text-base font-semibold leading-snug line-clamp-2 group-hover:text-primary">
                    {post.title}
                  </h3>
                  <p className="mt-2 text-sm text-text-muted line-clamp-2">
                    {post.description}
                  </p>
                  <span className="mt-4 inline-block text-xs font-medium text-primary">
                    읽어보기 →
                  </span>
                </Link>
              ))}
            </div>
          </Container>
        </section>
      )}

      {/* FAQ */}
      <section className="border-b border-border">
        <Container size="narrow" className="py-16">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            {info.shortKeyword} CBT 자주 묻는 질문
          </h2>
          <div className="mt-8 space-y-4">
            {info.faqs.map((f, i) => (
              <details
                key={i}
                className="group rounded-xl border border-border bg-surface p-5 transition-colors hover:border-primary/30"
              >
                <summary className="flex cursor-pointer items-center justify-between text-base font-semibold">
                  {f.q}
                  <span className="ml-4 text-text-muted transition-transform group-open:rotate-180">
                    ▾
                  </span>
                </summary>
                <p className="mt-3 text-sm leading-relaxed text-text-muted">{f.a}</p>
              </details>
            ))}
          </div>
        </Container>
      </section>

      {/* 다른 자격증 CBT 둘러보기 */}
      <section className="border-b border-border">
        <Container size="narrow" className="py-16">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            다른 자격증 CBT 모의고사
          </h2>
          <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3">
            {CERT_LIST.filter((c) => c.key !== certKey).map((c) => {
              const otherSlug = slugFromCert(c.key);
              return (
                <Link
                  key={c.key}
                  href={`/cbt-mock-exam/${otherSlug}`}
                  className={`rounded-lg border border-border bg-surface p-4 text-center transition-all hover:scale-[1.02] ${c.tailwind.borderHover}`}
                >
                  <span className={`mx-auto mb-2 block h-2 w-2 rounded-full ${c.tailwind.dot}`} />
                  <p className="text-sm font-semibold">{c.label} CBT</p>
                </Link>
              );
            })}
          </div>
        </Container>
      </section>

      {/* CTA */}
      <section>
        <Container size="default" className="py-20 text-center">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            지금 {info.shortKeyword} CBT 모의고사 시작
          </h2>
          <p className="mt-3 text-sm text-text-muted">
            Google 로그인 한 번 · 모든 기능 무료
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link
              href={`/mock-exams?cert=${certKey}`}
              className="btn-glow inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-semibold text-primary-fg transition-all hover:bg-primary-hover hover:scale-[1.02]"
            >
              {info.shortKeyword} CBT 모의고사 풀기 →
            </Link>
          </div>
        </Container>
      </section>
    </main>
  );
}
