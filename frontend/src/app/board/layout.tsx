import type { Metadata } from "next";

const SITE_URL = "https://www.sqldpass.com";

export const metadata: Metadata = {
  title: "합격 후기 | SQLD·정처기·컴활·ADsP | 문어CBT",
  description:
    "문어CBT 합격 후기 게시판 — SQLD, 정보처리기사 필기·실기, 컴퓨터활용능력 1·2급, ADsP 합격자들의 실제 학습 기간, 사용 자료, 시험 팁을 확인하세요.",
  alternates: { canonical: `${SITE_URL}/board` },
  openGraph: {
    title: "합격 후기 | 문어CBT",
    description:
      "SQLD·정처기·컴활·ADsP 합격자들이 직접 남긴 학습 후기. 시험 준비 기간, 추천 자료, 합격 팁을 한눈에.",
    url: `${SITE_URL}/board`,
    type: "website",
  },
  twitter: {
    card: "summary",
    title: "합격 후기 | 문어CBT",
    description:
      "SQLD·정처기·컴활·ADsP 합격자들이 직접 남긴 학습 후기와 팁.",
  },
};

export default function BoardLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
