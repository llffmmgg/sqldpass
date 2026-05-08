"use client";

import { useEffect, useState } from "react";
import { isCapacitorApp } from "@/lib/platform";
import { syncContent, type SyncProgress } from "@/lib/contentSync";
import { getMeta, getCacheStats } from "@/lib/offlineStore";

type Phase = "idle" | "blocking" | "background" | "error" | "done";

export default function InitialContentBootstrap() {
  const [phase, setPhase] = useState<Phase>("idle");
  const [progress, setProgress] = useState<SyncProgress | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!isCapacitorApp()) return;

    let cancelled = false;

    (async () => {
      const cachedVersion = await getMeta("version").catch(() => null);
      const stats = cachedVersion ? await getCacheStats().catch(() => null) : null;
      const hasCache = !!cachedVersion && (stats?.questions ?? 0) > 0;
      if (cancelled) return;

      setPhase(hasCache ? "background" : "blocking");

      try {
        await syncContent({
          onProgress: (p) => {
            if (cancelled) return;
            setProgress(p);
          },
        });
        if (!cancelled) setPhase("done");
      } catch (err) {
        if (cancelled) return;
        if (hasCache) {
          // Background refresh failed but offline cache is fine — let the user keep using the app.
          setPhase("done");
        } else {
          setProgress({
            phase: "error",
            error: err instanceof Error ? err.message : "콘텐츠 다운로드 실패",
          });
          setPhase("error");
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  if (phase === "idle" || phase === "done" || phase === "background") {
    return null;
  }

  return (
    <div
      role="dialog"
      aria-live="polite"
      className="fixed inset-0 z-[2000] flex flex-col items-center justify-center gap-6 bg-neutral-950 px-6 text-center text-neutral-100"
    >
      <div className="grid h-16 w-16 place-items-center rounded-2xl bg-gradient-to-br from-amber-400 to-amber-600 text-3xl font-extrabold text-neutral-950">
        문
      </div>

      <div className="space-y-2">
        <h1 className="text-lg font-semibold">문어CBT 준비 중</h1>
        <p className="text-sm text-neutral-400">
          {phase === "error"
            ? progress?.error ?? "콘텐츠 다운로드에 실패했어요."
            : progressMessage(progress)}
        </p>
      </div>

      {phase === "blocking" ? (
        <ProgressBar progress={progress} />
      ) : (
        <button
          type="button"
          onClick={() => {
            setProgress(null);
            setPhase("idle");
            setRetryKey((k) => k + 1);
          }}
          className="rounded-lg bg-amber-500 px-5 py-2.5 text-sm font-semibold text-neutral-950"
        >
          다시 시도
        </button>
      )}
    </div>
  );
}

function progressMessage(p: SyncProgress | null): string {
  if (!p) return "콘텐츠를 받아오는 중...";
  switch (p.phase) {
    case "checking":
      return "최신 콘텐츠 버전을 확인하는 중...";
    case "downloading":
      if (p.totalBytes && p.receivedBytes != null) {
        const mb = (p.receivedBytes / 1024 / 1024).toFixed(1);
        const total = (p.totalBytes / 1024 / 1024).toFixed(1);
        return `문제 데이터 받는 중... ${mb}MB / ${total}MB`;
      }
      if (p.receivedBytes != null) {
        const mb = (p.receivedBytes / 1024 / 1024).toFixed(1);
        return `문제 데이터 받는 중... ${mb}MB`;
      }
      return "문제 데이터 받는 중...";
    case "saving":
      return "오프라인 풀이용 데이터로 저장하는 중...";
    case "done":
      return "준비 완료!";
    case "skipped":
      return "이미 최신 상태예요.";
    case "error":
      return p.error ?? "오류가 발생했어요.";
  }
}

function ProgressBar({ progress }: { progress: SyncProgress | null }) {
  const ratio = computeRatio(progress);
  const indeterminate = ratio == null;
  return (
    <div className="w-full max-w-xs">
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-neutral-800">
        {indeterminate ? (
          <div
            className="h-full w-1/3 rounded-full bg-amber-500"
            style={{ animation: "boot-progress 1.2s ease-in-out infinite" }}
          />
        ) : (
          <div
            className="h-full rounded-full bg-amber-500 transition-[width] duration-300"
            style={{ width: `${(ratio * 100).toFixed(1)}%` }}
          />
        )}
      </div>
      <style jsx>{`
        @keyframes boot-progress {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(300%); }
        }
      `}</style>
    </div>
  );
}

function computeRatio(p: SyncProgress | null): number | null {
  if (!p) return null;
  if (p.phase !== "downloading") return null;
  if (!p.totalBytes || p.receivedBytes == null) return null;
  return Math.min(1, p.receivedBytes / p.totalBytes);
}
