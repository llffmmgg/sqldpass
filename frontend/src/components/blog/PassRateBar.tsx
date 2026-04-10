"use client";

/**
 * 합격률 수평 막대 그래프.
 * MDX에서 사용:
 * <PassRateBar label="제목" color="#f59e0b" items="2019:48.7:9663명 응시,2020:58.5:10933명 응시" />
 *
 * items 형식: "이름:값:부제,이름:값:부제,..."  (부제는 선택)
 */
export default function PassRateBar({
  items,
  color = "#f59e0b",
  label,
}: {
  items: string;
  color?: string;
  label?: string;
}) {
  if (!items) return null;

  const data = items.split(",").map((entry) => {
    const [name, valueStr, sub] = entry.trim().split(":");
    return { name: name.trim(), value: parseFloat(valueStr), sub: sub?.trim() };
  });

  if (data.length === 0) return null;
  const max = Math.max(...data.map((d) => d.value), 1);

  return (
    <div className="my-6 rounded-xl border border-border bg-surface p-5">
      {label && (
        <p className="mb-4 text-xs font-semibold uppercase tracking-wide text-muted">
          {label}
        </p>
      )}
      <div className="space-y-3">
        {data.map((d) => {
          const width = Math.max((d.value / max) * 100, 2);
          return (
            <div key={d.name} className="flex items-center gap-3">
              <span className="w-16 shrink-0 text-right text-sm font-medium text-foreground">
                {d.name}
              </span>
              <div className="relative flex-1 h-7 rounded-md bg-border/30 overflow-hidden">
                <div
                  className="h-full rounded-md transition-all duration-500"
                  style={{ width: `${width}%`, backgroundColor: color }}
                />
                <span className="absolute inset-0 flex items-center px-3 text-xs font-bold text-foreground">
                  {d.value}%
                  {d.sub && (
                    <span className="ml-1.5 font-normal text-muted">
                      {d.sub}
                    </span>
                  )}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
