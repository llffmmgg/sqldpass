import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "풀이 히스토리",
  robots: { index: false, follow: false },
};

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
