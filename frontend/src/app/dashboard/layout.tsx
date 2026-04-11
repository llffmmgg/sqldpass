import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "대시보드",
  description:
    "나의 학습 현황을 한눈에. 풀이 통계, 과목별 정답률, 연속 학습 기록을 확인하세요.",
  alternates: { canonical: "/dashboard" },
};

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
