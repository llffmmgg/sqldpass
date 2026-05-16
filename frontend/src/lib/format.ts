/**
 * 백엔드는 JVM TZ=Asia/Seoul + Jackson time-zone=Asia/Seoul 설정으로
 * LocalDateTime 을 KST naive ISO 로 직렬화한다. ("2026-05-17T11:21:00")
 * 따라서 TZ 접미사 없는 문자열은 UTC 가 아닌 KST 로 해석해야 시간이 맞다.
 * Instant/OffsetDateTime 은 +09:00 offset 이 붙어서 그대로 절대 시각으로 파싱된다.
 */
function toKstDate(isoString: string): Date {
  // Z 또는 ±HH:MM offset 명시 → 절대 시각으로 그대로 파싱
  if (isoString.endsWith("Z") || /[+-]\d{2}:?\d{2}$/.test(isoString)) {
    return new Date(isoString);
  }
  // TZ 미지정 (naive LocalDateTime) → KST 로 가정. 브라우저 TZ 무관.
  return new Date(isoString + "+09:00");
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
