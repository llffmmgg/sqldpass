export default function MockExamsLoading() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 h-7 w-40 animate-pulse rounded-md bg-surface/70" />
      <div className="mb-4 flex gap-2">
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="h-9 w-20 animate-pulse rounded-full border border-border bg-surface/40"
          />
        ))}
      </div>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 9 }).map((_, i) => (
          <div
            key={i}
            className="h-40 animate-pulse rounded-2xl border border-border bg-surface/40"
          />
        ))}
      </div>
    </div>
  );
}
