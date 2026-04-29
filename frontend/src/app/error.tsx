"use client";

import Link from "next/link";

interface ErrorPageProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: ErrorPageProps) {
  return (
    <div className="flex min-h-[60vh] items-center justify-center px-6 py-16">
      <div className="w-full max-w-md rounded-2xl border border-border bg-surface/60 p-8 text-center backdrop-blur">
        <div className="mb-4 text-4xl">⚠️</div>
        <h1 className="mb-2 text-xl font-semibold text-foreground">
          일시적인 오류가 발생했어요
        </h1>
        <p className="mb-6 text-sm leading-relaxed text-muted">
          잠시 후 다시 시도해주세요. 문제가 계속되면 우측 하단 피드백으로
          알려주시면 빠르게 살펴볼게요.
        </p>
        {error?.digest && (
          <p className="mb-6 select-all rounded-md bg-zinc-900/40 px-3 py-2 font-mono text-[11px] text-muted">
            {error.digest}
          </p>
        )}
        <div className="flex flex-col gap-2 sm:flex-row sm:justify-center">
          <button
            type="button"
            onClick={() => reset()}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition hover:bg-primary-hover"
          >
            다시 시도
          </button>
          <Link
            href="/"
            className="rounded-lg border border-border bg-transparent px-4 py-2 text-sm font-medium text-foreground transition hover:bg-surface"
          >
            홈으로
          </Link>
        </div>
      </div>
    </div>
  );
}
