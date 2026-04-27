"use client";

import Link from "next/link";
import { useEffect, useRef, useState, use } from "react";
import { useRouter } from "next/navigation";

import ImageUploadButton from "@/components/ImageUploadButton";
import PostMarkdown from "@/components/PostMarkdown";
import { Button, Container } from "@/components/ui";
import {
  CERT_TOKENS,
  certFromExamType,
  type CertKey,
} from "@/lib/cert-tokens";
import {
  addComment,
  deleteComment,
  deletePost,
  getPost,
  type PostDetail,
} from "@/lib/api";
import { getNickname, isLoggedIn } from "@/lib/auth";
import { formatRelativeDate } from "@/lib/format";

export default function PostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const postId = Number(id);
  const router = useRouter();

  const [post, setPost] = useState<PostDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [commentText, setCommentText] = useState("");
  const [submittingComment, setSubmittingComment] = useState(false);
  const [myNickname, setMyNickname] = useState<string | null>(null);
  const [loggedIn, setLoggedIn] = useState(false);
  const commentRef = useRef<HTMLTextAreaElement | null>(null);

  function insertCommentText(text: string) {
    const ta = commentRef.current;
    if (!ta) {
      setCommentText((prev) => prev + text);
      return;
    }
    const start = ta.selectionStart ?? commentText.length;
    const end = ta.selectionEnd ?? commentText.length;
    setCommentText(commentText.slice(0, start) + text + commentText.slice(end));
    requestAnimationFrame(() => {
      ta.focus();
      const cursor = start + text.length;
      ta.setSelectionRange(cursor, cursor);
    });
  }

  useEffect(() => {
    setMyNickname(getNickname());
    setLoggedIn(isLoggedIn());
  }, []);

  useEffect(() => {
    setError(null);
    getPost(postId)
      .then(setPost)
      .catch((e) => setError(e instanceof Error ? e.message : "게시글을 불러올 수 없습니다."));
  }, [postId]);

  async function handleAddComment() {
    if (!commentText.trim()) return;
    setSubmittingComment(true);
    try {
      const c = await addComment(postId, commentText.trim());
      setPost((prev) => (prev ? { ...prev, comments: [...prev.comments, c] } : prev));
      setCommentText("");
    } catch (e) {
      alert(e instanceof Error ? e.message : "댓글 작성에 실패했습니다.");
    } finally {
      setSubmittingComment(false);
    }
  }

  async function handleDeleteComment(commentId: number) {
    if (!confirm("이 댓글을 삭제할까요?")) return;
    try {
      await deleteComment(commentId);
      setPost((prev) =>
        prev ? { ...prev, comments: prev.comments.filter((c) => c.id !== commentId) } : prev,
      );
    } catch (e) {
      alert(e instanceof Error ? e.message : "댓글 삭제에 실패했습니다.");
    }
  }

  async function handleDeletePost() {
    if (!confirm("이 게시글을 삭제할까요? (복구 불가)")) return;
    try {
      await deletePost(postId);
      router.push("/board");
    } catch (e) {
      alert(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    }
  }

  if (error) {
    return (
      <main className="min-h-screen bg-bg text-text">
        <Container size="narrow" className="py-16 text-center">
          <p className="text-red-400">{error}</p>
          <Link href="/board" className="mt-4 inline-block text-sm text-text-muted hover:text-text">
            ← 게시판으로
          </Link>
        </Container>
      </main>
    );
  }

  if (!post) {
    return (
      <main className="min-h-screen bg-bg text-text">
        <Container size="narrow" className="py-16 text-center text-sm text-text-muted">
          불러오는 중…
        </Container>
      </main>
    );
  }

  const cert: CertKey | null = certFromExamType(post.cert);
  const token = cert ? CERT_TOKENS[cert] : null;
  const isAuthor = !!myNickname && myNickname === post.authorNickname;

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
            {post.status === "PENDING" && (
              <span className="rounded-md border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-[11px] font-bold text-amber-400">
                ⏳ 승인 대기 중 (운영자 검토 후 게시)
              </span>
            )}
          </div>

          <h1 className="mt-3 text-2xl font-bold tracking-tight sm:text-3xl">{post.title}</h1>

          <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-text-muted tabular-nums">
            <span>{post.authorNickname}</span>
            <span aria-hidden>·</span>
            <span>{formatRelativeDate(post.createdAt)}</span>
            <span aria-hidden>·</span>
            <span>조회 {post.viewCount.toLocaleString()}</span>
          </div>

          <hr className="my-6 border-border" />

          <PostMarkdown content={post.content} />

          {isAuthor && (
            <div className="mt-8 flex justify-end">
              <Button variant="danger" size="sm" onClick={handleDeletePost}>
                삭제
              </Button>
            </div>
          )}
        </article>

        {/* 댓글 */}
        <section className="mt-8">
          <h2 className="mb-3 text-sm font-semibold">댓글 {post.comments.length}</h2>

          {loggedIn ? (
            <div className="rounded-lg border border-border bg-surface p-3">
              <textarea
                ref={commentRef}
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                placeholder="댓글을 입력하세요 (마크다운·이미지 가능)"
                rows={3}
                className="w-full resize-none rounded-md border border-border bg-bg-elevated px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              />
              <div className="mt-2 flex items-center justify-between">
                <ImageUploadButton onInsert={insertCommentText} disabled={submittingComment} />
                <Button
                  variant="primary"
                  size="sm"
                  loading={submittingComment}
                  onClick={handleAddComment}
                  disabled={!commentText.trim()}
                >
                  댓글 작성
                </Button>
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-border bg-surface p-4 text-center text-sm text-text-muted">
              댓글을 작성하려면 로그인이 필요합니다.
            </div>
          )}

          {post.comments.length > 0 && (
            <ul className="mt-4 space-y-2">
              {post.comments.map((c) => {
                const mine = !!myNickname && myNickname === c.authorNickname;
                return (
                  <li
                    key={c.id}
                    className="rounded-lg border border-border bg-surface px-4 py-3"
                  >
                    <div className="flex items-center justify-between gap-2 text-xs text-text-muted tabular-nums">
                      <span className="font-medium text-text">{c.authorNickname}</span>
                      <div className="flex items-center gap-2">
                        <span>{formatRelativeDate(c.createdAt)}</span>
                        {mine && (
                          <button
                            type="button"
                            onClick={() => handleDeleteComment(c.id)}
                            className="text-text-subtle hover:text-red-400"
                          >
                            삭제
                          </button>
                        )}
                      </div>
                    </div>
                    <div className="mt-1.5 text-sm">
                      <PostMarkdown content={c.content} />
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </Container>
    </main>
  );
}
