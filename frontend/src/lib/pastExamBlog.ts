import {
  CERT_TOKENS,
  certFromExamType,
  certFromSlug,
  type CertKey,
} from "@/lib/cert-tokens";
import type { PublicPastExamSummary } from "@/lib/publicApi";

/**
 * 기출 복원 블로그 페이지 슬러그.
 * 패턴: `{certSlug}-{year}-{round}` (예: sqld-2025-57)
 * year 또는 round 가 없으면 id 폴백 (`{certSlug}-id-{id}`).
 */
export function pastExamBlogSlug(exam: PublicPastExamSummary): string {
  if (exam.examYear != null && exam.examRound != null) {
    return `${exam.certSlug}-${exam.examYear}-${exam.examRound}`;
  }
  return `${exam.certSlug}-id-${exam.id}`;
}

/** 슬러그 파싱 → 매칭되는 exam 찾기. */
export function findPastExamBySlug(
  exams: PublicPastExamSummary[],
  slug: string,
): PublicPastExamSummary | null {
  return exams.find((e) => pastExamBlogSlug(e) === slug) ?? null;
}

/**
 * 블로그 페이지에 노출할 제목.
 * 예: `[SQLD] 2025년 57회 기출 복원`
 */
export function pastExamBlogTitle(exam: PublicPastExamSummary): string {
  const cert = certFromExamType(exam.examType);
  const label = cert ? CERT_TOKENS[cert].label : exam.certSlug;

  const parts: string[] = [];
  if (exam.examYear != null) parts.push(`${exam.examYear}년`);
  if (exam.examRound != null) parts.push(`${exam.examRound}회`);
  if (parts.length === 0) parts.push(exam.name);

  return `[${label}] ${parts.join(" ")} 기출 복원`;
}

/**
 * 메타 description (검색 결과 스니펫).
 */
export function pastExamBlogDescription(exam: PublicPastExamSummary): string {
  const cert = certFromExamType(exam.examType);
  const labelLong = cert ? CERT_TOKENS[cert].labelLong : exam.certSlug;

  const parts: string[] = [];
  if (exam.examYear != null) parts.push(`${exam.examYear}년`);
  if (exam.examRound != null) parts.push(`${exam.examRound}회`);
  const round = parts.join(" ");

  return `${labelLong} ${round} 기출 복원 ${exam.totalQuestions}문항. 정답·해설을 펼쳐보고 직접 풀어볼 수도 있습니다.`;
}

/** /blog/past-exam/{slug} → CertKey */
export function certFromBlogPastExamSlug(slug: string): CertKey | null {
  // slug 형식: {certSlug}-{...}. certSlug 부분이 multi-word(`engineer-written`, `computer-literacy-1`) 일 수 있어 prefix 매칭.
  const candidates: { slug: string; key: CertKey }[] = [
    { slug: "sqld", key: "SQLD" },
    { slug: "engineer-written", key: "ENGINEER_WRITTEN" },
    { slug: "engineer", key: "ENGINEER_PRACTICAL" },
    { slug: "computer-literacy-1", key: "COMPUTER_LITERACY_1" },
    { slug: "computer-literacy-2", key: "COMPUTER_LITERACY_2" },
    { slug: "adsp", key: "ADSP" },
  ];
  // 더 긴 slug 가 먼저 매칭되도록 정렬 (engineer-written 이 engineer 보다 먼저)
  candidates.sort((a, b) => b.slug.length - a.slug.length);
  for (const c of candidates) {
    if (slug.startsWith(`${c.slug}-`)) return c.key;
  }
  return certFromSlug(slug);
}
