/**
 * 공개 콘텐츠 API 클라이언트 — 서버 컴포넌트/sitemap/generateMetadata에서 사용.
 *
 * 런타임: Next.js 서버 (fetch 캐싱 활용).
 * 인증 불필요 — 백엔드 /api/public/** 는 인터셉터 없음.
 */

const BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const REVALIDATE_SEC = 60 * 30; // 30분 ISR

async function publicFetch<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}/api/public${path}`, {
    next: { revalidate: REVALIDATE_SEC },
    headers: { "Content-Type": "application/json" },
  });
  if (!res.ok) {
    throw new Error(`public API ${path} failed: ${res.status}`);
  }
  return res.json();
}

export type CertSlug = "sqld" | "engineer" | "computer-literacy-1";

export interface PublicCert {
  slug: CertSlug;
  name: string;
  description: string;
  questionCount: number;
  categoryCount: number;
}

export interface PublicCategory {
  id: number;
  name: string;
  parentName: string;
  questionCount: number;
}

export interface PublicQuestionSummary {
  id: number;
  contentPreview: string;
  topic: string | null;
  difficulty: number | null;
  questionType: "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";
}

export interface PublicQuestionPage {
  questions: PublicQuestionSummary[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface PublicQuestionDetail {
  id: number;
  certSlug: CertSlug;
  certName: string;
  categoryId: number;
  categoryName: string;
  content: string;
  questionType: "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";
  correctOption: number | null;
  answer: string | null;
  keywords: string[];
  explanation: string | null;
  topic: string | null;
  difficulty: number | null;
}

// sitemap.ts 에서 호출
export function getPublicCerts(): Promise<PublicCert[]> {
  return publicFetch("/certs");
}

export async function getPublicCategoriesByCert(
  slug: CertSlug | string,
): Promise<(PublicCategory & { slug: string })[]> {
  const data = await publicFetch<PublicCategory[]>(`/certs/${slug}/categories`);
  // URL 안전한 slug: cat-{id} — 한글 encode 복잡도 회피
  return data.map((c) => ({ ...c, slug: `cat-${c.id}` }));
}

export function getPublicQuestionsByCategory(
  categoryId: number,
  page = 0,
  size = 20,
): Promise<PublicQuestionPage> {
  return publicFetch(
    `/categories/${categoryId}/questions?page=${page}&size=${size}`,
  );
}

export function getPublicQuestionDetail(
  id: number,
): Promise<PublicQuestionDetail> {
  return publicFetch(`/questions/${id}`);
}

export function getPublicAllQuestionIds(): Promise<number[]> {
  return publicFetch("/questions/ids");
}

export interface PublicStats {
  totalMembers: number;
  totalSolves: number;
}

/** 랜딩 페이지 노출용 통계 (회원 수 + 누적 풀이 수) */
export function getPublicStats(): Promise<PublicStats> {
  return publicFetch("/stats");
}

export interface PublicRankingEntry {
  rank: number;
  nickname: string;
  totalCorrect: number;
}

export interface PublicRanking {
  entries: PublicRankingEntry[];
  generatedAt: string;
}

/** 랜딩 페이지 노출용 TOP 30 랭킹 (누적 정답 수) */
export function getPublicRanking(): Promise<PublicRanking> {
  return publicFetch("/ranking");
}

/** 블로그 전체 조회수 조회 */
export function getPublicBlogViews(): Promise<Record<string, number>> {
  return publicFetch("/blog/views");
}

// 카테고리 slug(cat-{id}) → id 역변환
export function parseCategorySlug(slug: string): number | null {
  const m = /^cat-(\d+)$/.exec(slug);
  return m ? parseInt(m[1], 10) : null;
}
