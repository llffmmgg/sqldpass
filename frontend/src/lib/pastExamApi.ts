/**
 * 기출 복원(past-exams) — 비로그인 공개 API 래퍼.
 * /api/public/past-exams/* 로 토큰 없이 접근한다.
 */

import type { ExamType, QuestionType } from "@/lib/mockExamApi";
import { getToken } from "@/lib/auth";

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
  solved: boolean;
  bestCorrectCount: number | null;
  bestTotalCount: number | null;
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

export interface PastExamSubjectScore {
  subjectName: string;
  total: number;
  correct: number;
  /** 정답률 0~100, 소수 한자리 */
  rate: number;
  /** 100점 만점 환산 (정답률 백분율과 동일) */
  weighted: number;
  /** 자격증의 과목별 과락 컷에 미달했는지 (단일 과목 자격증에선 false) */
  failed: boolean;
}

export interface PastExamGradeResponse {
  totalCount: number;
  correctCount: number;
  score: number;
  items: PastExamGradedItem[];
  /** solve 테이블에 적재된 풀이 id — /history/{id} 진입에 사용. solve 저장 실패 시 null. */
  solveId: number | null;
  /** 합격 기준 과목 단위 정답률 / 과락 표시 */
  subjectScores: PastExamSubjectScore[];
  /** 자격증별 공식 합격 기준 적용한 최종 합격 여부 */
  passed: boolean;
  /** 합격/불합격 한 줄 요약 (UI 배너) */
  passReason: string;
}

async function publicFetch<T>(path: string, init?: RequestInit): Promise<T> {
  // 로그인 사용자는 Authorization 헤더를 선택적으로 붙여 서버가 memberId 주입 가능하게 한다.
  const token = getToken();
  const authHeaders: Record<string, string> = token
    ? { Authorization: `Bearer ${token}` }
    : {};

  const res = await fetch(`/api/public${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders,
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
