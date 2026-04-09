package com.sqldpass.controller.feedback.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.feedback.Feedback;
import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.persistent.feedback.FeedbackType;

public record FeedbackResponse(
        Long id,
        FeedbackType type,
        Long memberId,
        String memberNickname,
        Long questionId,
        String content,
        String pageUrl,
        FeedbackStatus status,
        String adminReply,
        LocalDateTime repliedAt,
        LocalDateTime createdAt
) {
    public static FeedbackResponse from(Feedback f, String memberNickname) {
        return new FeedbackResponse(
                f.getId(),
                f.getType(),
                f.getMemberId(),
                memberNickname,
                f.getQuestionId(),
                f.getContent(),
                f.getPageUrl(),
                f.getStatus(),
                f.getAdminReply(),
                f.getRepliedAt(),
                f.getCreatedAt());
    }
}
