export default function SolveLoading() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-center justify-between">
        <div className="h-6 w-28 animate-pulse rounded-md bg-surface/70" />
        <div className="h-6 w-20 animate-pulse rounded-md bg-surface/70" />
      </div>
      <div className="rounded-2xl border border-border bg-surface/40 p-6">
        <div className="mb-4 h-6 w-3/4 animate-pulse rounded-md bg-surface/70" />
        <div className="mb-2 h-4 w-full animate-pulse rounded-md bg-surface/60" />
        <div className="mb-2 h-4 w-5/6 animate-pulse rounded-md bg-surface/60" />
        <div className="mb-6 h-4 w-2/3 animate-pulse rounded-md bg-surface/60" />
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="h-12 animate-pulse rounded-xl border border-border bg-surface/40"
            />
          ))}
        </div>
      </div>
    </div>
  );
}
