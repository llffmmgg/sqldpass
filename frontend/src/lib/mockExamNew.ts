import type { MockExamSummary } from "@/lib/mockExamApi";

const NEW_WINDOW_DAYS = 3;

/**
 * "이 회차가 지금 NEW 인가" 판정.
 *
 * 트리거 시각: publishedAt > createdAt 중 더 최근. 윈도우 3일.
 *
 * 기출과의 연동 차단:
 *  - PAST_EXAM kind 회차는 NEW 표시하지 않음 (기출복원 페이지가 담당).
 *  - pastExamLinkedAt 은 트리거에서 제외 — 기출 회차 추가로 LATEST 모의고사가 NEW 로 부활하지 않게.
 *  → mock-exams NEW 와 past-exams NEW 가 완전히 별개로 동작.
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
  const candidates = [exam.publishedAt, exam.createdAt].filter(
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
