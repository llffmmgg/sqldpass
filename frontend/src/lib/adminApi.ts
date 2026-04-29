const BASE = "/api/admin";

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("admin_token");
}

export function setToken(token: string) {
  localStorage.setItem("admin_token", token);
}

export function clearToken() {
  localStorage.removeItem("admin_token");
}

export function isAuthenticated(): boolean {
  return !!getToken();
}

export async function adminFetch<T>(path: string, options?: RequestInit): Promise<T> {
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
    clearToken();
    window.location.href = "/admin/login";
    throw new Error("인증이 만료되었습니다.");
  }

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "요청에 실패했습니다." }));
    throw new Error(error.message);
  }

  if (res.status === 204 || res.status === 202) return undefined as T;
  // 200 인데 빈 body 인 경우 (Spring 의 void 반환) — json 파싱 시도하면 SyntaxError
  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return undefined as T;
  }
}

// Types

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface SubjectSolveStats {
  subjectId: number;
  subjectName: string;
  uniqueUsers: number;
  solveCount: number;
  totalQuestions: number;
}

export interface AdminActivityBucket {
  totalSolves: number;
  totalQuestions: number;
  uniqueMembers: number;
  todaySolves: number;
  todayQuestions: number;
  todayUniqueMembers: number;
}

export interface AdminCertActivity {
  certSlug: string;
  certName: string;
  mockExam: AdminActivityBucket;
  pastExam: AdminActivityBucket;
}

export interface AdminStats {
  totalQuestions: number;
  verifiedQuestions: number;
  unverifiedQuestions: number;
  totalMembers: number;
  totalSolves: number;
  totalAnonymousSolves: number;
  todayQuestions: number;
  todayMembers?: number;
  todaySolves?: number;
  todayAnonymousSolves?: number;
  subjectStats: SubjectSolveStats[];
  /** 자격증별 풀이 활동 (모의고사 / 기출 복원 분리, 누적+오늘자) */
  certActivity?: AdminCertActivity[];
}

export type AdminQuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export interface AdminQuestion {
  id: number;
  subjectId: number;
  subjectName: string;
  content: string;
  questionType: AdminQuestionType;
  correctOption: number | null;
  answer: string | null;
  keywords: string[] | null;
  explanation: string;
  summary: string | null;
  createdAt: string;
  verifiedAt: string | null;
  verificationCategory: string | null;
}

export interface AdminQuestionUpdatePayload {
  content: string;
  questionType: AdminQuestionType;
  correctOption: number | null;
  answer: string | null;
  keywords: string[] | null;
  explanation: string;
  summary: string | null;
}

