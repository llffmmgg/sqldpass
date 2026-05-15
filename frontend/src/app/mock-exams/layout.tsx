import type { Metadata } from "next";

const SITE_URL = "https://www.sqldpass.com";
const OG_IMAGE = {
  url: `${SITE_URL}/opengraph-image`,
  width: 1200,
  height: 630,
  alt: "문어CBT 무료 CBT 모의고사",
};

export const metadata: Metadata = {
  title: "무료 CBT 모의고사 | SQLD·정처기·컴활·ADsP 실전 타이머",
  description:
    "SQLD CBT, 정처기 필기·실기 CBT, 컴활 1·2급 CBT, ADsP CBT 모의고사를 무료로. 실전 타이머·자동 채점·오답 누적까지, 회차별 실력 추적이 가능한 자격증 CBT 모의고사 플랫폼.",
  keywords: [
    "CBT 모의고사",
    "무료 CBT 모의고사",
    "SQLD CBT",
    "SQLD 모의고사",
    "정처기 CBT",
    "정처기 필기 CBT",
    "정처기 실기 CBT",
    "정보처리기사 CBT",
    "정보처리기사 필기 CBT",
    "정보처리기사 실기 CBT",
    "컴활 CBT",
    "컴활 1급 CBT",
    "컴활 2급 CBT",
    "컴퓨터활용능력 CBT",
    "컴퓨터활용능력 1급 CBT",
    "컴퓨터활용능력 2급 CBT",
    "ADsP CBT",
    "ADsP 모의고사",
    "데이터분석 준전문가 CBT",
  ],
  alternates: {
    canonical: `${SITE_URL}/mock-exams`,
  },
  openGraph: {
    title: "무료 CBT 모의고사 | SQLD·정처기·컴활·ADsP | 문어CBT",
    description:
      "SQLD CBT, 정처기 CBT, 컴활 CBT, ADsP CBT 모의고사를 실전 타이머와 함께 무료로.",
    url: `${SITE_URL}/mock-exams`,
    type: "website",
    siteName: "문어CBT",
    locale: "ko_KR",
    images: [OG_IMAGE],
  },
  twitter: {
    card: "summary_large_image",
    title: "무료 CBT 모의고사 | SQLD·정처기·컴활·ADsP",
    description:
      "SQLD CBT, 정처기 CBT, 컴활 CBT, ADsP CBT 모의고사 무료 · 실전 타이머 · 자동 채점.",
    images: [OG_IMAGE.url],
  },
};

export default function MockExamsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
