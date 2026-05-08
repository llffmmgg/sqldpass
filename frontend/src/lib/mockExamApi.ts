import { fetchApi } from "@/lib/api";
import { isCapacitorApp } from "@/lib/platform";
import {
  getMockExam as getMockExamFromCache,
  getQuestionsForMockExam,
  listMockExams as listMockExamsFromCache,
  type OfflineMockExam,
  type OfflineQuestion,
} from "@/lib/offlineStore";

export type ExamType = "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1" | "COMPUTER_LITERACY_2" | "ENGINEER_WRITTEN" | "ADSP";
export type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export type DifficultyLabel = "쉬움" | "보통" | "어려움" | "매우 어려움";

export type MockExamVisibility = "DRAFT" | "PUBLISHED" | "PREMIUM";

export type MockExamKind = "AI" | "PAST_EXAM";

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
  /** 전문가 검수 완료 여부 */
  expertVerified: boolean;
  /** AI 생성 / 기출 복원 */
  kind: MockExamKind;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  /** DRAFT->PUBLISHED 첫 전환 시각 (NEW 뱃지 트리거) */
  publishedAt: string | null;
  /** 기출 복원으로 승격된 시각 (NEW 뱃지 트리거) */
  pastExamLinkedAt: string | null;
  /** 본 회원이 PREMIUM 회차의 잠금을 결제로 해제했는지 (비로그인/PUBLISHED 등은 항상 false) */
  purchased: boolean;
  /** PREMIUM 회차 자동 분류 — visibility=PREMIUM 또는 정규화 난이도 ≥ 0.5 (어려움/매우 어려움) */
  isPremium: boolean;
}

export type EngineerTemplateKey =
  | "PROGRAMMING_HEAVY"
  | "THEORY_HEAVY"
  | "BALANCED"
  | "DB_HEAVY"
  | "LATEST";

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

export async function getMockExams(): Promise<MockExamSummary[]> {
  // 안드로이드 앱은 IndexedDB 캐시를 우선 사용해도 좋겠지만, 사용자별 best score 와
  // purchased 플래그가 비어 보이는 게 더 나쁜 UX 라서 일단 기존 정책 유지: 온라인이면 API.
  // 오프라인이면 (네트워크 실패 시) 캐시로 폴백.
  try {
    return await fetchApi<MockExamSummary[]>("/mock-exams");
  } catch (err) {
    if (isCapacitorApp()) {
      const fallback = await listFromCacheAsSummaries();
      if (fallback.length > 0) return fallback;
    }
    throw err;
  }
}

export async function getMockExam(id: number): Promise<MockExamDetail> {
  // 앱 모드에선 캐시 우선 (지하철 풀이 시나리오). 캐시 hit 시 즉시 반환.
  // 캐시에 없으면 (DRAFT 회차거나 신규 회차) API 시도. API 가 실패하면 명시적 에러.
  if (isCapacitorApp()) {
    const cached = await fromCacheAsDetail(id).catch(() => null);
    if (cached) return cached;
  }
  try {
    return await fetchApi<MockExamDetail>(`/mock-exams/${id}`);
  } catch (err) {
    if (isCapacitorApp()) {
      const cached = await fromCacheAsDetail(id).catch(() => null);
      if (cached) return cached;
    }
    throw err;
  }
}

async function fromCacheAsDetail(id: number): Promise<MockExamDetail | null> {
  const exam = await getMockExamFromCache(id);
  if (!exam) return null;
  const questions = await getQuestionsForMockExam(id);
  return offlineToDetail(exam, questions);
}

async function listFromCacheAsSummaries(): Promise<MockExamSummary[]> {
  const exams = await listMockExamsFromCache();
  return exams.map(offlineToSummary);
}

function offlineToDetail(
  exam: OfflineMockExam,
  questions: OfflineQuestion[],
): MockExamDetail {
  return {
    id: exam.id,
    name: exam.name,
    examType: exam.examType as ExamType,
    sequence: exam.sequence,
    totalQuestions: questions.length,
    createdAt: "",
    questions: questions.map((q) => ({
      id: q.id,
      displayOrder: q.displayOrder ?? 0,
      content: q.content,
      questionType: q.questionType as QuestionType,
      subjectId: q.subjectId ?? 0,
      subjectName: q.subjectName ?? "",
    })),
  };
}

function offlineToSummary(exam: OfflineMockExam): MockExamSummary {
  // 사용자 단위 메타(solved, bestScore, purchased)는 캐시에 없으니 보수적인 기본값.
  return {
    id: exam.id,
    name: exam.name,
    examType: exam.examType as ExamType,
    sequence: exam.sequence,
    totalQuestions: exam.questionIds.length,
    createdAt: "",
    difficultyLabel: null,
    solved: false,
    bestCorrectCount: null,
    bestTotalCount: null,
    templateKey: null,
    templateLabel: null,
    visibility: exam.visibility as MockExamVisibility,
    expertVerified: exam.expertVerified,
    kind: exam.kind as MockExamKind,
    examYear: exam.examYear,
    examRound: exam.examRound,
    examDate: exam.examDate,
    publishedAt: null,
    pastExamLinkedAt: null,
    purchased: false,
    isPremium: exam.visibility === "PREMIUM",
  };
}
