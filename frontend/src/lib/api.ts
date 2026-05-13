import { getToken, clearAuth } from "@/lib/auth";
import { isCapacitorApp } from "@/lib/platform";

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

  if (res.status === 401) {
    // 토큰이 있을 때만 = 만료된 세션 → 정리하고 홈으로.
    // 토큰 없이 401 = 비로그인 페이지가 인증 API 를 우연히 건드린 케이스.
    // 이 경우 호출자가 catch 로 fallback 하도록 단순 throw 만 한다.
    if (token) {
      clearAuth();
      window.location.replace("/");
    }
    throw new Error("로그인이 필요합니다.");
  }

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

export type QuestionType = "MCQ" | "SHORT_ANSWER" | "DESCRIPTIVE";

export interface Question {
  id: number;
  subjectId: number;
  content: string;
  questionType: QuestionType;
}

export interface QuestionDetail {
  id: number;
  subjectId: number;
  content: string;
  questionType: QuestionType;
  /** MCQ일 때만 존재 */
  correctOption: number | null;
  /** SHORT_ANSWER/DESCRIPTIVE의 모범답안 */
  answer: string | null;
  /** SHORT_ANSWER alias 또는 DESCRIPTIVE 채점 키워드 */
  keywords: string[];
  explanation: string;
}

export interface SolveAnswerRequest {
  questionId: number;
  selectedOption?: number;
  answerText?: string;
}

export interface SolveRequest {
  subjectId?: number;
  mockExamId?: number;
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
  subjectId: number | null;
  mockExamId: number | null;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
  answers: SolveAnswerResponse[];
  /** 풀이 제출 직후 현재 연속 학습 일수. */
  currentStreak?: number | null;
  /** 이번 풀이로 도달한 마일스톤(7·30·100·365). 없으면 null. */
  milestoneReached?: number | null;
}

export interface SolveSummaryResponse {
  id: number;
  subjectId: number | null;
  mockExamId: number | null;
  totalCount: number;
  correctCount: number;
  score: number;
  solvedAt: string;
}

export interface OverallStatsResponse {
  avgDailyCount: number;
}

export interface WrongAnswerResponse {
  questionId: number;
  questionContent: string;
  subjectId: number;
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

export async function submitSolve(request: SolveRequest): Promise<SolveResponse> {
  try {
    return await fetchApi<SolveResponse>("/solves", {
      method: "POST",
      body: JSON.stringify(request),
    });
  } catch (err) {
    // 오프라인 + 안드로이드 앱 + 모의고사(회차) 제출이면 IndexedDB 큐로 fallback.
    // 네트워크 단절은 fetch 가 TypeError 로 던지고, OS 가 미리 알면 navigator.onLine=false.
    const offlineLike =
      err instanceof TypeError ||
      (typeof navigator !== "undefined" && navigator.onLine === false);
    if (offlineLike && isCapacitorApp() && request.mockExamId != null) {
      const { submitSolveOffline } = await import("@/lib/solveOffline");
      return submitSolveOffline(request);
    }
    throw err;
  }
}

export function getSolves(opts?: { mockExamId?: number }) {
  const qs = opts?.mockExamId != null ? `?mockExamId=${opts.mockExamId}` : "";
  return fetchApi<SolveSummaryResponse[]>(`/solves${qs}`);
}

export function getSolve(id: number) {
  return fetchApi<SolveResponse>(`/solves/${id}`);
}

export function getOverallStats() {
  return fetchApi<OverallStatsResponse>("/solves/stats/overall-avg");
}

/**
 * 내 모의고사·기출 best score 맵.
 * 기출 카탈로그가 SSR + ISR 이라 회원별 점수를 클라이언트에서 별도로 머지하는 용도.
 */
export type BestScoreMap = Record<number, { correct: number; total: number }>;

export function getMyBestScores(): Promise<BestScoreMap> {
  return fetchApi<BestScoreMap>("/mock-exams/best-scores");
}

export function getWrongAnswers(subjectId?: number) {
  const params = subjectId ? `?subjectId=${subjectId}` : "";
  return fetchApi<WrongAnswerResponse[]>(`/wrong-answers${params}`);
}

export function getWrongAnswerStats() {
  return fetchApi<WrongAnswerStatsResponse[]>("/wrong-answers/stats");
}

export interface WrongAnswerRetryRequest {
  selectedOption?: number;
  answerText?: string;
}

export interface WrongAnswerRetryResponse {
  correct: boolean;
  correctOption: number | null;
  correctAnswer: string | null;
  explanation: string | null;
}

/** 오답 다시 풀기 — 정답이면 다음 조회 시 자동으로 목록에서 사라짐 */
export function retryWrongAnswer(questionId: number, body: WrongAnswerRetryRequest) {
  return fetchApi<WrongAnswerRetryResponse>(`/wrong-answers/${questionId}/retry`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

// ============================================================
// 즐겨찾기 (Bookmark)
// ============================================================

export interface BookmarkResponse {
  questionId: number;
  questionContent: string;
  subjectId: number;
  subjectName: string;
  createdAt: string;
}

/**
 * 즐겨찾기 목록 응답 — 권한 별 표시 제한 메타 포함.
 * - hasLibraryAccess 권한이 있으면 items 전체 반환, limited=false
 * - 무료/Starter 등 권한 없으면 items 최근 30개만, totalCount 가 30 초과면 limited=true
 */
export interface BookmarkListResponse {
  items: BookmarkResponse[];
  totalCount: number;
  limited: boolean;
  freeLimit: number;
}

/** 204/200 No-body API 호출 (JSON 파싱하지 않음) */
async function fetchApiVoid(path: string, options?: RequestInit): Promise<void> {
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
    if (token) {
      clearAuth();
      window.location.replace("/");
    }
    throw new Error("로그인이 필요합니다.");
  }
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "요청에 실패했습니다." }));
    throw new Error(error.message);
  }
}

