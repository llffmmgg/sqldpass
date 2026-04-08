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

async function adminFetch<T>(path: string, options?: RequestInit): Promise<T> {
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
  return res.json();
}

// Types

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface AdminStats {
  totalQuestions: number;
  totalMembers: number;
  totalSolves: number;
  todayQuestions: number;
}

export interface AdminQuestion {
  id: number;
  subjectId: number;
  subjectName: string;
  content: string;
  correctOption: number;
  explanation: string;
  summary: string | null;
  createdAt: string;
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

export function getQuestions(page = 0, size = 20, subjectId?: number) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (subjectId) params.set("subjectId", String(subjectId));
  return adminFetch<AdminQuestionPage>(`/questions?${params}`);
}

export function getQuestion(id: number) {
  return adminFetch<AdminQuestion>(`/questions/${id}`);
}

export function updateQuestion(id: number, data: { content: string; correctOption: number; explanation: string; summary: string | null }) {
  return adminFetch<AdminQuestion>(`/questions/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export function deleteQuestion(id: number) {
  return adminFetch<void>(`/questions/${id}`, { method: "DELETE" });
}

export function getMembers(page = 0, size = 20) {
  return adminFetch<AdminMemberPage>(`/members?page=${page}&size=${size}`);
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

export interface AdminMockExam {
  id: number;
  name: string;
  examType: "SQLD" | "ENGINEER_PRACTICAL";
  sequence: number;
  totalQuestions: number;
  createdAt: string;
  difficultyLabel: "쉬움" | "보통" | "어려움" | "매우 어려움" | null;
}

export function getAdminMockExams() {
  return adminFetch<AdminMockExam[]>("/mock-exams");
}

export type CreateMockExamType = "SQLD" | "ENGINEER_PRACTICAL";

export function createMockExam(examType: CreateMockExamType = "SQLD") {
  return adminFetch<AdminMockExam>("/mock-exams", {
    method: "POST",
    body: JSON.stringify({ examType }),
  });
}

export function deleteMockExam(id: number) {
  return adminFetch<void>(`/mock-exams/${id}`, { method: "DELETE" });
}
