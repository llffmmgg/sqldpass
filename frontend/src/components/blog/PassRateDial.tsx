/**
 * 카테고리 합격률 원형 게이지 그리드 (서버 컴포넌트).
 * 연도별 추이가 아닌 "항목 간 비교"용 (1급 vs 2급, 필기 vs 실기 등).
 *
 * MDX 사용:
 * <PassRateDial label="컴활 필기·실기 합격률" color="#6366f1"
 *   items="2급 필기:65:60~70% 범위,1급 필기:55:50~60% 범위,..." />
 *
 * items 형식: "이름:값:부제,..." (값은 %)
 */

type Point = { name: string; value: number; sub?: string };

function parseItems(items: string): Point[] {
  return items
    .split(",")
    .map((entry) => {
      const parts = entry.trim().split(":");
      return {
        name: parts[0]?.trim() ?? "",
        value: parseFloat(parts[1] ?? "0"),
        sub: parts[2]?.trim() || undefined,
      };
    })
    .filter((p) => p.name !== "" && Number.isFinite(p.value));
}

function fmt(v: number): string {
  return Number.isInteger(v) ? v.toString() : v.toFixed(1);
}

export default function PassRateDial({
  items,
  color = "#6366f1",
  label,
}: {
  items: string;
  color?: string;
  label?: string;
}) {
  const data = parseItems(items);
  if (data.length === 0) return null;

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
            margin: "0 0 1rem",
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
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
          gap: "0.75rem",
        }}
      >
        {data.map((d, i) => (
          <Dial key={`${d.name}-${i}`} point={d} color={color} />
        ))}
      </div>
    </div>
  );
}

function Dial({ point, color }: { point: Point; color: string }) {
  const size = 128;
  const strokeWidth = 8;
  const r = (size - strokeWidth) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const C = 2 * Math.PI * r;
  const clamped = Math.max(0, Math.min(100, point.value));
  const offset = C * (1 - clamped / 100);

  const ariaLabel = `${point.name} ${fmt(point.value)}%${
    point.sub ? ` (${point.sub})` : ""
  }`;

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "0.5rem",
        padding: "0.75rem 0.5rem",
      }}
    >
      <p
        style={{
          margin: 0,
          fontSize: "0.8125rem",
          fontWeight: 600,
          color: "var(--foreground)",
          textAlign: "center",
          lineHeight: 1.3,
        }}
      >
        {point.name}
      </p>
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        role="img"
        aria-label={ariaLabel}
        style={{ fontVariantNumeric: "tabular-nums" }}
      >
        <circle
          cx={cx}
          cy={cy}
          r={r}
          fill="none"
          stroke="var(--border)"
          strokeWidth={strokeWidth}
          opacity="0.7"
        />
        <circle
          cx={cx}
          cy={cy}
          r={r}
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeDasharray={C}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform={`rotate(-90 ${cx} ${cy})`}
        />
        <text
          x={cx}
          y={cy}
          textAnchor="middle"
          dominantBaseline="central"
          fontSize="22"
          fontWeight="700"
          fill="var(--foreground)"
        >
          {fmt(point.value)}
          <tspan fontSize="13" fontWeight="600" dy="-4" dx="1" fill="var(--muted)">
            %
          </tspan>
        </text>
      </svg>
      {point.sub && (
        <p
          style={{
            margin: 0,
            fontSize: "0.6875rem",
            color: "var(--muted)",
            textAlign: "center",
            letterSpacing: "0.02em",
            lineHeight: 1.3,
          }}
        >
          {point.sub}
        </p>
      )}
    </div>
  );
}
