import type { Metadata } from "next";

const SITE_URL = "https://www.sqldpass.com";

export const metadata: Metadata = {
  title: "모의고사 · 자격증별 실전 CBT",
  description:
    "SQLD, 정보처리기사, 컴퓨터활용능력 1급 등 자격증별 실전 모의고사. 매번 새로 구성되는 CBT 환경에서 실력을 점검하세요.",
  alternates: {
    canonical: `${SITE_URL}/mock-exams`,
  },
  openGraph: {
    title: "모의고사 · 자격증별 실전 CBT | sqldpass",
    description:
      "SQLD, 정보처리기사, 컴퓨터활용능력 1급 등 자격증별 실전 모의고사.",
    url: `${SITE_URL}/mock-exams`,
  },
};

export default function MockExamsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
