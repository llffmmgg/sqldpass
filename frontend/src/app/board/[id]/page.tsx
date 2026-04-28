import Link from "next/link";

import { Container } from "@/components/ui";
import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import { getPublicPost } from "@/lib/publicApi";
import { formatRelativeDate } from "@/lib/format";
import type { PostDetail } from "@/lib/api";
import BoardPostClient from "./BoardPostClient";

export default async function PostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const postId = Number(id);

  // SSR — PUBLISHED 게시글은 정적 HTML 로 본문/제목/메타가 노출되도록.
  // 작성자 본인의 PENDING 글은 SSR 에서 미수신 → 클라이언트 fallback 으로 처리.
  let initial: PostDetail | null = null;
  try {
    initial = (await getPublicPost(postId)) as unknown as PostDetail;
  } catch {
    initial = null;
  }

  const cert: CertKey | null = certFromExamType(initial?.cert ?? null);
  const token = cert ? CERT_TOKENS[cert] : null;

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="narrow" className="py-12">
        <Link href="/board" className="text-sm text-text-muted hover:text-text">
          ← 게시판
        </Link>

        <article className="mt-4 rounded-xl border border-border bg-surface p-6">
          <div className="flex flex-wrap items-center gap-2 text-xs">
            {token && (
              <span
                className={`rounded-md border px-2 py-0.5 text-[11px] font-bold ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.text}`}
              >
                {token.label}
              </span>
            )}
            {initial?.status === "PENDING" && (
              <span className="rounded-md border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[11px] font-bold text-amber-400">
                ⏳ 승인 대기 중 (운영자 검토 후 게시)
              </span>
            )}
          </div>

          {initial && (
            <>
              <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">
                {initial.title}
              </h1>
              <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-text-muted tabular-nums">
                <span>{initial.authorNickname}</span>
                <span aria-hidden>·</span>
                <span>{formatRelativeDate(initial.createdAt)}</span>
                <span aria-hidden>·</span>
                <span>조회 {initial.viewCount.toLocaleString()}</span>
              </div>
              <hr className="my-6 border-border" />
            </>
          )}

          <BoardPostClient postId={postId} initial={initial} />
        </article>
      </Container>
    </main>
  );
}
