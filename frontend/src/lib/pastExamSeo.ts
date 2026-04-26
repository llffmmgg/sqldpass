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
  const title = `${token.label} 기출 복원 | 무료 CBT | 문어CBT`;
  const description = `${token.labelLong} 기출 복원 문제를 로그인 없이 확인하고, 로그인 후 채점과 해설까지 이어서 볼 수 있는 무료 CBT 페이지입니다.`;

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
