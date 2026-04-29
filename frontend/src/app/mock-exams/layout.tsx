import type { Metadata } from "next";

const SITE_URL = "https://www.sqldpass.com";

export const metadata: Metadata = {
  title: "모의고사 · 자격증별 실전 CBT",
  description:
    "SQLD, 정보처리기사 실기·필기, 컴퓨터활용능력 1·2급, ADsP 실전 모의고사. 매번 AI로 새로 구성되는 CBT 환경에서 실력을 점검하세요.",
  keywords: [
    "SQLD 모의고사",
    "정처기 실기 모의고사",
    "정처기 필기 모의고사",
    "컴활 1급 모의고사",
    "컴활 2급 모의고사",
    "ADsP 모의고사",
    "데이터분석 준전문가 모의고사",
    "CBT 모의고사",
    "무료 모의고사",
  ],
  alternates: {
    canonical: `${SITE_URL}/mock-exams`,
  },
  openGraph: {
    title: "모의고사 · 자격증별 실전 CBT | 문어CBT",
    description:
      "SQLD, 정처기 실기·필기, 컴활 1·2급, ADsP 실전 모의고사. AI로 매번 새롭게 출제되는 CBT.",
    url: `${SITE_URL}/mock-exams`,
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "모의고사 · 자격증별 실전 CBT | 문어CBT",
    description:
      "SQLD, 정처기 실기·필기, 컴활 1·2급, ADsP 실전 모의고사.",
  },
};

export default function MockExamsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
