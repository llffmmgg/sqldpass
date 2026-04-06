import { fetchApi } from "@/lib/api";

export type ExamType = "SQLD" | "ENGINEER_PRACTICAL";
export type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export interface MockExamSummary {
  id: number;
  name: string;
  examType: ExamType;
  sequence: number;
  totalQuestions: number;
  createdAt: string;
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
