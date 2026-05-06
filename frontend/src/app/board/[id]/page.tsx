import Link from "next/link";

import { Container } from "@/components/ui";
import { getPublicPost } from "@/lib/publicApi";
import type { PostDetail } from "@/lib/api";
import BoardPostClient from "./BoardPostClient";

export default async function PostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const postId = Number(id);

  // SSR keeps published posts indexable; the client fetch owns the counted view.
  let initial: PostDetail | null = null;
  try {
    initial = (await getPublicPost(postId)) as unknown as PostDetail;
  } catch {
    initial = null;
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-12">
        <Link href="/board" className="text-sm text-text-muted hover:text-text">
          ← 게시판
        </Link>

        <article className="mt-4 rounded-xl border border-border bg-surface p-6">
          <BoardPostClient postId={postId} initial={initial} />
        </article>
      </Container>
    </main>
  );
}
