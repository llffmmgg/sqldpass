"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 게시판 목록 fetch 후 setState */

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { Container } from "@/components/ui";
import { CERT_LIST, CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import { listPosts, type PostPage, type PostSummary } from "@/lib/api";
import { formatRelativeDate } from "@/lib/format";

export default function BoardPage() {
  // useSearchParams 는 CSR bailout 이라 prerender 시 Suspense 경계 필요
  return (
    <Suspense fallback={<BoardLoading />}>
      <BoardContent />
    </Suspense>
  );
}

function BoardLoading() {
  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="default" className="py-12">
        <p className="py-16 text-center text-sm text-text-muted">불러오는 중…</p>
      </Container>
    </main>
  );
}

function BoardContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const certParam = searchParams.get("cert"); // ExamType key (예: SQLD)

  const [data, setData] = useState<PostPage | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    listPosts({ category: "PASS_REVIEW", cert: certParam ?? undefined, size: 30 })
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "게시판을 불러올 수 없습니다."));
  }, [certParam]);

  function setCertFilter(cert: string | null) {
    const params = new URLSearchParams(searchParams.toString());
    if (cert) params.set("cert", cert);
    else params.delete("cert");
    router.push(`/board${params.toString() ? `?${params.toString()}` : ""}`, { scroll: false });
  }

  return (
    <main className="min-h-screen bg-bg text-text">
      <Container size="default" className="py-12">
        <header className="mb-8 flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">합격 후기</h1>
            <p className="mt-1.5 text-sm text-text-muted">
              합격 후기를 공유해주세요. 운영자 검토 후 게시됩니다.
            </p>
          </div>
          <Link
            href="/board/submit"
            className="inline-flex h-10 shrink-0 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-primary-fg transition-colors hover:bg-primary-hover"
          >
            <PencilIcon className="h-3.5 w-3.5" />
            후기 쓰기
          </Link>
        </header>

        {/* 자격증 chip filter (태그) */}
        <div className="mb-6 flex flex-wrap items-center gap-1.5">
          <FilterChip active={!certParam} onClick={() => setCertFilter(null)} label="전체" />
          {CERT_LIST.map((c) => (
            <FilterChip
              key={c.key}
              active={certParam === c.key}
              onClick={() => setCertFilter(c.key)}
              label={c.label}
              dotClass={c.tailwind.dot}
            />
          ))}
        </div>

        {error && (
          <div className="rounded-lg border border-red-500/30 bg-red-500/5 px-4 py-3 text-sm text-red-400">
            {error}
          </div>
        )}

        {!error && !data && (
          <p className="py-16 text-center text-sm text-text-muted">불러오는 중…</p>
        )}

        {data && data.items.length === 0 && (
          <div className="rounded-xl border border-border bg-surface p-10 text-center">
            <p className="text-base font-semibold">아직 등록된 후기가 없어요</p>
            <p className="mt-2 text-sm text-text-muted">
              첫 합격 후기의 주인공이 되어주세요.
            </p>
            <Link
              href="/board/submit"
              className="mt-5 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-fg hover:bg-primary-hover"
            >
              <PencilIcon className="h-3.5 w-3.5" />
              후기 쓰기
            </Link>
          </div>
        )}

        {data && data.items.length > 0 && (
          <ul className="divide-y divide-border border-y border-border">
            {data.items.map((p) => (
              <li key={p.id}>
                <PostRow post={p} />
              </li>
            ))}
          </ul>
        )}
      </Container>
    </main>
  );
}

function FilterChip({
  active,
  onClick,
  label,
  dotClass,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
  dotClass?: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
        active
          ? "border-primary/40 bg-primary/10 text-primary"
          : "border-border bg-surface text-text-muted hover:border-border-strong hover:text-text"
      }`}
    >
      {dotClass && <span className={`h-1.5 w-1.5 rounded-full ${dotClass}`} aria-hidden />}
      {label}
    </button>
  );
}

function PostRow({ post }: { post: PostSummary }) {
  const cert: CertKey | null = certFromExamType(post.cert);
  const token = cert ? CERT_TOKENS[cert] : null;
  return (
    <Link
      href={`/board/${post.id}`}
      className="flex items-center gap-3 px-4 py-3 transition-colors hover:bg-surface"
    >
      {token && (
        <span
          className={`shrink-0 rounded-md border px-2 py-0.5 text-[10px] font-bold ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.text}`}
        >
          {token.label}
        </span>
      )}
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-text">{post.title}</p>
        <div className="mt-1 flex items-center gap-2 text-[11px] text-text-muted tabular-nums">
          <span>{post.authorNickname}</span>
          <span aria-hidden>·</span>
          <span>{formatRelativeDate(post.createdAt)}</span>
          <span aria-hidden>·</span>
          <span>조회 {post.viewCount.toLocaleString()}</span>
          {post.commentCount > 0 && (
            <>
              <span aria-hidden>·</span>
              <span>댓글 {post.commentCount}</span>
            </>
          )}
        </div>
      </div>
    </Link>
  );
}

function PencilIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
    </svg>
  );
}
