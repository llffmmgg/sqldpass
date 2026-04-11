/** 백엔드 UTC 날짜를 KST Date로 변환 (타임존 접미사 없는 경우 UTC로 강제 해석) */
function toKstDate(isoString: string): Date {
  const s = isoString.endsWith("Z") || isoString.includes("+") ? isoString : isoString + "Z";
  return new Date(s);
}

export function formatDate(isoString: string): string {
  return toKstDate(isoString).toLocaleDateString("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

/** Admin용 — 날짜 + 시:분 표시 (KST) */
export function formatDateTime(isoString: string): string {
  return toKstDate(isoString).toLocaleString("ko-KR", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/** "오늘" / "N일 전" / "N주 전" / 오래되면 절대 날짜로 — 정보 밀도 ↑ */
export function formatRelativeDate(isoString: string): string {
  const target = toKstDate(isoString);
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
