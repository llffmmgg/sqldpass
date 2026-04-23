/**
 * 기출 복원(past-exams) — 비로그인 공개 API 래퍼.
 * /api/public/past-exams/* 로 토큰 없이 접근한다.
 */

import type { ExamType, QuestionType } from "@/lib/mockExamApi";

export interface PastExamSummary {
  id: number;
  name: string;
  examType: ExamType;
  certSlug: string;
  totalQuestions: number;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  expertVerified: boolean;
  createdAt: string;
}

export interface PastExamQuestion {
  id: number;
  displayOrder: number;
  content: string;
  questionType: QuestionType;
  subjectId: number;
  subjectName: string;
}

export interface PastExamDetail {
  id: number;
  name: string;
  examType: ExamType;
  certSlug: string;
  totalQuestions: number;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  expertVerified: boolean;
  questions: PastExamQuestion[];
}

export interface PastExamAnswerPayload {
  questionId: number;
  selectedOption?: number | null;
  answerText?: string | null;
}

export interface PastExamGradedItem {
  questionId: number;
  correct: boolean;
  partialScore: number;
  selectedOption: number | null;
  submittedAnswerText: string | null;
  correctOption: number | null;
  answer: string | null;
  keywords: string[];
  explanation: string | null;
}

export interface PastExamGradeResponse {
  totalCount: number;
  correctCount: number;
  score: number;
  items: PastExamGradedItem[];
}

async function publicFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api/public${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || `public API ${path} failed: ${res.status}`);
  }
  // 응답 없는 POST 대응
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export function listPastExams(certSlug: string) {
  return publicFetch<PastExamSummary[]>(
    `/past-exams?cert=${encodeURIComponent(certSlug)}`,
  );
}

export function getPastExam(id: number) {
  return publicFetch<PastExamDetail>(`/past-exams/${id}`);
}

export function gradePastExam(
  id: number,
  answers: PastExamAnswerPayload[],
) {
  return publicFetch<PastExamGradeResponse>(`/past-exams/${id}/grade`, {
    method: "POST",
    body: JSON.stringify({ answers }),
  });
}
