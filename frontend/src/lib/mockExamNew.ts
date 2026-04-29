import type { MockExamSummary } from "@/lib/mockExamApi";

const NEW_WINDOW_DAYS = 3;

/**
 * "이 회차가 지금 NEW 인가" 판정.
 *
 * 트리거 시각: pastExamLinkedAt > publishedAt > createdAt 우선순위로 가장 최근 시점.
 * 윈도우: 3일.
 *
 * PAST_EXAM kind 회차는 mock-exams 페이지에서 NEW 표시하지 않는다 — 기출복원 페이지가 담당.
 */
export function isExamNew(exam: MockExamSummary): boolean {
  if (exam.kind === "PAST_EXAM") return false;
  const triggerIso = newestTriggerIso(exam);
  if (!triggerIso) return false;
  return isWithinDays(triggerIso, NEW_WINDOW_DAYS);
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
