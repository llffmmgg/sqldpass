"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 탭 변경 시 fetch + state reset 패턴 */

import Link from "next/link";
import { useEffect, useState } from "react";

import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import { adminListPosts, type PostStatus, type PostSummary } from "@/lib/api";

type Tab = "ALL" | "PENDING" | "PUBLISHED";

export default function AdminPostsPage() {
  const [tab, setTab] = useState<Tab>("PENDING");
  const [items, setItems] = useState<PostSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    setItems(null);
    const status: PostStatus | undefined = tab === "ALL" ? undefined : tab;
    adminListPosts(status)
      .then(setItems)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, [tab]);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight">게시판 관리</h1>
        <p className="mt-1 text-sm text-muted">
          승인 대기 중인 후기 검토 + 발행된 게시글·댓글 관리.
        </p>
      </header>

      {/* 탭 */}
      <div className="flex gap-1 rounded-lg border border-border bg-surface p-1 text-sm">
        <TabButton active={tab === "PENDING"} onClick={() => setTab("PENDING")} label="승인 대기" />
        <TabButton active={tab === "PUBLISHED"} onClick={() => setTab("PUBLISHED")} label="발행됨" />
        <TabButton active={tab === "ALL"} onClick={() => setTab("ALL")} label="전체" />
      </div>

      {error && (
        <div className="rounded border border-red-500/30 bg-red-500/5 px-4 py-3 text-sm text-red-400">
          {error}
        </div>
      )}

      {!items && !error && <p className="text-sm text-muted">불러오는 중…</p>}

      {items && items.length === 0 && (
        <div className="rounded border border-border bg-surface px-4 py-10 text-center text-sm text-muted">
          {tab === "PENDING" && "승인 대기 중인 후기가 없습니다."}
          {tab === "PUBLISHED" && "발행된 게시글이 없습니다."}
          {tab === "ALL" && "게시글이 없습니다."}
        </div>
      )}

      {items && items.length > 0 && (
        <ul className="divide-y divide-border rounded border border-border bg-surface">
          {items.map((p) => {
            const cert: CertKey | null = certFromExamType(p.cert);
            const token = cert ? CERT_TOKENS[cert] : null;
            return (
              <li key={p.id}>
                <Link
                  href={`/admin/posts/${p.id}`}
                  className="flex items-center gap-3 px-4 py-3 hover:bg-background/40"
                >
                  {token && (
                    <span
                      className={`shrink-0 rounded-md border px-2 py-0.5 text-[10px] font-bold ${token.tailwind.border} ${token.tailwind.bgSoft} ${token.tailwind.text}`}
                    >
                      {token.label}
                    </span>
                  )}
                  <span
                    className={`shrink-0 rounded-md border px-2 py-0.5 text-[10px] font-bold ${
                      p.status === "PENDING"
                        ? "border-amber-500/30 bg-amber-500/10 text-amber-400"
                        : "border-green-500/30 bg-green-500/10 text-green-400"
                    }`}
                  >
                    {p.status === "PENDING" ? "대기" : "발행"}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-foreground">{p.title}</p>
                    <p className="mt-1 flex items-center gap-2 text-xs text-muted tabular-nums">
                      <span>{p.authorNickname}</span>
                      <span aria-hidden>·</span>
                      <span>{new Date(p.createdAt).toLocaleString("ko-KR")}</span>
                      {p.commentCount > 0 && (
                        <>
                          <span aria-hidden>·</span>
                          <span>댓글 {p.commentCount}</span>
                        </>
                      )}
                      {p.viewCount > 0 && (
                        <>
                          <span aria-hidden>·</span>
                          <span>조회 {p.viewCount.toLocaleString()}</span>
                        </>
                      )}
                    </p>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  label,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 rounded-md px-3 py-1.5 transition-colors ${
        active
          ? "bg-primary/10 font-semibold text-primary"
          : "text-muted hover:bg-background/40 hover:text-foreground"
      }`}
    >
      {label}
    </button>
  );
}
