import type { Metadata } from "next";
import MyLearningClient from "./MyLearningClient";

const TITLE = "내 학습 — 대시보드와 오답노트";
const DESCRIPTION =
  "최근 풀이 활동, 합격 확률, 자격증별 성취도, 그리고 오답노트와 북마크를 한 화면에서 확인하세요.";

export const metadata: Metadata = {
  title: TITLE,
  description: DESCRIPTION,
  alternates: { canonical: "/my-learning" },
  robots: { index: false, follow: false },
  openGraph: {
    type: "website",
    url: "/my-learning",
    title: TITLE,
    description: DESCRIPTION,
    siteName: "문어CBT",
    locale: "ko_KR",
  },
};

export default function MyLearningPage() {
  return <MyLearningClient />;
}
