"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import PostMarkdown from "@/components/PostMarkdown";
import { Button } from "@/components/ui";
import {
  addComment,
  deleteComment,
  deletePost,
  getPost,
  type PostComment,
  type PostDetail,
} from "@/lib/api";
import { getNickname, isLoggedIn } from "@/lib/auth";
import { formatRelativeDate } from "@/lib/format";

interface Props {
  postId: number;
  initial: PostDetail | null;
}

/**
 * 게시글 상세 인터랙션 (댓글 작성/삭제, 글 삭제, 본문 클라이언트 렌더).
 *
 * 본문/메타는 SSR 으로 layout 에서 렌더되지만, 댓글 작성·로그인 상태 동기화는
 * 클라이언트에서만 가능하므로 본문도 함께 클라이언트 렌더(SSR HTML 과 동일).
 *
 * initial 이 null 이면(SSR 실패 — 비공개/PENDING) 클라이언트가 토큰으로 재시도.
 */
export default function BoardPostClient({ postId, initial }: Props) {
  const router = useRouter();
  const [post, setPost] = useState<PostDetail | null>(initial);
  const [error, setError] = useState<string | null>(null);
  const [commentText, setCommentText] = useState("");
  const [submittingComment, setSubmittingComment] = useState(false);
  const [myNickname, setMyNickname] = useState<string | null>(null);
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setMyNickname(getNickname());
    setLoggedIn(isLoggedIn());
  }, []);

  // SSR 이 PUBLISHED 만 가져오므로, 작성자 본인의 PENDING 글은 fallback fetch.
  useEffect(() => {
    if (initial) return;
    let cancelled = false;
    getPost(postId)
      .then((p) => {
        if (!cancelled) setPost(p);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "게시글을 불러올 수 없습니다.");
      });
    return () => {
      cancelled = true;
    };
  }, [postId, initial]);

  async function handleAddComment() {
    if (!commentText.trim()) return;
    setSubmittingComment(true);
    try {
      const c = await addComment(postId, commentText.trim());
      setPost((prev) =>
        prev ? { ...prev, comments: [...prev.comments, c as PostComment] } : prev,
      );
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
        prev
          ? { ...prev, comments: prev.comments.filter((c) => c.id !== commentId) }
          : prev,
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
      <div className="py-16 text-center">
        <p className="text-red-400">{error}</p>
        <Link
          href="/board"
          className="mt-4 inline-block text-sm text-text-muted hover:text-text"
        >
          ← 게시판으로
        </Link>
      </div>
    );
  }

  if (!post) {
    return (
      <p className="py-16 text-center text-sm text-text-muted">불러오는 중…</p>
    );
  }

  const isAuthor = !!myNickname && myNickname === post.authorNickname;

  return (
    <>
      <PostMarkdown content={post.content} />

      {isAuthor && (
        <div className="mt-8 flex justify-end">
          <Button variant="danger" size="sm" onClick={handleDeletePost}>
            삭제
          </Button>
        </div>
      )}

      <section className="mt-8">
        <h2 className="mb-3 text-sm font-semibold">댓글 {post.comments.length}</h2>

        {loggedIn ? (
          <div className="rounded-lg border border-border bg-surface p-3">
            <textarea
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="댓글을 입력하세요"
              rows={3}
              className="w-full resize-none rounded-md border border-border bg-bg-elevated px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
            />
            <div className="mt-2 flex justify-end">
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
    </>
  );
}
