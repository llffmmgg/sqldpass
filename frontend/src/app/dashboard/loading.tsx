export default function DashboardLoading() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 h-7 w-48 animate-pulse rounded-md bg-surface/70" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="h-28 animate-pulse rounded-2xl border border-border bg-surface/40"
          />
        ))}
      </div>
      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="h-72 animate-pulse rounded-2xl border border-border bg-surface/40 lg:col-span-2" />
        <div className="h-72 animate-pulse rounded-2xl border border-border bg-surface/40" />
      </div>
    </div>
  );
}
