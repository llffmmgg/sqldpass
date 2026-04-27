import { getToken, clearAuth } from "@/lib/auth";

const BASE = "/api";

export async function fetchApi<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });

  if (res.status === 401) {
    clearAuth();
    window.location.replace("/");
    throw new Error("로그인이 필요합니다.");
  }

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "요청에 실패했습니다." }));
    throw new Error(error.message);
  }

  return res.json();
}

// 타입 정의

export interface Subject {
  id: number;
  name: string;
  displayOrder: number;
  children: Subject[];
}

export type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export interface Question {
  id: number;
  subjectId: number;
  content: string;
  questionType: QuestionType;
}

export interface QuestionDetail {
  id: number;
  subjectId: number;
  content: string;
  questionType: QuestionType;
  /** MCQ일 때만 존재 */
  correctOption: number | null;
  /** SHORT_ANSWER/DESCRIPTIVE의 모범답안 */
  answer: string | null;
  /** SHORT_ANSWER alias 또는 DESCRIPTIVE 채점 키워드 */
  keywords: string[];
  explanation: string;
}

export interface SolveAnswerRequest {
  questionId: number;
  selectedOption?: number;
  answerText?: string;
}

export interface SolveRequest {
  subjectId?: number;
  mockExamId?: number;
  answers: SolveAnswerRequest[];
}

export interface SolveAnswerResponse {
  questionId: number;
  selectedOption: number;
  correctOption: number;
  correct: boolean;
}

export interface SolveResponse {
  id: number;
  subjectId: number | null;
  mockExamId: number | null;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
  answers: SolveAnswerResponse[];
  /** 풀이 제출 직후 현재 연속 학습 일수. */
  currentStreak?: number | null;
  /** 이번 풀이로 도달한 마일스톤(7·30·100·365). 없으면 null. */
  milestoneReached?: number | null;
}

export interface SolveSummaryResponse {
  id: number;
  subjectId: number | null;
  mockExamId: number | null;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
}

export interface OverallStatsResponse {
  avgDailyCount: number;
}

export interface WrongAnswerResponse {
  questionId: number;
  questionContent: string;
  subjectId: number;
  subjectName: string;
  wrongCount: number;
  lastWrongAt: string;
}

export interface WrongAnswerStatsResponse {
  subjectId: number;
  subjectName: string;
  totalSolved: number;
  wrongCount: number;
  wrongRate: number;
}

// API 함수

export function getSubjects() {
  return fetchApi<Subject[]>("/subjects");
}

export function getQuestions(subjectId: number, size = 10) {
  return fetchApi<Question[]>(`/questions?subjectId=${subjectId}&size=${size}`);
}

export function getQuestionDetail(id: number) {
  return fetchApi<QuestionDetail>(`/questions/${id}`);
}

export function submitSolve(request: SolveRequest) {
  return fetchApi<SolveResponse>("/solves", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function getSolves(opts?: { mockExamId?: number }) {
  const qs = opts?.mockExamId != null ? `?mockExamId=${opts.mockExamId}` : "";
  return fetchApi<SolveSummaryResponse[]>(`/solves${qs}`);
}

export function getSolve(id: number) {
  return fetchApi<SolveResponse>(`/solves/${id}`);
}

export function getOverallStats() {
  return fetchApi<OverallStatsResponse>("/solves/stats/overall-avg");
}

/**
 * 내 모의고사·기출 best score 맵.
 * 기출 카탈로그가 SSR + ISR 이라 회원별 점수를 클라이언트에서 별도로 머지하는 용도.
 */
export type BestScoreMap = Record<number, { correct: number; total: number }>;

export function getMyBestScores(): Promise<BestScoreMap> {
  return fetchApi<BestScoreMap>("/mock-exams/best-scores");
}

export function getWrongAnswers(subjectId?: number) {
  const params = subjectId ? `?subjectId=${subjectId}` : "";
  return fetchApi<WrongAnswerResponse[]>(`/wrong-answers${params}`);
}

export function getWrongAnswerStats() {
  return fetchApi<WrongAnswerStatsResponse[]>("/wrong-answers/stats");
}

export interface WrongAnswerRetryRequest {
  selectedOption?: number;
  answerText?: string;
}

export interface WrongAnswerRetryResponse {
  correct: boolean;
  correctOption: number | null;
  correctAnswer: string | null;
  explanation: string | null;
}

/** 오답 다시 풀기 — 정답이면 다음 조회 시 자동으로 목록에서 사라짐 */
export function retryWrongAnswer(questionId: number, body: WrongAnswerRetryRequest) {
  return fetchApi<WrongAnswerRetryResponse>(`/wrong-answers/${questionId}/retry`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

// ============================================================
// 즐겨찾기 (Bookmark)
// ============================================================

export interface BookmarkResponse {
  questionId: number;
  questionContent: string;
  subjectId: number;
  subjectName: string;
  createdAt: string;
}

/** 204/200 No-body API 호출 (JSON 파싱하지 않음) */
async function fetchApiVoid(path: string, options?: RequestInit): Promise<void> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });
  if (res.status === 401) {
    clearAuth();
    window.location.replace("/");
    throw new Error("로그인이 필요합니다.");
  }
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "요청에 실패했습니다." }));
    throw new Error(error.message);
  }
}

/** 즐겨찾기 추가 (멱등성 — 이미 있으면 no-op) */
export function addBookmark(questionId: number): Promise<void> {
  return fetchApiVoid(`/bookmarks/${questionId}`, { method: "POST" });
}

/** 즐겨찾기 제거 (없어도 성공) */
export function removeBookmark(questionId: number): Promise<void> {
  return fetchApiVoid(`/bookmarks/${questionId}`, { method: "DELETE" });
}

/** 내 즐겨찾기 목록 (최신순) */
export function getBookmarks(): Promise<BookmarkResponse[]> {
  return fetchApi<BookmarkResponse[]>(`/bookmarks`);
}

/** 특정 문제 즐겨찾기 여부 (버튼 상태 동기화) */
export function checkBookmark(questionId: number): Promise<{ bookmarked: boolean }> {
  return fetchApi<{ bookmarked: boolean }>(`/bookmarks/exists/${questionId}`);
}
