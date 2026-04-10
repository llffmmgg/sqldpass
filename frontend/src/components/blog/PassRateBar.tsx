/**
 * 합격률 수평 막대 그래프 (서버 컴포넌트).
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
    const parts = entry.trim().split(":");
    return {
      name: parts[0]?.trim() ?? "",
      value: parseFloat(parts[1] ?? "0"),
      sub: parts[2]?.trim(),
    };
  });

  if (data.length === 0) return null;
  const max = Math.max(...data.map((d) => d.value), 1);

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
      <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
        {data.map((d) => {
          const widthPct = Math.max((d.value / max) * 100, 3);
          return (
            <div
              key={d.name}
              style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}
            >
              <span
                style={{
                  width: "4rem",
                  flexShrink: 0,
                  textAlign: "right",
                  fontSize: "0.875rem",
                  fontWeight: 500,
                  color: "var(--foreground)",
                }}
              >
                {d.name}
              </span>
              <div
                style={{
                  position: "relative",
                  flex: 1,
                  height: "1.75rem",
                  borderRadius: "6px",
                  backgroundColor: "var(--border)",
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    height: "100%",
                    width: `${widthPct}%`,
                    borderRadius: "6px",
                    backgroundColor: color,
                    transition: "width 0.5s",
                  }}
                />
                <span
                  style={{
                    position: "absolute",
                    inset: 0,
                    display: "flex",
                    alignItems: "center",
                    paddingLeft: "0.75rem",
                    fontSize: "0.75rem",
                    fontWeight: 700,
                    color: "var(--foreground)",
                  }}
                >
                  {d.value}%
                  {d.sub && (
                    <span
                      style={{
                        marginLeft: "0.5rem",
                        fontWeight: 400,
                        color: "var(--muted)",
                      }}
                    >
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
