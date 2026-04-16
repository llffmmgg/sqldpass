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

export function getSolves() {
  return fetchApi<SolveSummaryResponse[]>("/solves");
}

export function getSolve(id: number) {
  return fetchApi<SolveResponse>(`/solves/${id}`);
}

export function getOverallStats() {
  return fetchApi<OverallStatsResponse>("/solves/stats/overall-avg");
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
