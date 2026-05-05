import type { Metadata } from "next";
import SolveClient from "./SolveClient";

const TITLE = "문제 풀기 — SQLD·정처기·컴활·ADsP 무료 CBT";
const DESCRIPTION =
  "과목별로 무한 문제를 풀어보세요. SQLD, 정보처리기사 필기·실기, 컴퓨터활용능력 1·2급, ADsP 기출 변형 문제를 자동 채점·해설과 함께 무료로 제공합니다.";

export const metadata: Metadata = {
  title: TITLE,
  description: DESCRIPTION,
  alternates: { canonical: "/solve" },
  openGraph: {
    type: "website",
    url: "/solve",
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
  { key: "SQLD", label: "SQLD", desc: "데이터 모델링·SQL 활용 핵심 개념을 4지선다로" },
  { key: "ENGINEER_INFO_WRITTEN", label: "정처기 필기", desc: "5과목 통합 4지선다 — 알고리즘·DB·네트워크·운영체제" },
  { key: "ENGINEER_INFO_PRACTICAL", label: "정처기 실기", desc: "단답형·서술형 — 자동 채점 + 키워드 해설" },
  { key: "COMPUTER_ACTIVE_1", label: "컴활 1급 필기", desc: "컴퓨터일반·스프레드시트·데이터베이스 3과목" },
  { key: "COMPUTER_ACTIVE_2", label: "컴활 2급 필기", desc: "컴퓨터일반·스프레드시트 2과목" },
  { key: "ADSP", label: "ADsP", desc: "데이터 이해·분석 기획·분석 3과목" },
] as const;

export default function SolvePage() {
  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: "https://www.sqldpass.com" },
      { "@type": "ListItem", position: 2, name: "문제 풀기", item: "https://www.sqldpass.com/solve" },
    ],
  };

  const webAppLd = {
    "@context": "https://schema.org",
    "@type": "WebApplication",
    name: "문어CBT 문제 풀기",
    url: "https://www.sqldpass.com/solve",
    applicationCategory: "EducationalApplication",
    operatingSystem: "Web",
    inLanguage: "ko-KR",
    description: DESCRIPTION,
    offers: { "@type": "Offer", price: "0", priceCurrency: "KRW" },
  };

  return (
    <main className="min-h-screen bg-bg text-text">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbLd) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(webAppLd) }}
      />

      {/* SEO 전용 — 화면에는 안 보이고 크롤러·스크린리더에만 노출. */}
      <div className="sr-only">
        <h1>과목별 문제 풀기</h1>
        <p>
          자격증과 과목을 고른 뒤 10문제 한 세트를 풀어보세요. 매 세트마다 다른 랜덤 문제로 무한 반복할 수 있고, 문제마다 즉시 해설과 정답을 확인합니다.
          오답 노트와 정답률 통계는 로그인 후 자동으로 저장됩니다.
        </p>
        <ul>
          {CERT_INTRO.map((c) => (
            <li key={c.key}>
              <span>{c.label}</span> <span>{c.desc}</span>
            </li>
          ))}
        </ul>
      </div>

      <SolveClient />
    </main>
  );
}
