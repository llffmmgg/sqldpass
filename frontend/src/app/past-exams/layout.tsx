import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "기출 복원 · 무료 CBT — 문어CBT",
  description:
    "SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 기출 복원 회차를 실제 시험 시간과 동일한 환경으로 무료 응시. 로그인 없이 해설과 정답까지 바로 확인.",
  alternates: {
    canonical: "https://www.sqldpass.com/past-exams",
  },
  openGraph: {
    title: "기출 복원 · 무료 CBT — 문어CBT",
    description:
      "SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 기출 복원 회차를 실제 시험 시간과 동일한 환경으로 무료 응시.",
    url: "https://www.sqldpass.com/past-exams",
    type: "website",
  },
};

const BREADCRUMB_LD = {
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  itemListElement: [
    {
      "@type": "ListItem",
      position: 1,
      name: "홈",
      item: "https://www.sqldpass.com/",
    },
    {
      "@type": "ListItem",
      position: 2,
      name: "기출 복원",
      item: "https://www.sqldpass.com/past-exams",
    },
  ],
};

export default function PastExamsLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(BREADCRUMB_LD) }}
      />
      {children}
    </>
  );
}
