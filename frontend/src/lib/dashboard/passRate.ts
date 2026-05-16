/**
 * 예상 합격률 계산 — 신뢰도 보정 sigmoid.
 *
 * gap     = avg - threshold              (합격선까지의 점수 마진)
 * base    = 1 / (1 + e^(-gap/5))         (sigmoid, k=5)
 * shrink  = max(0, 1 - N/30)             (풀이수 30건 이상이면 보정 0)
 * final   = base * (1 - shrink) + 0.5 * shrink
 *
 * 풀이수가 적으면 50% 쪽으로 끌어당겨 과대 추정 방지.
 * 30건 이상은 base 그대로.
 */

import type { CertActivity } from "./activeCerts";
import { PASS_THRESHOLD } from "./activeCerts";
import type { CertKey } from "../cert-tokens";

/** 풀이수 이 값 이상이면 신뢰도 보정 0. */
const FULL_CONFIDENCE_N = 30;
/** sigmoid 의 가파름 — 작을수록 가파름. 5점 단위에서 의미있는 차이. */
const SIGMOID_K = 5;

export type PassBand = "stable" | "fit" | "effort" | "risk" | "challenge";

export type CertPassRate = {
  cert: CertKey;
  /** 0-100 정수 % */
  probPct: number;
  /** avg - threshold (소수 1자리) */
  margin: number;
  /** 신뢰도 0-1 (1 = full). 풀이수 비례. */
  confidence: number;
  band: PassBand;
};

function sigmoid(x: number): number {
  return 1 / (1 + Math.exp(-x));
}

function bandFor(margin: number): PassBand {
  if (margin >= 15) return "stable";
  if (margin >= 5) return "fit";
  if (margin >= 0) return "effort";
  if (margin >= -10) return "risk";
  return "challenge";
}

export function computePassRate(activity: CertActivity): CertPassRate {
  const threshold = PASS_THRESHOLD[activity.cert];
  const avg = activity.recent5AvgScore;
  const margin = avg - threshold;
  const base = sigmoid(margin / SIGMOID_K);
  const shrink = Math.max(0, 1 - activity.solveCount / FULL_CONFIDENCE_N);
  const prob = base * (1 - shrink) + 0.5 * shrink;
  return {
    cert: activity.cert,
    probPct: Math.round(prob * 100),
    margin: Math.round(margin * 10) / 10,
    confidence: 1 - shrink,
    band: bandFor(margin),
  };
}

/** band → token 색 매핑. CSS var 직접 참조해 다크/라이트 양쪽 유효. */
export const BAND_META: Record<
  PassBand,
  { label: string; textClass: string; barColor: string }
> = {
  stable: { label: "안정권", textClass: "text-primary", barColor: "var(--primary)" },
  fit: { label: "적정권", textClass: "text-primary", barColor: "var(--primary)" },
  effort: { label: "노력권", textClass: "text-warning", barColor: "var(--warning)" },
  risk: { label: "위험권", textClass: "text-warning", barColor: "var(--warning)" },
  challenge: { label: "도전권", textClass: "text-danger", barColor: "var(--danger)" },
};
