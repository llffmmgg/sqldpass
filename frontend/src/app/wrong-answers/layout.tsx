import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "오답 노트",
  description:
    "틀린 문제를 자동으로 모아 복습. 자격증별로 오답을 확인하고 다시 풀어보세요.",
  alternates: { canonical: "/wrong-answers" },
};

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
