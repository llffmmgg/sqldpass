"use client";

import { useEffect, useState } from "react";
import { getNickname } from "@/lib/auth";
import type { PublicRanking } from "@/lib/publicApi";

/**
 * 랜딩 페이지 TOP 30 랭킹 섹션.
 *
 * 데이터는 server component(page.tsx)에서 ISR로 받아서 props로 전달.
 * 본인 매칭은 client에서 localStorage 닉네임으로 수행 (닉네임이 unique constraint).
 *
 * 30위 안에 있으면 amber 링 + 배경으로 강조.
 * 30위 밖이면 별도 표시 없음 (사용자 결정).
 */
export default function RankingSection({ data }: { data: PublicRanking }) {
  const [myNickname, setMyNickname] = useState<string | null>(null);

  useEffect(() => {
    setMyNickname(getNickname());
  }, []);

  if (!data.entries || data.entries.length === 0) {
    return null;
  }

  return (
    <section className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
      <div className="text-center">
        <h2 className="text-2xl font-bold sm:text-3xl">
          🏆 <span className="bg-gradient-to-r from-amber-400 via-amber-300 to-amber-500 bg-clip-text text-transparent">TOP 30 학습자</span>
        </h2>
        <p className="mt-2 text-sm text-muted">
          누적 정답 수 기준 · 1시간마다 갱신
        </p>
      </div>

      <ol className="mt-8 overflow-hidden rounded-xl border border-border bg-surface/40">
        {data.entries.map((entry) => {
          const isMe = myNickname !== null && entry.nickname === myNickname;
          const rankColor =
            entry.rank === 1
              ? "text-amber-300"
              : entry.rank === 2
                ? "text-zinc-300"
                : entry.rank === 3
                  ? "text-orange-300"
                  : "text-muted";
          const rankIcon =
            entry.rank === 1
              ? "🥇"
              : entry.rank === 2
                ? "🥈"
                : entry.rank === 3
                  ? "🥉"
                  : null;

          return (
            <li
              key={`${entry.rank}-${entry.nickname}`}
              className={`flex items-center gap-3 border-b border-border/50 px-4 py-3 last:border-b-0 transition-colors ${
                isMe
                  ? "bg-amber-500/10 ring-1 ring-inset ring-amber-400/40"
                  : "hover:bg-surface/70"
              }`}
            >
              <div className={`flex w-12 shrink-0 items-center gap-1 font-mono text-sm font-semibold tabular-nums ${rankColor}`}>
                {rankIcon ? <span className="text-base">{rankIcon}</span> : <span className="text-muted/70">#</span>}
                <span>{entry.rank}</span>
              </div>
              <div className="flex-1 truncate text-sm font-medium text-foreground">
                {entry.nickname}
                {isMe && (
                  <span className="ml-2 rounded-full border border-amber-400/40 bg-amber-500/15 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
                    나
                  </span>
                )}
              </div>
              <div className="shrink-0 font-mono text-sm font-semibold tabular-nums text-foreground/90">
                {entry.totalCorrect.toLocaleString("ko-KR")}
                <span className="ml-1 text-xs text-muted">개</span>
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
