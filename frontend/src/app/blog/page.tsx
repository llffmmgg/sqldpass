import type { Metadata } from "next";
import { getAllPosts, getAllCategories } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";
import BlogList from "./BlogList";

export const metadata: Metadata = {
  title: "자격증 시험 준비 블로그",
  description:
    "SQLD, 정처기 필기·실기, 컴활 1·2급, ADsP 시험 준비에 필요한 학습 전략, 출제 경향, CBT 활용법을 정리한 블로그입니다.",
  alternates: { canonical: "https://www.sqldpass.com/blog" },
  openGraph: {
    title: "자격증 시험 준비 블로그 | 문어CBT",
    description:
      "SQLD, 정처기, 컴활, ADsP 시험 준비 꿀팁과 학습 전략을 공유합니다.",
    url: "https://www.sqldpass.com/blog",
  },
};

export default async function BlogPage() {
  const posts = getAllPosts();
  const categories = getAllCategories();
  let viewCounts: Record<string, number> = {};
  try {
    viewCounts = await getPublicBlogViews();
  } catch {
    /* 백엔드 미연결 시 무시 */
  }

  const recommendedPosts = [...posts].sort(() => Math.random() - 0.5).slice(0, 3);

  return (
    <BlogList
      posts={posts}
      categories={categories}
      viewCounts={viewCounts}
      recommendedPosts={recommendedPosts}
    />
  );
}
