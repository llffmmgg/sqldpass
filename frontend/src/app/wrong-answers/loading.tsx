export default function WrongAnswersLoading() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 h-7 w-32 animate-pulse rounded-md bg-surface/70" />
      <div className="mb-4 flex gap-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="h-8 w-24 animate-pulse rounded-full border border-border bg-surface/40"
          />
        ))}
      </div>
      <div className="space-y-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="h-24 animate-pulse rounded-2xl border border-border bg-surface/40"
          />
        ))}
      </div>
    </div>
  );
}
