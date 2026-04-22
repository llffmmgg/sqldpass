/**
 * 연도별/회차별 합격률 추이 라인 차트 (서버 컴포넌트).
 * 단조 큐빅 스플라인 곡선 + 면적 그라디언트 + 평균선(점선) + 최고/최저 강조.
 *
 * MDX 사용:
 * <PassRateTrend label="SQLD 연도별 합격률" color="#f59e0b"
 *   items="2019:48.7:9663명 응시,2020:58.5:10933명 응시,..." />
 *
 * items 형식: "이름:값:부제,..." (부제는 선택, 값은 % 숫자)
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

function hash(s: string): string {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0;
  return Math.abs(h).toString(36);
}

function monotonePath(pts: { x: number; y: number }[]): string {
  const n = pts.length;
  if (n < 2) return "";
  if (n === 2) return `M ${pts[0].x} ${pts[0].y} L ${pts[1].x} ${pts[1].y}`;

  const dx: number[] = [];
  const dy: number[] = [];
  const m: number[] = [];
  for (let i = 0; i < n - 1; i++) {
    dx[i] = pts[i + 1].x - pts[i].x;
    dy[i] = pts[i + 1].y - pts[i].y;
    m[i] = dx[i] === 0 ? 0 : dy[i] / dx[i];
  }
  const t: number[] = [m[0]];
  for (let i = 1; i < n - 1; i++) {
    if (m[i - 1] * m[i] <= 0) t.push(0);
    else {
      const c = dx[i - 1] + dx[i];
      t.push((3 * c) / ((c + dx[i]) / m[i - 1] + (c + dx[i - 1]) / m[i]));
    }
  }
  t.push(m[n - 2]);

  let d = `M ${pts[0].x} ${pts[0].y}`;
  for (let i = 0; i < n - 1; i++) {
    const c1x = pts[i].x + dx[i] / 3;
    const c1y = pts[i].y + (t[i] * dx[i]) / 3;
    const c2x = pts[i + 1].x - dx[i] / 3;
    const c2y = pts[i + 1].y - (t[i + 1] * dx[i]) / 3;
    d += ` C ${c1x} ${c1y}, ${c2x} ${c2y}, ${pts[i + 1].x} ${pts[i + 1].y}`;
  }
  return d;
}

export default function PassRateTrend({
  items,
  color = "#f59e0b",
  label,
}: {
  items: string;
  color?: string;
  label?: string;
}) {
  const data = parseItems(items);
  if (data.length === 0) return null;

  const W = 800;
  const H = 320;
  const padL = 44;
  const padR = 72;
  const padT = 44;
  const padB = 56;
  const chartW = W - padL - padR;
  const chartH = H - padT - padB;

  const vals = data.map((d) => d.value);
  const rawMin = Math.min(...vals);
  const rawMax = Math.max(...vals);
  const span = rawMax - rawMin;
  const cushion = Math.max(span * 0.25, 8);
  let yMin = Math.max(0, Math.floor((rawMin - cushion) / 5) * 5);
  let yMax = Math.min(100, Math.ceil((rawMax + cushion) / 5) * 5);
  if (yMax - yMin < 20) {
    const mid = (yMax + yMin) / 2;
    yMin = Math.max(0, Math.floor((mid - 12) / 5) * 5);
    yMax = Math.min(100, Math.ceil((mid + 12) / 5) * 5);
  }
  if (yMax === yMin) {
    yMin = Math.max(0, yMin - 10);
    yMax = Math.min(100, yMax + 10);
  }

  const xOf = (i: number) =>
    data.length === 1 ? padL + chartW / 2 : padL + (i / (data.length - 1)) * chartW;
  const yOf = (v: number) => padT + (1 - (v - yMin) / (yMax - yMin)) * chartH;

  const points = data.map((d, i) => ({ x: xOf(i), y: yOf(d.value), d }));
  const bottomY = padT + chartH;
  const linePath = monotonePath(points);
  const areaPath =
    data.length >= 2
      ? `${linePath} L ${points[points.length - 1].x} ${bottomY} L ${points[0].x} ${bottomY} Z`
      : "";

  const gridTicks = [0, 1 / 3, 2 / 3, 1].map((t) => yMin + (yMax - yMin) * t);

  const avg = vals.reduce((a, b) => a + b, 0) / vals.length;
  const showAvg = data.length >= 3;

  const maxIdx = data.length >= 3 ? vals.indexOf(rawMax) : -1;
  const minIdx = data.length >= 3 ? vals.lastIndexOf(rawMin) : -1;

  const badges = points.map((p, i) => {
    const text = `${fmt(p.d.value)}%`;
    const w = text.length * 7 + 14;
    const h = 20;
    let bx = p.x - w / 2;
    let by = p.y - h - 10;
    if (bx < padL - 10) bx = padL - 10;
    if (bx + w > W - padR + 10) bx = W - padR + 10 - w;
    if (by < padT - 6) by = p.y + 12;
    return { bx, by, w, h, text, idx: i };
  });

  const gradId = `trend-grad-${hash(items + color)}`;
  const ariaLabel = label
    ? `${label}: ${data.map((d) => `${d.name} ${fmt(d.value)}%`).join(", ")}`
    : data.map((d) => `${d.name} ${fmt(d.value)}%`).join(", ");

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
            margin: "0 0 0.75rem",
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
      <svg
        viewBox={`0 0 ${W} ${H}`}
        preserveAspectRatio="xMidYMid meet"
        role="img"
        aria-label={ariaLabel}
        style={{
          width: "100%",
          height: "auto",
          display: "block",
          fontVariantNumeric: "tabular-nums",
        }}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity="0.22" />
            <stop offset="100%" stopColor={color} stopOpacity="0" />
          </linearGradient>
        </defs>

        {gridTicks.map((v, i) => {
          const y = yOf(v);
          const edge = i === 0 || i === gridTicks.length - 1;
          return (
            <g key={`g-${i}`}>
              <line
                x1={padL}
                x2={W - padR}
                y1={y}
                y2={y}
                stroke="var(--border)"
                strokeWidth="1"
                strokeDasharray={edge ? undefined : "2 4"}
                opacity={edge ? 0.75 : 0.45}
              />
              <text
                x={padL - 10}
                y={y + 3}
                textAnchor="end"
                fontSize="10"
                fill="var(--muted)"
                opacity="0.75"
              >
                {Math.round(v)}%
              </text>
            </g>
          );
        })}

        {areaPath && <path d={areaPath} fill={`url(#${gradId})`} />}

        {showAvg && (
          <g>
            <line
              x1={padL}
              x2={W - padR}
              y1={yOf(avg)}
              y2={yOf(avg)}
              stroke="var(--foreground)"
              strokeWidth="1"
              strokeDasharray="3 3"
              opacity="0.35"
            />
            <text
              x={W - padR + 6}
              y={yOf(avg) + 3}
              fontSize="10"
              fontWeight="500"
              fill="var(--muted)"
            >
              평균 {fmt(avg)}%
            </text>
          </g>
        )}

        {data.length >= 2 && (
          <path
            d={linePath}
            fill="none"
            stroke={color}
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}

        {points.map((p, i) => {
          const hot = i === maxIdx || i === minIdx;
          return (
            <g key={`p-${i}`}>
              {hot && <circle cx={p.x} cy={p.y} r="10" fill={color} opacity="0.12" />}
              <circle
                cx={p.x}
                cy={p.y}
                r={hot ? 5.5 : 4.5}
                fill={color}
                stroke="var(--surface)"
                strokeWidth="2.5"
              />
              <circle cx={p.x - 1} cy={p.y - 1.2} r="1.2" fill="var(--surface)" opacity="0.9" />
            </g>
          );
        })}

        {badges.map((b) => {
          const hot = b.idx === maxIdx || b.idx === minIdx;
          return (
            <g key={`b-${b.idx}`}>
              <rect
                x={b.bx}
                y={b.by}
                width={b.w}
                height={b.h}
                rx="4"
                fill="var(--surface)"
                stroke={hot ? color : "var(--border)"}
                strokeWidth={hot ? 1.25 : 1}
              />
              <text
                x={b.bx + b.w / 2}
                y={b.by + b.h / 2 + 4}
                textAnchor="middle"
                fontSize="11"
                fontWeight="700"
                fill={hot ? color : "var(--foreground)"}
              >
                {b.text}
              </text>
            </g>
          );
        })}

        {points.map((p, i) => (
          <g key={`x-${i}`}>
            <text
              x={p.x}
              y={bottomY + 22}
              textAnchor="middle"
              fontSize="11"
              fontWeight="500"
              fill="var(--foreground)"
            >
              {p.d.name}
            </text>
            {p.d.sub && (
              <text
                x={p.x}
                y={bottomY + 38}
                textAnchor="middle"
                fontSize="9.5"
                fill="var(--muted)"
                opacity="0.8"
              >
                {p.d.sub}
              </text>
            )}
          </g>
        ))}
      </svg>
    </div>
  );
}
