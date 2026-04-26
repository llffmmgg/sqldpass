import type { Metadata } from "next";
import { getAllPosts, getAllCategories, type BlogPostMeta } from "@/lib/blog";
import { getPublicBlogViews } from "@/lib/publicApi";
import { CERT_TOKENS, certFromExamType } from "@/lib/cert-tokens";
import {
  pastExamBlogDescription,
  pastExamBlogSlug,
  pastExamBlogTitle,
} from "@/lib/pastExamBlog";
import { loadPastExamListsByCert, flattenPastExamLists } from "@/lib/pastExamCatalog";
import BlogList from "./BlogList";

export const dynamic = "force-dynamic";

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
  const mdxPosts = getAllPosts();

  // 기출 복원 회차를 가상 BlogPostMeta 로 변환해 메인 리스트에 합류시킴.
  const listsByCert = await loadPastExamListsByCert().catch(() => null);
  const pastExamPosts: BlogPostMeta[] = listsByCert
    ? flattenPastExamLists(listsByCert).map((exam) => {
        const cert = certFromExamType(exam.examType);
        const category = cert ? CERT_TOKENS[cert].blogCategory : "일반";
        return {
          slug: `past-exam/${pastExamBlogSlug(exam)}`,
          title: pastExamBlogTitle(exam),
          description: pastExamBlogDescription(exam),
          date: exam.examDate ?? exam.createdAt.slice(0, 10),
          category,
          tags: ["기출 복원", category],
          readingTime: `${Math.max(5, Math.round(exam.totalQuestions / 4))}분`,
        };
      })
    : [];

  const posts: BlogPostMeta[] = [...mdxPosts, ...pastExamPosts].sort((a, b) =>
    a.date > b.date ? -1 : 1,
  );

  const baseCategories = getAllCategories();
  const pastExamCategoryCounts = new Map<string, number>();
  for (const p of pastExamPosts) {
    pastExamCategoryCounts.set(
      p.category,
      (pastExamCategoryCounts.get(p.category) ?? 0) + 1,
    );
  }
  const categoryMap = new Map(baseCategories.map((c) => [c.category, c.count]));
  for (const [cat, n] of pastExamCategoryCounts) {
    categoryMap.set(cat, (categoryMap.get(cat) ?? 0) + n);
  }
  const categories = Array.from(categoryMap.entries())
    .map(([category, count]) => ({ category, count }))
    .sort((a, b) => b.count - a.count);

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
