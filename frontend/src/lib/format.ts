export function formatDate(isoString: string): string {
  return new Date(isoString).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

/** Admin용 — 날짜 + 시:분 표시 */
export function formatDateTime(isoString: string): string {
  return new Date(isoString).toLocaleString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/** "오늘" / "N일 전" / "N주 전" / 오래되면 절대 날짜로 — 정보 밀도 ↑ */
export function formatRelativeDate(isoString: string): string {
  const target = new Date(isoString);
  const now = new Date();
  const startOfDay = (d: Date) => {
    const x = new Date(d);
    x.setHours(0, 0, 0, 0);
    return x;
  };
  const diffDays = Math.floor(
    (startOfDay(now).getTime() - startOfDay(target).getTime()) / 86400000
  );
  if (diffDays <= 0) return "오늘";
  if (diffDays === 1) return "어제";
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffDays < 28) return `${Math.floor(diffDays / 7)}주 전`;
  return target.toLocaleDateString("ko-KR", { year: "numeric", month: "short", day: "numeric" });
}