/** 즐겨찾기 추가 (멱등성 — 이미 있으면 no-op) */
export function addBookmark(questionId: number): Promise<void> {
  return fetchApiVoid(`/bookmarks/${questionId}`, { method: "POST" });
}

/** 즐겨찾기 제거 (없어도 성공) */
export function removeBookmark(questionId: number): Promise<void> {
  return fetchApiVoid(`/bookmarks/${questionId}`, { method: "DELETE" });
}

/**
 * 내 즐겨찾기 목록 (최신순). 무료/Starter 사용자는 응답에서 자동으로 최근 30개로 잘림.
 * 31번째 이상은 백엔드에 보존되며 결제 후 즉시 복원.
 */
export function getBookmarks(): Promise<BookmarkListResponse> {
  return fetchApi<BookmarkListResponse>(`/bookmarks`);
}

/**
 * 오답노트 잠금 화면 미리보기 — 권한이 없는 사용자가 본인 오답 상위 N개를 블러 처리해 보기 위한 응답.
 * 제목·과목만 반환. 정답/해설은 응답에서 제외.
 */
export interface WrongAnswerPreviewResponse {
  questionId: number;
  questionContent: string;
  subjectName: string;
}

export function getWrongAnswersPreview(limit: number = 5): Promise<WrongAnswerPreviewResponse[]> {
  return fetchApi<WrongAnswerPreviewResponse[]>(`/wrong-answers/preview?limit=${limit}`);
}

/** 특정 문제 즐겨찾기 여부 (버튼 상태 동기화) */
export function checkBookmark(questionId: number): Promise<{ bookmarked: boolean }> {
  return fetchApi<{ bookmarked: boolean }>(`/bookmarks/exists/${questionId}`);
}

// ============================================================
// 게시판 (Post / Comment)
// ============================================================

export type PostCategory = "PASS_REVIEW";
export type PostStatus = "PENDING" | "PUBLISHED";

export interface PostSummary {
  id: number;
  category: PostCategory;
  status: PostStatus;
  cert: string | null; // ExamType
  title: string;
  viewCount: number;
  commentCount: number;
  authorNickname: string;
  createdAt: string;
}

export interface PostComment {
  id: number;
  content: string;
  authorNickname: string;
  authorId: number;
  createdAt: string;
}

export interface PostDetail {
  id: number;
  category: PostCategory;
  status: PostStatus;
  cert: string | null;
  title: string;
  content: string;
  viewCount: number;
  authorNickname: string;
  authorId: number;
  createdAt: string;
  updatedAt: string;
  comments: PostComment[];
}