export interface AdminQuestionPage {
  content: AdminQuestion[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AdminMember {
  id: number;
  provider: string;
  nickname: string;
  email: string | null;
  createdAt: string;
  totalSolved: number;
  totalCorrect: number;
  activeDays: number;
  streakDays: number;
}

export interface AdminMemberPage {
  content: AdminMember[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface GenerationResult {
  totalGenerated: number;
  totalVerified: number;
  totalSaved: number;
  errors: string[];
}

// API functions

export function login(request: LoginRequest) {
  return adminFetch<LoginResponse>("/login", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function getStats() {
  return adminFetch<AdminStats>("/stats");
}

export interface AdminTrendPoint {
  date: string;
  newMembers: number;
  newSolves: number;
}

export interface AdminTrend {
  days: number;
  points: AdminTrendPoint[];
}

export function getTrend(days: number) {
  return adminFetch<AdminTrend>(`/stats/trend?days=${days}`);
}

export function getQuestions(page = 0, size = 20, subjectId?: number) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (subjectId) params.set("subjectId", String(subjectId));
  return adminFetch<AdminQuestionPage>(`/questions?${params}`);
}

export function getQuestion(id: number) {
  return adminFetch<AdminQuestion>(`/questions/${id}`);
}

export function updateQuestion(id: number, data: AdminQuestionUpdatePayload) {
  return adminFetch<AdminQuestion>(`/questions/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export function deleteQuestion(id: number) {
  return adminFetch<void>(`/questions/${id}`, { method: "DELETE" });
}

export interface QuestionVerifyResult {
  questionId: number;
  subjectName: string;
  summary: string | null;
  reason: string;
}

export type VerificationExamType = "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1" | "COMPUTER_LITERACY_2" | "ENGINEER_WRITTEN" | "ADSP";

export interface QuestionVerifyHistory {
  runId: number;
  examType: VerificationExamType | null;
  subjectId: number | null;
  subjectName: string | null;
  limitRequested: number;
  forceRecheck: boolean;
  processedCount: number;
  suspiciousCount: number;
  fixedCount: number;
  unfixableCount: number;
  errorCount: number;
  completedAt: string;
}

export interface QuestionVerifyRun {
  examType: VerificationExamType | null;
  subjectId: number | null;
  subjectName: string | null;
  requestedLimit: number;
  forceRecheck: boolean;
  processedCount: number;
  suspiciousCount: number;
  fixedCount: number;
  unfixableCount: number;
  errorCount: number;
  completedAt: string;
  suspiciousQuestions: QuestionVerifyResult[];
  recentRuns: QuestionVerifyHistory[];
  markdownByBucket: {
    unfixable: string;
    fixed: string;
    error: string;
  };
}

export type VerifyBucket = "unfixable" | "fixed" | "error";

export function downloadBucketMarkdown(
  run: QuestionVerifyRun,
  bucket: VerifyBucket,
) {
  const md = run.markdownByBucket?.[bucket];
  if (!md || md.trim().length === 0) return;
  const blob = new Blob([md], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  const ts = run.completedAt.replace(/[:.]/g, "-");
  a.download = `${bucket}-${ts}.md`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export interface VerifyAllQuestionsParams {
  limit: number;
  subjectId?: number;
  examType?: VerificationExamType;
  mockExamId?: number;
  force?: boolean;
}

export function verifyAllQuestions(params: VerifyAllQuestionsParams) {
  const query = new URLSearchParams({ limit: String(params.limit) });
  if (params.subjectId) query.set("subjectId", String(params.subjectId));
  if (params.examType) query.set("examType", params.examType);
  if (params.mockExamId) query.set("mockExamId", String(params.mockExamId));
  if (params.force) query.set("force", "true");
  return adminFetch<QuestionVerifyRun>(`/questions/verify?${query}`, { method: "POST" });
}

export function getQuestionVerifyHistory(limit = 5) {
  return adminFetch<QuestionVerifyHistory[]>(`/questions/verify/history?limit=${limit}`);
}

export type VerificationCategory = "AUTO_FIXED" | "MANUAL_REVIEW" | "ERROR";

export interface VerificationIssue {
  id: number;
  subjectName: string | null;
  summary: string | null;
  contentPreview: string | null;
  category: VerificationCategory;
}

export interface VerificationIssuePage {
  content: VerificationIssue[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export function getVerifyIssues(category: VerificationCategory, page = 0, size = 20) {
  const params = new URLSearchParams({ category, page: String(page), size: String(size) });
  return adminFetch<VerificationIssuePage>(`/questions/verify/issues?${params}`);
}

export function getVerifyIssueCounts() {
  return adminFetch<Record<VerificationCategory, number>>("/questions/verify/issues/counts");
}

export type AdminMemberSort =
  | "default"
  | "totalSolved"
  | "totalCorrect"
  | "activeDays"
  | "streakDays";
export type AdminMemberOrder = "asc" | "desc";

export function getMembers(
  page = 0,
  size = 20,
  sort: AdminMemberSort = "default",
  order: AdminMemberOrder = "desc",
) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort,
    order,
  });
  return adminFetch<AdminMemberPage>(`/members?${params.toString()}`);
}

// 어드민 - 유저 대시보드

export interface AdminMemberDashboard {
  member: {
    id: number;
    nickname: string;
    provider: string;
    createdAt: string;
  };
  stats: {
    totalSolved: number;
    totalCorrect: number;
    overallRate: number;
    streakDays: number;
    totalSessions: number;
  };
  recentActivity: { date: string; count: number }[];
  subjectStats: {
    subjectId: number;
    subjectName: string;
    total: number;
    correct: number;
    rate: number;
  }[];
  weakSubjects: {
    subjectId: number;
    subjectName: string;
    wrongCount: number;
    wrongRate: number;
  }[];
  recentSolves: {
    id: number;
    solvedAt: string;
    totalCount: number;
    correctCount: number;
    subjectId: number | null;
    mockExamId: number | null;
  }[];
}

export function getMemberDashboard(memberId: number) {
  return adminFetch<AdminMemberDashboard>(`/members/${memberId}/dashboard`);
}

// 어드민 - 피드백

export type FeedbackType = "QUESTION_ERROR" | "BUG" | "FEATURE" | "OTHER";
export type FeedbackStatus = "NEW" | "REVIEWED" | "RESOLVED" | "WONTFIX";

export interface AdminFeedback {
  id: number;
  type: FeedbackType;
  memberId: number;
  memberNickname: string | null;
  questionId: number | null;
  content: string;
  pageUrl: string | null;
  status: FeedbackStatus;
  adminReply: string | null;
  repliedAt: string | null;
  createdAt: string;
}

export interface AdminFeedbackPage {
  content: AdminFeedback[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export function getFeedbacks(status: FeedbackStatus | "ALL", page = 0, size = 20) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status !== "ALL") params.set("status", status);
  return adminFetch<AdminFeedbackPage>(`/feedback?${params}`);
}

export function updateFeedbackStatus(id: number, status: FeedbackStatus) {
  return adminFetch<AdminFeedback>(`/feedback/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

/** 어드민 답변 작성 — 자동 RESOLVED 처리 + 작성자에게 알림 발송 */
export function replyFeedback(id: number, reply: string) {
  return adminFetch<AdminFeedback>(`/feedback/${id}/reply`, {
    method: "PATCH",
    body: JSON.stringify({ reply }),
  });
}

// 어드민 - 공지사항

export type NoticeDisplayType = "BANNER" | "MODAL";

export interface AdminNotice {
  id: number;
  displayType: NoticeDisplayType;
  title: string | null;
  body: string;
  active: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface NoticePayload {
  displayType: NoticeDisplayType;
  title: string | null;
  body: string;
  active: boolean;
}

export function listNotices() {
  return adminFetch<AdminNotice[]>("/notices");
}

export function createNotice(payload: NoticePayload) {
  return adminFetch<AdminNotice>("/notices", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateNotice(id: number, payload: NoticePayload) {
  return adminFetch<AdminNotice>(`/notices/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function setNoticeActive(id: number, active: boolean) {
  return adminFetch<AdminNotice>(`/notices/${id}/active`, {
    method: "PATCH",
    body: JSON.stringify({ active }),
  });
}

export function deleteNotice(id: number) {
  return adminFetch<void>(`/notices/${id}`, { method: "DELETE" });
}

export interface GenerationStatus {
  status: "IDLE" | "RUNNING" | "COMPLETED" | "FAILED";
  result: string | null;
  startedAt: string | null;
}

export function getGenerationStatus() {
  return adminFetch<GenerationStatus>("/generate/status");
}

export function generateQuestions(count = 3) {
  return adminFetch<void>(`/generate?count=${count}`, { method: "POST" });
}

export function resetGeneration() {
  return adminFetch<void>("/generate/reset", { method: "POST" });
}

// 모의고사 관리

export type EngineerTemplate =
  | "PROGRAMMING_HEAVY"
  | "THEORY_HEAVY"
  | "BALANCED"
  | "DB_HEAVY"
  | "LATEST";

export const ENGINEER_TEMPLATE_LABEL: Record<EngineerTemplate, string> = {
  PROGRAMMING_HEAVY: "프로그래밍 편중형",
  THEORY_HEAVY: "이론 편중형",
  BALANCED: "균형형",
  DB_HEAVY: "DB 강조형",
  LATEST: "최신 기출 분포 반영",
};

export type MockExamVisibility = "DRAFT" | "PUBLISHED" | "PREMIUM";

export type MockExamKind = "AI" | "PAST_EXAM";

export interface AdminMockExam {
  id: number;
  name: string;
  examType: "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1" | "COMPUTER_LITERACY_2" | "ENGINEER_WRITTEN" | "ADSP";
  sequence: number;
  totalQuestions: number;
  createdAt: string;
  difficultyLabel: "쉬움" | "보통" | "어려움" | "매우 어려움" | null;
  templateKey: EngineerTemplate | null;
  templateLabel: string | null;
  visibility: MockExamVisibility;
  expertVerified: boolean;
  kind: MockExamKind;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
}

export function changeMockExamVisibility(id: number, visibility: MockExamVisibility) {
  return adminFetch<AdminMockExam>(`/mock-exams/${id}/visibility`, {
    method: "PATCH",
    body: JSON.stringify({ visibility }),
  });
}

export function getAdminMockExams() {
  return adminFetch<AdminMockExam[]>("/mock-exams");
}

export interface AdminMockExamDetail {
  id: number;
  name: string;
  examType: string;
  sequence: number;
  totalQuestions: number;
  createdAt: string;
  expertVerified: boolean;
  kind: MockExamKind;
  examYear: number | null;
  examRound: number | null;
  examDate: string | null;
  questions: {
    id: number;
    displayOrder: number;
    content: string;
    questionType: string;
    subjectId: number;
    subjectName: string;
  }[];
}

export function getAdminMockExamDetail(id: number) {
  return adminFetch<AdminMockExamDetail>(`/mock-exams/${id}`);
}

export function markMockExamVerified(id: number) {
  return adminFetch<{ marked: number }>(`/mock-exams/${id}/mark-verified`, { method: "POST" });
}

export function toggleExpertVerified(id: number) {
  return adminFetch<{ expertVerified: boolean }>(`/mock-exams/${id}/toggle-expert-verified`, { method: "POST" });
}

export interface SetPastExamMetaPayload {
  promote: boolean;
  examYear?: number | null;
  examRound?: number | null;
  examDate?: string | null;
}

export function setPastExamMeta(id: number, payload: SetPastExamMetaPayload) {
  return adminFetch<AdminMockExam>(`/mock-exams/${id}/past-exam-meta`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export interface AdminSolveDetail {
  solveId: number;
  memberId: number;
  memberNickname: string;
  mockExamId: number | null;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
  answers: AdminSolveAnswerDetail[];
}

export interface AdminSolveAnswerDetail {
  questionId: number;
  questionContent: string;
  subjectName: string;
  questionType: string;
  selectedOption: number | null;
  correctOption: number | null;
  userAnswerText: string | null;
  correctAnswer: string | null;
  correct: boolean;
  explanation: string;
}

export function getAdminSolveDetail(solveId: number) {
  return adminFetch<AdminSolveDetail>(`/solves/${solveId}`);
}

export type CreateMockExamType = "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1" | "COMPUTER_LITERACY_2" | "ENGINEER_WRITTEN" | "ADSP";

/** 생성 시 난이도는 SQLD, 정처기 실기, 컴활 1급 모두에 적용된다. */
export type MockExamCreationDifficulty = "EASY" | "NORMAL" | "HARD" | "VERY_HARD";

export function createMockExam(
  examType: CreateMockExamType = "SQLD",
  difficulty?: MockExamCreationDifficulty,
  engineerTemplate?: EngineerTemplate | null,
) {
  return adminFetch<AdminMockExam>("/mock-exams", {
    method: "POST",
    body: JSON.stringify({ examType, difficulty, engineerTemplate: engineerTemplate ?? null }),
  });
}

export function deleteMockExam(id: number) {
  return adminFetch<void>(`/mock-exams/${id}`, { method: "DELETE" });
}

// ----------------------------------------------------------
// 어드민 수동 모의고사 등록 (JSON 한 통으로 메타 + 문제 N개)
// ----------------------------------------------------------

export type ManualQuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export interface ManualMockExamQuestion {
  subjectId: number;
  content: string;
  /** 생략 시 MCQ */
  questionType?: ManualQuestionType;
  /** MCQ 일 때 1~4 필수 */
  correctOption?: number;
  /** SHORT_ANSWER / DESCRIPTIVE 일 때 필수 */
  answer?: string;
  keywords?: string[];
  explanation: string;
  summary?: string;
  topic?: string;
  difficulty?: number;
}

export interface ManualMockExamPayload {
  name: string;
  examType: CreateMockExamType;
  /** true 면 PAST_EXAM 으로 승격 */
  pastExam?: boolean;
  examYear?: number;
  examRound?: number;
  /** YYYY-MM-DD */
  examDate?: string;
  /** true 면 등록과 동시에 전문가 검수 완료 처리 */
  expertVerified?: boolean;
  questions: ManualMockExamQuestion[];
}

export function createManualMockExam(payload: ManualMockExamPayload) {
  return adminFetch<AdminMockExam>("/mock-exams/manual", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

// ----------------------------------------------------------
// LLM 검증용 Markdown export
// ----------------------------------------------------------

export type ExportExamType = "SQLD" | "ENGINEER_PRACTICAL" | "COMPUTER_LITERACY_1" | "COMPUTER_LITERACY_2" | "ENGINEER_WRITTEN" | "ADSP";

/**
 * 문제를 .md 파일로 다운로드. 다운로드 즉시 백엔드에서 export 마크가 찍힘.
 * - force=false: 미export(신규) 문제만
 * - force=true: 이미 마크된 것까지 포함
 *
 * @returns 다운로드된 문제 수 (X-Export-Count 헤더)
 */
export async function exportQuestions(examType: ExportExamType, force: boolean = false): Promise<number> {
  const token = getToken();
  const url = `${BASE}/questions/export?examType=${examType}&force=${force}`;
  const res = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (res.status === 401) {
    clearToken();
    window.location.href = "/admin/login";
    throw new Error("인증이 만료되었습니다.");
  }
  if (!res.ok) {
    throw new Error(`다운로드 실패: ${res.status}`);
  }

  const blob = await res.blob();
  const cdHeader = res.headers.get("Content-Disposition") ?? "";
  const match = cdHeader.match(/filename="?([^";]+)"?/);
  const filename = match?.[1] ?? `sqldpass-${examType.toLowerCase()}.md`;
  const count = parseInt(res.headers.get("X-Export-Count") ?? "0", 10);

  const a = document.createElement("a");
  const objectUrl = URL.createObjectURL(blob);
  a.href = objectUrl;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(objectUrl);

  return count;
}

/** 지정 examType의 export 마크 일괄 리셋. 리셋된 행 수 반환. */
export async function resetExportMark(examType: ExportExamType): Promise<number> {
  const data = await adminFetch<{ reset: number }>(
    `/questions/export/reset?examType=${examType}`,
    { method: "POST" },
  );
  return data.reset;
}
