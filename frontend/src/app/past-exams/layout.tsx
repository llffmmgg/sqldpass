import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "기출 복원 | 무료 CBT | 문어CBT",
  description:
    "SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 기출 복원 문제를 자격증별로 확인하고 로그인 후 채점과 해설까지 이어서 볼 수 있는 무료 CBT 페이지입니다.",
  alternates: {
    canonical: "https://www.sqldpass.com/past-exams",
  },
  openGraph: {
    title: "기출 복원 | 무료 CBT | 문어CBT",
    description:
      "SQLD, 정보처리기사, 컴퓨터활용능력, ADsP 기출 복원 문제를 자격증별로 확인할 수 있는 무료 CBT 페이지입니다.",
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

export default function PastExamsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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
