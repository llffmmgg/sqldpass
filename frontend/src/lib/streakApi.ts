/**
 * 연속 학습(streak) 조회 — 로그인 사용자 전용.
 */
import { fetchApi } from "@/lib/api";

export interface Streak {
  currentStreak: number;
  longestStreak: number;
  lastSolveDate: string | null;
  solvedToday: boolean;
}

export function getMyStreak(): Promise<Streak> {
  return fetchApi<Streak>("/streak/me");
}

/** 내 마지막 풀이 자격증 slug (Daily Question 기본 탭 계산용). 없으면 null. */
export async function getLastSolvedCert(): Promise<string | null> {
  const res = await fetchApi<{ cert: string | null }>("/solves/me/last-cert");
  return res.cert ?? null;
}
