import type { Metadata } from "next";
import { Container } from "@/components/ui";
import MockExamsClient from "./MockExamsClient";

const TITLE = "무료 CBT 모의고사 — SQLD·정처기·컴활·ADsP";
const DESCRIPTION =
  "SQLD, 정보처리기사 필기·실기, 컴퓨터활용능력 1·2급, ADsP 모의고사를 무료로. 실전 회차와 기출 변형 + 자동 채점·해설을 가입 없이 바로 풀어보세요.";

export const metadata: Metadata = {
  title: TITLE,
  description: DESCRIPTION,
  alternates: { canonical: "/mock-exams" },
  openGraph: {
    type: "website",
    url: "/mock-exams",
    title: TITLE,
    description: DESCRIPTION,
    siteName: "문어CBT",
    locale: "ko_KR",
  },
  twitter: {
    card: "summary_large_image",
    title: TITLE,
    description: DESCRIPTION,
  },
};

const CERT_INTRO = [
  { key: "SQLD", label: "SQLD CBT", desc: "50문항 90분 · 데이터 모델링·SQL 활용 핵심 기출 변형" },
  { key: "ENGINEER_INFO_WRITTEN", label: "정처기 필기 CBT", desc: "100문항 150분 · 5과목 통합 실전 회차" },
  { key: "ENGINEER_INFO_PRACTICAL", label: "정처기 실기 CBT", desc: "단답·서술형 자동 채점 + 키워드 기반 해설" },
  { key: "COMPUTER_ACTIVE_1", label: "컴활 1급 필기 CBT", desc: "60문항 60분 · 3과목 (컴퓨터일반·스프레드시트·데이터베이스)" },
  { key: "COMPUTER_ACTIVE_2", label: "컴활 2급 필기 CBT", desc: "40문항 40분 · 2과목 (컴퓨터일반·스프레드시트)" },
  { key: "ADSP", label: "ADsP CBT", desc: "50문항 90분 · 3과목 데이터분석 준전문가 실전 회차" },
] as const;

export default function MockExamsPage() {
  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: "https://www.sqldpass.com" },
      { "@type": "ListItem", position: 2, name: "모의고사", item: "https://www.sqldpass.com/mock-exams" },
    ],
  };

  const collectionLd = {
    "@context": "https://schema.org",
    "@type": "CollectionPage",
    name: TITLE,
    description: DESCRIPTION,
    url: "https://www.sqldpass.com/mock-exams",
    inLanguage: "ko-KR",
    isPartOf: {
      "@type": "WebSite",
      name: "문어CBT",
      url: "https://www.sqldpass.com",
    },
    about: CERT_INTRO.map((c) => ({ "@type": "Thing", name: c.label })),
  };

  return (
    <main className="min-h-screen bg-bg text-text">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(collectionLd) }}
      />

      <Container size="narrow" className="pt-16">
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">무료 CBT 모의고사</h1>
        <p className="mt-3 text-sm leading-relaxed text-text-muted">
          SQLD · 정보처리기사 필기/실기 · 컴퓨터활용능력 1·2급 · ADsP 모의고사를 무료로 제공합니다.
          실전과 동일한 타이머·자동 채점·해설로 매주 새로운 회차가 추가됩니다. 점수 기록과 오답 노트는 로그인 후 자동으로 저장됩니다.
        </p>

        <ul className="mt-6 grid grid-cols-1 gap-2 text-sm text-text-muted sm:grid-cols-2">
          {CERT_INTRO.map((c) => (
            <li key={c.key} className="rounded-lg border border-border bg-surface px-3 py-2">
              <span className="font-semibold text-text">{c.label}</span>
              <span className="ml-2 text-xs text-text-subtle">{c.desc}</span>
            </li>
          ))}
        </ul>
      </Container>

      <div className="mt-8">
        <MockExamsClient />
      </div>
    </main>
  );
}
