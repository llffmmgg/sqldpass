import { fetchApi } from "@/lib/api";

export type FeedbackType = "QUESTION_ERROR" | "BUG" | "FEATURE" | "OTHER";
export type FeedbackStatus = "NEW" | "REVIEWED" | "RESOLVED" | "WONTFIX";

export interface CreateFeedbackPayload {
  type: FeedbackType;
  questionId?: number | null;
  content: string;
  pageUrl?: string | null;
}

export interface FeedbackResponseDto {
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

/** 피드백 제출 — 로그인 필수 (401이면 fetchApi가 홈으로 리다이렉트) */
export function submitFeedback(payload: CreateFeedbackPayload) {
  return fetchApi<FeedbackResponseDto>("/feedback", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
