import { fetchApi } from "@/lib/api";

export interface MockExamSummary {
  id: number;
  name: string;
  sequence: number;
  totalQuestions: number;
  createdAt: string;
}

export interface MockExamQuestion {
  id: number;
  displayOrder: number;
  content: string;
  subjectId: number;
  subjectName: string;
}

export interface MockExamDetail {
  id: number;
  name: string;
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
