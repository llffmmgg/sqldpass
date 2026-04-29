import type { Metadata } from "next";

const SITE_URL = "https://www.sqldpass.com";

export const metadata: Metadata = {
  title: "문제 풀기 · 과목별 CBT 연습",
  description:
    "SQLD, 정보처리기사, 컴퓨터활용능력 1급 과목별 문제를 바로 풀어보세요. 오답 자동 복습과 실력 추적까지.",
  alternates: {
    canonical: `${SITE_URL}/solve`,
  },
  openGraph: {
    title: "문제 풀기 · 과목별 CBT 연습 | 문어CBT",
    description:
      "자격증 과목별 문제를 바로 풀어보세요. 오답 자동 복습과 실력 추적까지.",
    url: `${SITE_URL}/solve`,
  },
};

export default function SolveLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
