import { fetchApi } from "@/lib/api";

export type ExamType = "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1";
export type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export type DifficultyLabel = "쉬움" | "보통" | "어려움" | "매우 어려움";

export interface MockExamSummary {
  id: number;
  name: string;
  examType: ExamType;
  sequence: number;
  totalQuestions: number;
  createdAt: string;
  /** 백엔드 정규화 라벨 (null=데이터 없음) */
  difficultyLabel: DifficultyLabel | null;
  /** 로그인 사용자가 한 번이라도 풀었는지 (비로그인 false) */
  solved: boolean;
  /** 사용자의 최고 정답 수 (미풀이/비로그인 시 null) */
  bestCorrectCount: number | null;
  /** 사용자의 최고 풀이 시 총 문항 수 (미풀이/비로그인 시 null) */
  bestTotalCount: number | null;
}

export interface MockExamQuestion {
  id: number;
  displayOrder: number;
  content: string;
  questionType: QuestionType;
  subjectId: number;
  subjectName: string;
}

export interface MockExamDetail {
  id: number;
  name: string;
  examType: ExamType;
  sequence: number;
  totalQuestions: number;
  createdAt: string;
  questions: MockExamQuestion[];
}

export function getMockExams() {
  return fetchApi<MockExamSummary[]>("/mock-exams");
}

export function getMockExam(id: number) {
  return fetchApi<MockExamDetail>(`/mock-exams/${id}`);
}
