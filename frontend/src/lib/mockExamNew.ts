import type { MockExamSummary } from "@/lib/mockExamApi";

const NEW_WINDOW_DAYS = 3;
const SEEN_KEY_PREFIX = "mockexam:seen:";

/**
 * "이 회차가 지금 NEW 인가" 판정.
 *
 * 트리거 시각: pastExamLinkedAt > publishedAt > createdAt 우선순위로 가장 최근 시점.
 * 윈도우: 3일. 그 안에 들어왔으면 NEW.
 * Per-user dismissal: 사용자가 한 번이라도 카드를 클릭했으면 그 회차 NEW 표시 해제.
 *   (트리거가 또 갱신되면 다시 NEW 로 등장)
 */
export function isExamNew(exam: MockExamSummary): boolean {
  const triggerIso = newestTriggerIso(exam);
  if (!triggerIso) return false;
  if (!isWithinDays(triggerIso, NEW_WINDOW_DAYS)) return false;
  if (typeof window === "undefined") return true;
  try {
    const seenAt = window.localStorage.getItem(SEEN_KEY_PREFIX + exam.id);
    if (seenAt && new Date(seenAt) >= new Date(triggerIso)) return false;
  } catch {
    // localStorage 접근 차단 환경 — 시간 기준만 적용
  }
  return true;
}

export function markExamSeen(examId: number): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(SEEN_KEY_PREFIX + examId, new Date().toISOString());
  } catch {
    // 스토리지 차단 시 무시
  }
}

export function countNewExams(exams: MockExamSummary[] | null | undefined): number {
  if (!exams) return 0;
  return exams.filter(isExamNew).length;
}

function newestTriggerIso(exam: MockExamSummary): string | null {
  const candidates = [exam.pastExamLinkedAt, exam.publishedAt, exam.createdAt].filter(
    (v): v is string => Boolean(v),
  );
  if (candidates.length === 0) return null;
  return candidates.reduce((a, b) => (new Date(a).getTime() > new Date(b).getTime() ? a : b));
}

function isWithinDays(iso: string, days: number): boolean {
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return false;
  return Date.now() - t <= days * 24 * 60 * 60 * 1000;
}