export interface PostPage {
  items: PostSummary[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface PostSubmitRequest {
  category: PostCategory;
  cert?: string | null;
  title: string;
  content: string;
}

export interface PostEditRequest {
  title: string;
  content: string;
}

export function listPosts(opts?: {
  category?: PostCategory;
  cert?: string;
  page?: number;
  size?: number;
}): Promise<PostPage> {
  const params = new URLSearchParams();
  if (opts?.category) params.set("category", opts.category);
  if (opts?.cert) params.set("cert", opts.cert);
  if (opts?.page != null) params.set("page", String(opts.page));
  if (opts?.size != null) params.set("size", String(opts.size));
  const qs = params.toString();
  return fetchApi<PostPage>(`/posts${qs ? `?${qs}` : ""}`);
}

export function getPost(id: number): Promise<PostDetail> {
  return fetchApi<PostDetail>(`/posts/${id}`);
}

export function submitPost(body: PostSubmitRequest): Promise<number> {
  return fetchApi<number>(`/posts`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function editPost(id: number, body: PostEditRequest): Promise<void> {
  return fetchApiVoid(`/posts/${id}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function deletePost(id: number): Promise<void> {
  return fetchApiVoid(`/posts/${id}`, { method: "DELETE" });
}

export function addComment(postId: number, content: string): Promise<PostComment> {
  return fetchApi<PostComment>(`/posts/${postId}/comments`, {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export function deleteComment(commentId: number): Promise<void> {
  return fetchApiVoid(`/posts/comments/${commentId}`, { method: "DELETE" });
}

// ----- 이미지 업로드 (Cloudflare R2 presigned PUT) -----
export interface PresignedUpload {
  uploadUrl: string;
  publicUrl: string;
  key: string;
  maxBytes: number;
}

/** 이미지 업로드 presigned URL 발급 + R2 직접 PUT 후 public URL 반환. */
export async function uploadImage(file: File): Promise<string> {
  if (!file.type.startsWith("image/")) {
    throw new Error("이미지 파일만 업로드할 수 있습니다.");
  }
  const presigned = await fetchApi<PresignedUpload>(`/uploads/image-url`, {
    method: "POST",
    body: JSON.stringify({ contentType: file.type }),
  });
  if (file.size > presigned.maxBytes) {
    throw new Error(
      `파일 크기가 한도(${Math.floor(presigned.maxBytes / 1024 / 1024)}MB)를 초과합니다.`,
    );
  }
  const putRes = await fetch(presigned.uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": file.type },
    body: file,
  });
  if (!putRes.ok) {
    throw new Error(`업로드에 실패했습니다 (${putRes.status})`);
  }
  return presigned.publicUrl;
}

// ===== 어드민 =====
// 어드민 토큰(admin_token)을 쓰므로 adminFetch 사용. fetchApi 의 401 핸들러는
// 사용자 토큰을 클리어하고 / 로 보내버려 어드민 흐름이 망가짐.
import { adminFetch } from "@/lib/adminApi";

export function adminListPendingPosts(): Promise<PostSummary[]> {
  return adminFetch<PostSummary[]>(`/posts/pending`);
}

/** 어드민 게시글 목록 — status 필터 (미지정 시 전체). */
export function adminListPosts(status?: PostStatus): Promise<PostSummary[]> {
  const qs = status ? `?status=${status}` : "";
  return adminFetch<PostSummary[]>(`/posts${qs}`);
}

export function adminGetPost(id: number): Promise<PostDetail> {
  return adminFetch<PostDetail>(`/posts/${id}`);
}

export function adminApprovePost(id: number): Promise<void> {
  return adminFetch<void>(`/posts/${id}/approve`, { method: "POST" });
}

export function adminEditPost(id: number, body: PostEditRequest): Promise<void> {
  return adminFetch<void>(`/posts/${id}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function adminDeletePost(id: number): Promise<void> {
  return adminFetch<void>(`/posts/${id}`, { method: "DELETE" });
}

export function adminDeleteComment(commentId: number): Promise<void> {
  return adminFetch<void>(`/posts/comments/${commentId}`, { method: "DELETE" });
}
