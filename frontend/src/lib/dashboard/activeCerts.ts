/**
 * 사용자 풀이 기록에서 "활성 자격증" 도출 — Hero D-day / 추천 위젯에서 사용.
 * 풀이수 ≥ 5건인 자격증만 활성으로 인정 (가벼운 시험 풀이 1-2건은 노이즈).
 */

import type { SolveSummaryResponse } from "@/lib/api";
import type { CertKey } from "@/lib/cert-tokens";

export type CertActivity = {
  cert: CertKey;
  /** 해당 자격증 풀이 총 문제수 */
  solveCount: number;
  /** 최근 5회 풀이 score 평균 (0-100). 풀이 0건이면 0. */
  recent5AvgScore: number;
};

const MIN_SOLVES = 5;

function buildCertActivity(
  solves: SolveSummaryResponse[],
  subjectCertMap: Record<number, CertKey>,
  minSolves: number,
): CertActivity[] {
  const byCert = new Map<CertKey, {
    count: number;
    recentScores: { score: number; at: string }[];
  }>();

  for (const s of solves) {
    if (s.subjectId == null) continue;
    const cert = subjectCertMap[s.subjectId];
    if (!cert) continue;
    const entry = byCert.get(cert) ?? { count: 0, recentScores: [] };
    entry.count += s.totalCount;
    entry.recentScores.push({ score: s.score, at: s.solvedAt });
    byCert.set(cert, entry);
  }

  const result: CertActivity[] = [];
  for (const [cert, entry] of byCert) {
    if (entry.count < minSolves) continue;
    const recent5 = entry.recentScores
      .sort((a, b) => b.at.localeCompare(a.at))
      .slice(0, 5);
    const avg = recent5.length > 0
      ? recent5.reduce((s, r) => s + r.score, 0) / recent5.length
      : 0;
    result.push({ cert, solveCount: entry.count, recent5AvgScore: Math.round(avg * 10) / 10 });
  }

  result.sort((a, b) => b.solveCount - a.solveCount);
  return result;
}

export function inferActiveCerts(
  solves: SolveSummaryResponse[],
  subjectCertMap: Record<number, CertKey>,
): CertActivity[] {
  return buildCertActivity(solves, subjectCertMap, MIN_SOLVES);
}

/** 풀이 ≥1 건이지만 < MIN_SOLVES 인 자격증. 합격률 추정 불가 + CTA cell 용도. */
export function inferTouchedButInactiveCerts(
  solves: SolveSummaryResponse[],
  subjectCertMap: Record<number, CertKey>,
): CertActivity[] {
  const all = buildCertActivity(solves, subjectCertMap, 1);
  return all.filter((a) => a.solveCount < MIN_SOLVES);
}

/** 자격증별 합격선 (% 기준). 합격까지 점수 차이 계산용. */
export const PASS_THRESHOLD: Record<CertKey, number> = {
  SQLD: 60,
  ENGINEER_WRITTEN: 60,
  ENGINEER_PRACTICAL: 60,
  COMPUTER_LITERACY_1: 70,
  COMPUTER_LITERACY_2: 60,
  ADSP: 60,
};
