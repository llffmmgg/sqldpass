import type { Metadata } from "next";

import { CERT_TOKENS, certFromSlug } from "@/lib/cert-tokens";

const SITE_URL = "https://www.sqldpass.com";

export function buildPastExamCertMetadata(certSlug: string): Metadata {
  const cert = certFromSlug(certSlug);
  if (!cert) {
    return {};
  }

  const token = CERT_TOKENS[cert];
  const canonical = `${SITE_URL}/past-exams/${certSlug}`;
  // "CBT" 표현은 /cbt-mock-exam/{cert} 가 단독 거점. 여기서는 "기출 복원" 키워드로 분리.
  const title = `${token.label} 기출 복원 — 회차별 무료 풀이 | 문어CBT`;
  const description = `${token.labelLong} 정기 회차 기출 복원 문제집. 회차별로 실전 타이머에 풀어보고 로그인 후 자동 채점·해설까지 이어서 확인할 수 있습니다.`;

  return {
    title,
    description,
    alternates: { canonical },
    openGraph: {
      title,
      description,
      url: canonical,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
    },
  };
}
