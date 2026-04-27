"use client";

/* eslint-disable react-hooks/set-state-in-effect -- 마운트 시 pending 목록 fetch 후 setState */

import Link from "next/link";
import { useEffect, useState } from "react";

import { CERT_TOKENS, certFromExamType, type CertKey } from "@/lib/cert-tokens";
import { adminListPendingPosts, type PostSummary } from "@/lib/api";

export default function AdminPostsPage() {
  const [items, setItems] = useState<PostSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    adminListPendingPosts()
      .then(setItems)
      .catch((e) => setError(e instanceof Error ? e.message : "목록을 불러올 수 없습니다."));
  }, []);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-xl font-bold tracking-tight">후기 승인 대기</h1>
        <p className="mt-1 text-sm text-muted">
          사용자가 제출한 합격 후기를 검토하고 승인 또는 삭제할 수 있어요.
        </p>
      </header>

      {error && (
        <div className="rounded border border-red-500/30 bg-red-500/5 px-4 py-3 text-sm text-red-400">
          {error}
        </div>
      )}

      {!items && !error && <p className="text-sm text-muted">불러오는 중…</p>}

      {items && items.length === 0 && (
        <div className="rounded border border-border bg-surface px-4 py-10 text-center text-sm text-muted">
          승인 대기 중인 후기가 없습니다.
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
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-foreground">{p.title}</p>
                    <p className="mt-1 text-xs text-muted tabular-nums">
                      {p.authorNickname} · {new Date(p.createdAt).toLocaleString("ko-KR")}
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
