import { getToken } from "@/lib/auth";

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

export interface Question {
  id: number;
  subjectId: number;
  content: string;
}

export interface QuestionDetail {
  id: number;
  subjectId: number;
  content: string;
  correctOption: number;
  explanation: string;
}

export interface SolveAnswerRequest {
  questionId: number;
  selectedOption: number;
}

export interface SolveRequest {
  subjectId: number;
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
  subjectId: number;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
  answers: SolveAnswerResponse[];
}

export interface SolveSummaryResponse {
  id: number;
  subjectId: number;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
}

export interface WrongAnswerResponse {
  questionId: number;
  questionContent: string;
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

export function getWrongAnswers(subjectId?: number) {
  const params = subjectId ? `?subjectId=${subjectId}` : "";
  return fetchApi<WrongAnswerResponse[]>(`/wrong-answers${params}`);
}

export function getWrongAnswerStats() {
  return fetchApi<WrongAnswerStatsResponse[]>("/wrong-answers/stats");
}
