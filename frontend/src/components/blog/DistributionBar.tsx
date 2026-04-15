/**
 * 누적 수평 막대 — 전체 100%를 여러 세그먼트로 분할 시각화 (파이차트 대용).
 *
 * MDX 사용:
 * <DistributionBar label="정처기 실기 배점 분포"
 *   items="코드 작성:35:#10b981,용어·약술:25:#f59e0b,SQL 쿼리:15:#8b5cf6,기타 단답:25:#6b7280" />
 *
 * items 형식: "이름:값:색상,이름:값:색상,..."
 * - 값은 비율(합계가 100이면 100% 기준, 아니면 자동 정규화)
 * - 색상은 선택 (기본 palette에서 순환)
 */

const DEFAULT_COLORS = ["#f59e0b", "#10b981", "#8b5cf6", "#ef4444", "#06b6d4", "#ec4899"];

export default function DistributionBar({
  items,
  label,
}: {
  items: string;
  label?: string;
}) {
  if (!items) return null;

  const data = items.split(",").map((entry, idx) => {
    const parts = entry.trim().split(":");
    return {
      name: parts[0]?.trim() ?? "",
      value: parseFloat(parts[1] ?? "0"),
      color: parts[2]?.trim() || DEFAULT_COLORS[idx % DEFAULT_COLORS.length],
    };
  });

  if (data.length === 0) return null;
  const total = data.reduce((s, d) => s + d.value, 0);
  if (total <= 0) return null;

  return (
    <div
      style={{
        margin: "1.5rem 0",
        borderRadius: "12px",
        border: "1px solid var(--border)",
        backgroundColor: "var(--surface)",
        padding: "1.25rem",
      }}
    >
      {label && (
        <p
          style={{
            marginBottom: "1rem",
            fontSize: "0.75rem",
            fontWeight: 600,
            textTransform: "uppercase",
            letterSpacing: "0.05em",
            color: "var(--muted)",
          }}
        >
          {label}
        </p>
      )}

      {/* 누적 막대 */}
      <div
        style={{
          display: "flex",
          height: "2.25rem",
          width: "100%",
          borderRadius: "8px",
          overflow: "hidden",
          border: "1px solid var(--border)",
        }}
      >
        {data.map((d) => {
          const pct = (d.value / total) * 100;
          return (
            <div
              key={d.name}
              style={{
                width: `${pct}%`,
                backgroundColor: d.color,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "0.75rem",
                fontWeight: 700,
                color: "#fff",
                minWidth: pct > 5 ? "auto" : "0",
                overflow: "hidden",
                whiteSpace: "nowrap",
              }}
              title={`${d.name}: ${d.value} (${pct.toFixed(1)}%)`}
            >
              {pct >= 8 ? `${pct.toFixed(0)}%` : ""}
            </div>
          );
        })}
      </div>

      {/* 범례 */}
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "1rem",
          marginTop: "0.875rem",
          fontSize: "0.8125rem",
        }}
      >
        {data.map((d) => {
          const pct = (d.value / total) * 100;
          return (
            <div
              key={d.name}
              style={{ display: "flex", alignItems: "center", gap: "0.375rem" }}
            >
              <span
                style={{
                  display: "inline-block",
                  width: "0.75rem",
                  height: "0.75rem",
                  borderRadius: "2px",
                  backgroundColor: d.color,
                  flexShrink: 0,
                }}
              />
              <span style={{ color: "var(--foreground)", fontWeight: 500 }}>{d.name}</span>
              <span style={{ color: "var(--muted)", fontVariantNumeric: "tabular-nums" }}>
                {pct.toFixed(0)}%
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
