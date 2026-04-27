"use client";

import Link from "next/link";
import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";

import PostMarkdown from "@/components/PostMarkdown";
import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import {
  adminApprovePost,
  adminDeleteComment,
  adminDeletePost,
  adminEditPost,
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
  // 본문 표시 모드: preview(렌더) / raw(원본) / edit(편집)
  const [view, setView] = useState<"preview" | "raw" | "edit">("preview");
  // 편집 모드 임시 버퍼 (저장 시 백엔드 반영)
  const [draftTitle, setDraftTitle] = useState("");
  const [draftContent, setDraftContent] = useState("");

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

  function startEdit() {
    if (!post) return;
    setDraftTitle(post.title);
    setDraftContent(post.content);
    setView("edit");
  }

  async function saveEdit() {
    if (!draftTitle.trim() || !draftContent.trim()) {
      alert("제목과 본문을 모두 입력해주세요.");
      return;
    }
    setBusy(true);
    try {
      await adminEditPost(postId, {
        title: draftTitle.trim(),
        content: draftContent.trim(),
      });
      // 로컬 state 동기화
      setPost((prev) =>
        prev ? { ...prev, title: draftTitle.trim(), content: draftContent.trim() } : prev,
      );
      setView("preview");
    } catch (e) {
      alert(e instanceof Error ? e.message : "수정에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  }

  function cancelEdit() {
    setView("preview");
  }

  async function handleDeleteComment(commentId: number) {
    if (!confirm("이 댓글을 삭제할까요?")) return;
    try {
      await adminDeleteComment(commentId);
      setPost((prev) =>
        prev ? { ...prev, comments: prev.comments.filter((c) => c.id !== commentId) } : prev,
      );
    } catch (e) {
      alert(e instanceof Error ? e.message : "댓글 삭제에 실패했습니다.");
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
    <div className="space-y-6 lg:grid lg:grid-cols-[minmax(0,1fr)_300px] lg:gap-6 lg:space-y-0">
      <div className="space-y-6">
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
            {view === "preview" && "사용자가 보는 모습 (마크다운 렌더링)"}
            {view === "raw" && "원본 마크다운 텍스트"}
            {view === "edit" && "편집 모드 — 저장하면 게시판에 반영됩니다"}
          </span>
          <div className="flex items-center gap-1 rounded-md border border-border p-0.5 text-xs">
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
            {view !== "edit" ? (
              <button
                type="button"
                onClick={startEdit}
                className="rounded border-l border-border px-2 py-1 text-muted hover:text-foreground"
              >
                ✏️ 편집
              </button>
            ) : (
              <span className="rounded bg-amber-500/10 px-2 py-1 font-semibold text-amber-400">
                편집 중
              </span>
            )}
          </div>
        </div>

        <div className="mt-4">
          {view === "preview" && <PostMarkdown content={post.content} />}
          {view === "raw" && (
            <pre className="overflow-x-auto whitespace-pre-wrap rounded-md border border-border bg-background/40 p-4 text-xs leading-relaxed text-muted">
              {post.content}
            </pre>
          )}
          {view === "edit" && (
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-muted">제목</label>
                <input
                  type="text"
                  value={draftTitle}
                  maxLength={120}
                  onChange={(e) => setDraftTitle(e.target.value)}
                  className="mt-1 w-full rounded-md border border-border bg-background/40 px-3 py-2 text-sm focus:border-primary focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-muted">본문 (마크다운)</label>
                <textarea
                  value={draftContent}
                  rows={20}
                  onChange={(e) => setDraftContent(e.target.value)}
                  className="mt-1 w-full resize-y rounded-md border border-border bg-background/40 px-3 py-2 text-sm font-mono leading-relaxed focus:border-primary focus:outline-none"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={cancelEdit}
                  disabled={busy}
                  className="rounded-md border border-border px-3 py-1.5 text-xs text-muted hover:text-foreground disabled:opacity-50"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={saveEdit}
                  disabled={busy}
                  className="rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-primary-fg hover:bg-primary-hover disabled:opacity-50"
                >
                  💾 저장
                </button>
              </div>
            </div>
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

      {/* 댓글 관리 — 어드민이 임의 삭제 가능 */}
      <section className="rounded-xl border border-border bg-surface p-6">
        <div className="flex items-baseline justify-between">
          <h2 className="text-sm font-semibold">댓글 관리 ({post.comments.length})</h2>
          <span className="text-xs text-muted">어드민은 모든 댓글을 삭제할 수 있어요</span>
        </div>
        {post.comments.length === 0 ? (
          <p className="mt-4 text-center text-sm text-muted">아직 댓글이 없습니다.</p>
        ) : (
          <ul className="mt-4 space-y-2">
            {post.comments.map((c) => (
              <li
                key={c.id}
                className="rounded-lg border border-border bg-background/40 px-4 py-3"
              >
                <div className="flex items-center justify-between gap-2 text-xs text-muted tabular-nums">
                  <span className="font-medium text-foreground">{c.authorNickname}</span>
                  <div className="flex items-center gap-2">
                    <span>{new Date(c.createdAt).toLocaleString("ko-KR")}</span>
                    <button
                      type="button"
                      onClick={() => handleDeleteComment(c.id)}
                      className="rounded border border-red-500/40 bg-red-500/10 px-2 py-0.5 text-[11px] text-red-400 hover:bg-red-500/15"
                    >
                      삭제
                    </button>
                  </div>
                </div>
                <p className="mt-1.5 whitespace-pre-wrap text-sm text-foreground">{c.content}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
      </div>

      {/* 우측 사이드 — 복붙용 스니펫 */}
      <aside className="lg:sticky lg:top-4 lg:self-start lg:max-h-[calc(100vh-2rem)] lg:overflow-y-auto">
        <CopySnippets />
      </aside>
    </div>
  );
}

/** 어드민이 후기에 자주 추가하는 마크다운 스니펫 — 클릭 시 클립보드 복사. */
function CopySnippets() {
  const snippets = [
    {
      title: "쿠팡 SQLD 교재 링크",
      hint: "본문 자료 소개 섹션에 한 번 삽입",
      text: "[SQLD 교재 쿠팡에서 보기](https://link.coupang.com/a/exyFNq)",
    },
    {
      title: "쿠팡 파트너스 면책 문구",
      hint: "글 맨 하단에 한 번",
      text: "이 포스팅은 쿠팡 파트너스 활동의 일환으로, 이에 따른 일정액의 수수료를 제공받습니다.",
    },
  ];

  return (
    <div className="space-y-3 rounded-xl border border-border bg-surface p-4">
      <div>
        <h3 className="text-sm font-semibold">📋 자주 쓰는 스니펫</h3>
        <p className="mt-1 text-[11px] text-muted">클릭하면 클립보드에 복사됩니다.</p>
      </div>
      {snippets.map((s) => (
        <SnippetCard key={s.title} {...s} />
      ))}
    </div>
  );
}

function SnippetCard({ title, hint, text }: { title: string; hint: string; text: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1200);
    } catch {
      alert("복사에 실패했습니다. 직접 선택해서 복사해주세요.");
    }
  }

  return (
    <button
      type="button"
      onClick={copy}
      className="block w-full rounded-lg border border-border bg-background/40 p-3 text-left transition-colors hover:border-primary/40"
    >
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-foreground">{title}</span>
        <span
          className={`text-[11px] ${copied ? "text-primary" : "text-muted"}`}
        >
          {copied ? "✓ 복사됨" : "복사"}
        </span>
      </div>
      <p className="mt-1 text-[10px] text-muted">{hint}</p>
      <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all rounded border border-border bg-background/60 p-2 text-[11px] leading-relaxed text-muted">
        {text}
      </pre>
    </button>
  );
}
