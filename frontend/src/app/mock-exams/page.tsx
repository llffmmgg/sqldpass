import type { Metadata } from "next";
import MockExamsClient from "./MockExamsClient";

// 이 페이지는 "자격증 모의고사 카탈로그" 거점. "X cbt" 단독 키워드는 /cbt-mock-exam/{cert} 가 담당하므로
// 여기서는 "CBT" 표현을 약화하고 "모의고사 모음·자격증별" 키워드를 강조한다.
const TITLE = "자격증 모의고사 모음 — SQLD·정처기·컴활·ADsP";
const DESCRIPTION =
  "SQLD, 정보처리기사 필기·실기, 컴퓨터활용능력 1·2급, ADsP 모의고사를 자격증별로 모았습니다. 실전 회차와 기출 변형 + 자동 채점·해설을 가입 없이 바로 풀어보세요.";

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

      {/* SEO 전용 — 화면에는 안 보이고 크롤러·스크린리더에만 노출. */}
      <div className="sr-only">
        <h1>자격증 모의고사 모음</h1>
        <p>
          SQLD · 정보처리기사 필기/실기 · 컴퓨터활용능력 1·2급 · ADsP 모의고사를 자격증별로 한 곳에 모았습니다.
          실전과 동일한 타이머·자동 채점·해설로 매주 새로운 회차가 추가됩니다. 점수 기록과 오답 노트는 로그인 후 자동으로 저장됩니다.
        </p>
        <ul>
          {CERT_INTRO.map((c) => (
            <li key={c.key}>
              <span>{c.label}</span> <span>{c.desc}</span>
            </li>
          ))}
        </ul>
      </div>

      <MockExamsClient />
    </main>
  );
}
