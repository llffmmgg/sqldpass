/**
 * 공개 콘텐츠 API 클라이언트 — 서버 컴포넌트/sitemap/generateMetadata에서 사용.
 *
 * 런타임: Next.js 서버 (fetch 캐싱 활용).
 * 인증 불필요 — 백엔드 /api/public/** 는 인터셉터 없음.
 */

// SSR/ISR 전용. 서버 네트워크에서 백엔드 직통을 위해 INTERNAL_API_URL 우선.
// OCI compose 내부: http://app:8080 (nginx 우회)
// Vercel/로컬: NEXT_PUBLIC_API_URL 기존대로 사용
const BASE =
  process.env.INTERNAL_API_URL ??
  process.env.NEXT_PUBLIC_API_URL ??
  "http://localhost:8080";
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

export type CertSlug = "sqld" | "engineer" | "computer-literacy-1" | "computer-literacy-2" | "engineer-written" | "adsp";

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

/**
 * 자격증별 오늘의 문제 (날짜 시드 기반, 모든 사용자 동일).
 * 클라이언트 컴포넌트용 — 상대경로로 Vercel rewrites/OCI nginx 경유.
 */
export async function getDailyQuestion(
  cert: CertSlug | string,
): Promise<PublicQuestionDetail> {
  const res = await fetch(
    `/api/public/daily-question?cert=${encodeURIComponent(cert)}`,
    { cache: "no-store" },
  );
  if (!res.ok) {
    throw new Error(`daily-question failed: ${res.status}`);
  }
  return res.json();
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

/** 블로그 전체 조회수 조회 — 증가 반영이 즉시 보이도록 짧은 revalidate. */
export async function getPublicBlogViews(): Promise<Record<string, number>> {
  const res = await fetch(`${BASE}/api/public/blog/views`, {
    next: { revalidate: 30 },
    headers: { "Content-Type": "application/json" },
  });
  if (!res.ok) {
    throw new Error(`public API /blog/views failed: ${res.status}`);
  }
  return res.json();
}

// 카테고리 slug(cat-{id}) → id 역변환
export function parseCategorySlug(slug: string): number | null {
  const m = /^cat-(\d+)$/.exec(slug);
  return m ? parseInt(m[1], 10) : null;
}


// ================= 비회원 풀이 일일 한도 =================

export interface PublicSolveQuota {
  used: number;
  limit: number;
  remaining: number;
  exhausted: boolean;
  /** 서버 기준 오늘 (YYYY-MM-DD). 자정 리셋 안내용 */
  today: string;
}

/** 페이지 진입 시 1회 호출 — 헤더 칩 표시용. */
export async function getSolveQuota(): Promise<PublicSolveQuota> {
  const res = await fetch(`/api/public/solve-quota`, { cache: "no-store" });
  if (!res.ok) throw new Error(`solve-quota failed: ${res.status}`);
  return res.json();
}

/** 정답 제출 후 호출 — 서버에서 +1 한 뒤 갱신된 한도 상태를 반환. */
export async function incrementAnonymousSolve(
  delta: number = 1,
): Promise<PublicSolveQuota> {
  const res = await fetch(`/api/public/anonymous-solve?delta=${delta}`, {
    method: "POST",
  });
  if (!res.ok) throw new Error(`anonymous-solve failed: ${res.status}`);
  return res.json();
}

// ================= 기출 복원 (past-exams) — 서버용 SEO 호출 =================

export type PublicExamType =
  | "SQLD"
  | "ENGINEER_PRACTICAL"
  | "ENGINEER_WRITTEN"
  | "COMPUTER_LITERACY_1"
  | "COMPUTER_LITERACY_2"
  | "ADSP";

export interface PublicPastExamSummary {
  id: number;
  name: string;
  examType: PublicExamType;
  certSlug: string;
  totalQuestions: number;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  expertVerified: boolean;
  createdAt: string;
}

export interface PublicPastExamDetail {
  id: number;
  name: string;
  examType: PublicExamType;
  certSlug: string;
  totalQuestions: number;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  expertVerified: boolean;
}

export function getPublicPastExamsByCert(
  slug: CertSlug | string,
): Promise<PublicPastExamSummary[]> {
  return publicFetch(`/past-exams?cert=${encodeURIComponent(slug)}`);
}

export function getPublicPastExam(id: number): Promise<PublicPastExamDetail> {
  return publicFetch(`/past-exams/${id}`);
}
