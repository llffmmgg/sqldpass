import { fetchApi } from "@/lib/api";

export type ExamType = "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1" | "ENGINEER_WRITTEN";
export type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export type DifficultyLabel = "쉬움" | "보통" | "어려움" | "매우 어려움";

export type MockExamVisibility = "DRAFT" | "PUBLISHED" | "PREMIUM";

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
  /** 정처기 실기 분포 템플릿 키 (다른 시험/구 정처기는 null) */
  templateKey: EngineerTemplateKey | null;
  /** 정처기 분포 템플릿 한글 라벨 */
  templateLabel: string | null;
  /** 공개 상태 — 백엔드 enum (사용자 목록은 PUBLISHED/PREMIUM만, 어드민은 DRAFT 포함) */
  visibility: MockExamVisibility;
  /** 전문가 검증 완료 여부 */
  expertVerified: boolean;
}

export type EngineerTemplateKey =
  | "PROGRAMMING_HEAVY"
  | "THEORY_HEAVY"
  | "BALANCED"
  | "DB_HEAVY";

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
