import { fetchApi } from "@/lib/api";

// GET /api/quota — 무료 회원의 오늘 사용량/한도를 한 번에 반환.
//
// 백엔드 계약:
// - `questionUsed` / `mockUsed`: 오늘 사용한 개수 (정수)
// - `questionLimit` / `mockLimit`: 한도. **null 이면 무제한** (활성 구독자/특정 흐름)
// - `resetAt`: KST naive ISO ("2026-05-22T00:00:00", Z 없음).
//
// 카운팅·증가 로직은 클라가 일절 만지지 않는다. 서버가 단일 진실 소스.
export type Quota = {
  questionUsed: number;
  questionLimit: number | null;
  mockUsed: number;
  mockLimit: number | null;
  resetAt: string;
};

export async function fetchQuota(): Promise<Quota> {
  return fetchApi<Quota>("/quota");
}
