"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 게시글 fetch 후 setState */

import Link from "next/link";
import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";

import PostMarkdown from "@/components/PostMarkdown";
import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import {
  adminApprovePost,
  adminDeletePost,
  adminGetPost,
  type PostDetail,
} from "@/lib/api";

export default function AdminPostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const postId = Number(id);
  const router = useRouter();

  const [post, setPost] = useState<PostDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // 본문을 사용자가 보는 렌더링(preview) 으로 볼지, 원본 markdown 텍스트로 볼지
  const [view, setView] = useState<"preview" | "raw">("preview");

  useEffect(() => {
    setError(null);
    adminGetPost(postId)
      .then(setPost)
      .catch((e) => setError(e instanceof Error ? e.message : "게시글을 불러올 수 없습니다."));
  }, [postId]);

  async function handleApprove() {
    if (!confirm("이 후기를 승인할까요? 게시판에 즉시 노출됩니다.")) return;
    setBusy(true);
    try {
      await adminApprovePost(postId);
      router.push("/admin/posts");
    } catch (e) {
      alert(e instanceof Error ? e.message : "승인에 실패했습니다.");
      setBusy(false);
    }
  }

  async function handleDelete() {
    if (!confirm("이 게시글을 삭제할까요? (반려 처리, 복구 불가)")) return;
    setBusy(true);
    try {
      await adminDeletePost(postId);
      router.push("/admin/posts");
    } catch (e) {
      alert(e instanceof Error ? e.message : "삭제에 실패했습니다.");
      setBusy(false);
    }
  }

  if (error) {
    return (
      <div className="space-y-3">
        <Link href="/admin/posts" className="text-sm text-muted hover:text-foreground">
          ← 후기 승인
        </Link>
        <p className="text-sm text-red-400">{error}</p>
      </div>
    );
  }

  if (!post) {
    return <p className="text-sm text-muted">불러오는 중…</p>;
  }

  const cert: CertKey | null = certFromExamType(post.cert);
  const token = cert ? CERT_TOKENS[cert] : null;

  return (
    <div className="max-w-3xl space-y-6">
      <Link href="/admin/posts" className="text-sm text-muted hover:text-foreground">
        ← 후기 승인
      </Link>

      <article className="rounded-xl border border-border bg-surface p-6">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          {token && (
            <span
              className={`rounded-md border px-2 py-0.5 text-[11px] font-bold ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.text}`}
            >
              {token.label}
            </span>
          )}
          <span
            className={`rounded-md border px-2 py-0.5 text-[11px] font-bold ${
              post.status === "PENDING"
                ? "border-amber-500/30 bg-amber-500/10 text-amber-400"
                : "border-green-500/30 bg-green-500/10 text-green-400"
            }`}
          >
            {post.status}
          </span>
        </div>

        <h1 className="mt-3 text-2xl font-bold tracking-tight">{post.title}</h1>

        <div className="mt-3 text-xs text-muted tabular-nums">
          {post.authorNickname} · {new Date(post.createdAt).toLocaleString("ko-KR")}
        </div>

        <div className="mt-5 flex items-center justify-between border-t border-border pt-5">
          <span className="text-xs text-muted">
            {view === "preview"
              ? "사용자가 보는 모습 (마크다운 렌더링)"
              : "원본 마크다운 텍스트"}
          </span>
          <div className="flex gap-1 rounded-md border border-border p-0.5 text-xs">
            <button
              type="button"
              onClick={() => setView("preview")}
              className={`rounded px-2 py-1 transition-colors ${
                view === "preview"
                  ? "bg-primary/10 font-semibold text-primary"
                  : "text-muted hover:text-foreground"
              }`}
            >
              미리보기
            </button>
            <button
              type="button"
              onClick={() => setView("raw")}
              className={`rounded px-2 py-1 transition-colors ${
                view === "raw"
                  ? "bg-primary/10 font-semibold text-primary"
                  : "text-muted hover:text-foreground"
              }`}
            >
              원본
            </button>
          </div>
        </div>

        <div className="mt-4">
          {view === "preview" ? (
            <PostMarkdown content={post.content} />
          ) : (
            <pre className="overflow-x-auto whitespace-pre-wrap rounded-md border border-border bg-background/40 p-4 text-xs leading-relaxed text-muted">
              {post.content}
            </pre>
          )}
        </div>
      </article>

      <div className="flex justify-end gap-2">
        {post.status === "PENDING" && (
          <button
            type="button"
            onClick={handleApprove}
            disabled={busy}
            className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-fg hover:bg-primary-hover disabled:opacity-50"
          >
            ✅ 승인
          </button>
        )}
        <button
          type="button"
          onClick={handleDelete}
          disabled={busy}
          className="inline-flex items-center rounded-md border border-red-500/40 bg-red-500/10 px-4 py-2 text-sm font-semibold text-red-400 hover:bg-red-500/15 disabled:opacity-50"
        >
          🗑 반려/삭제
        </button>
      </div>
    </div>
  );
}
