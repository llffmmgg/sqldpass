import type { Metadata } from "next";
import { MDXRemote } from "next-mdx-remote/rsc";
import remarkGfm from "remark-gfm";
import { Container } from "@/components/ui";
import { getChangelogContent } from "@/lib/changelog";

export const metadata: Metadata = {
  title: "업데이트 내역",
  description: "sqldpass가 최근 어떻게 개선됐는지 기능 단위로 정리했어요.",
  alternates: { canonical: "https://www.sqldpass.com/changelog" },
  openGraph: {
    type: "website",
    title: "업데이트 내역 | sqldpass",
    description: "sqldpass가 최근 어떻게 개선됐는지 기능 단위로 정리했어요.",
    url: "https://www.sqldpass.com/changelog",
  },
};

export default function ChangelogPage() {
  const content = getChangelogContent();
  return (
    <Container size="narrow" className="py-12">
      <header className="mx-auto max-w-[44rem]">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">업데이트 내역</h1>
        <p className="mt-3 text-base text-text-muted">
          sqldpass가 최근 어떻게 바뀌었는지 기능 단위로 정리했어요.
        </p>
        <hr className="mt-8 border-border" />
      </header>

      <article className="prose-custom mt-10">
        <MDXRemote
          source={content}
          options={{ mdxOptions: { remarkPlugins: [remarkGfm] } }}
        />
      </article>
    </Container>
  );
}
