"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { getNickname } from "@/lib/auth";

interface RankingEntry {
  rank: number;
  nickname: string;
  totalCorrect: number;
}

interface RankingData {
  entries: RankingEntry[];
  generatedAt: string;
}

function subscribeAuth(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("storage", callback);
  return () => window.removeEventListener("storage", callback);
}

export default function RankingSection() {
  const [data, setData] = useState<RankingData | null>(null);
  // localStorage(외부 store) 구독 — set-state-in-effect 회피.
  const myNickname = useSyncExternalStore<string | null>(
    subscribeAuth,
    () => getNickname(),
    () => null,
  );

  useEffect(() => {
    let alive = true;
    fetch("/api/public/ranking")
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((next) => {
        if (alive) setData(next);
      })
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, []);

  if (!data || !data.entries || data.entries.length === 0) {
    return null;
  }

  return (
    <section className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
      <div className="text-center">
        <h2 className="text-2xl font-bold sm:text-3xl">
          🏆 <span className="text-primary">TOP 30 학습자</span>
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
              ? "text-primary"
              : entry.rank === 2
                ? "text-muted"
                : entry.rank === 3
                  ? "text-primary"
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
                  ? "bg-primary/10 ring-1 ring-inset ring-primary/40"
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
                  <span className="ml-2 rounded-full border border-primary/40 bg-primary/15 px-2 py-0.5 text-[10px] font-semibold text-primary">
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
