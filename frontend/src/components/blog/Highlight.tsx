/**
 * 색상 강조 텍스트.
 * MDX에서 <Highlight color="red">텍스트</Highlight> 로 사용.
 */

const COLORS: Record<string, string> = {
  red: "#ef4444",
  blue: "#3b82f6",
  green: "#22c55e",
  amber: "#f59e0b",
  emerald: "#10b981",
  purple: "#8b5cf6",
};

export default function Highlight({
  children,
  color = "red",
  bold = true,
}: {
  children: React.ReactNode;
  color?: string;
  bold?: boolean;
}) {
  const resolved = COLORS[color] ?? color;
  return (
    <span style={{ color: resolved, fontWeight: bold ? 700 : undefined }}>
      {children}
    </span>
  );
}
