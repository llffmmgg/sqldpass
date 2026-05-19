import type { Metadata } from "next";
import MiniMockExamsClient from "./MiniMockExamsClient";

const TITLE = "미니 모의고사 — 5–10분 짧은 한 세트";
const DESCRIPTION =
  "SQLD, 정처기, 컴활, ADsP 의 정규 회차에서 과목 분포는 그대로 두고 분량만 축약한 미니 한 세트. 짧은 시간에 약점만 점검해 보세요.";

export const metadata: Metadata = {
  title: TITLE,
  description: DESCRIPTION,
  alternates: { canonical: "/mini-mock-exams" },
  openGraph: {
    type: "website",
    url: "/mini-mock-exams",
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

export default function MiniMockExamsPage() {
  return (
    <main className="min-h-screen bg-bg text-text">
      <MiniMockExamsClient />
    </main>
  );
}
