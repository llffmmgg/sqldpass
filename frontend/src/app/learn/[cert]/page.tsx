import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import {
  getPublicCategoriesByCert,
  getPublicCerts,
  type CertSlug,
} from "@/lib/publicApi";

const CERT_META: Record<CertSlug, { title: string; description: string }> = {
  sqld: {
    title: "SQLD 기출문제 · 과목별 해설",
    description:
      "SQL 개발자(SQLD) 자격증의 과목별 기출 유형과 해설을 한 곳에서. 1과목 데이터 모델링, 2과목 SQL 기본/활용.",
  },
  engineer: {
    title: "정보처리기사 실기 기출문제 · 카테고리별 해설",
    description:
      "정보처리기사 실기 카테고리별 기출 유형 — C언어, Java, Python, SQL, 소프트웨어 설계, 데이터베이스, 네트워크/OS, 보안, 신기술 동향.",
  },
  "computer-literacy-1": {
    title: "컴퓨터활용능력 1급 필기 기출문제 · 과목별 해설",
    description:
      "컴퓨터활용능력 1급 필기 — 컴퓨터 일반, 스프레드시트 일반, 데이터베이스 일반 60문항 4지선다 기출 유형과 해설.",
  },
  "computer-literacy-2": {
    title: "컴퓨터활용능력 2급 필기 기출문제 · 과목별 해설",
    description:
      "컴퓨터활용능력 2급 필기 — 컴퓨터 일반, 스프레드시트 일반 40문항 4지선다 기출 유형과 해설.",
  },
  "engineer-written": {
    title: "정보처리기사 필기 기출문제 · 과목별 해설",
    description:
      "정보처리기사 필기 — 소프트웨어 설계, 소프트웨어 개발, 데이터베이스 구축, 프로그래밍 언어 활용, 정보시스템 구축 관리 100문항 4지선다 기출 유형과 해설.",
  },
};

export async function generateStaticParams() {
  try {
    const certs = await getPublicCerts();
    return certs.map((c) => ({ cert: c.slug }));
  } catch {
    return [];
  }
}

export async function generateMetadata(
  { params }: { params: Promise<{ cert: string }> },
): Promise<Metadata> {
  const { cert } = await params;
  const meta = CERT_META[cert as CertSlug];
  if (!meta) return { title: "자격증을 찾을 수 없습니다" };
  return {
    title: meta.title,
    description: meta.description,
    alternates: { canonical: `https://www.sqldpass.com/learn/${cert}` },
    openGraph: {
      title: `${meta.title} | SQLD Pass`,
      description: meta.description,
      url: `https://www.sqldpass.com/learn/${cert}`,
    },
  };
}

export default async function CertPage(
  { params }: { params: Promise<{ cert: string }> },
) {
  const { cert } = await params;
  if (!(cert in CERT_META)) notFound();

  let categories: Awaited<ReturnType<typeof getPublicCategoriesByCert>> = [];
  try {
    categories = await getPublicCategoriesByCert(cert);
  } catch {
    /* 실패 시 빈 */
  }

  const meta = CERT_META[cert as CertSlug];
  const certDisplayName: Record<string, string> = {
    sqld: "SQLD",
    engineer: "정처기 실기",
    "computer-literacy-1": "컴활 1급 필기",
    "computer-literacy-2": "컴활 2급 필기",
    "engineer-written": "정처기 필기",
  };
  const certName = certDisplayName[cert] ?? cert;

  // BreadcrumbList JSON-LD
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
    ],
  };

  // ItemList JSON-LD
  const itemListLd = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: meta.title,
    itemListElement: categories.map((cat, i) => ({
      "@type": "ListItem",
      position: i + 1,
      name: cat.name,
      url: `https://www.sqldpass.com/learn/${cert}/${cat.slug}`,
    })),
  };

  return (
    <main className="mx-auto max-w-5xl px-4 py-16 sm:px-6 lg:px-8">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(itemListLd) }}
      />

      <nav className="text-sm text-muted">
        <Link href="/learn" className="hover:text-foreground">
          자격증
        </Link>
        <span className="mx-2">/</span>
        <span className="text-foreground">
          {certName}
        </span>
      </nav>

      <header className="mt-4 mb-12">
        <h1 className="text-3xl font-bold sm:text-4xl">{meta.title}</h1>
        <p className="mt-3 max-w-2xl text-muted">{meta.description}</p>
      </header>

      {categories.length === 0 ? (
        <p className="text-muted">카테고리를 불러올 수 없습니다.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              href={`/learn/${cert}/${cat.slug}`}
              className="group rounded-lg border border-border bg-surface p-5 transition-all hover:scale-[1.01] active:scale-[0.99] hover:border-amber-500/30 hover:shadow-[0_0_18px_var(--glow)]"
            >
              <div className="flex items-center justify-between">
                <p className="text-xs text-muted">{cat.parentName}</p>
                <span className="font-mono text-xs text-muted">
                  {cat.questionCount}문제
                </span>
              </div>
              <h3 className="mt-2 text-base font-semibold">{cat.name}</h3>
              <p className="mt-3 text-xs text-amber-300 group-hover:text-amber-200">
                기출 보기 →
              </p>
            </Link>
          ))}
        </div>
      )}

      <section className="mt-16 rounded-xl border border-border bg-surface/50 p-6">
        <h2 className="text-lg font-semibold">
          {certName} 모의고사로 연습하세요
        </h2>
        <p className="mt-2 text-sm text-muted">
          매번 새로 추가되는 실전형 {cert === "engineer" ? "20" : cert === "sqld" ? "50" : "40"}문항 세트.
          로그인하고 바로 시작.
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
