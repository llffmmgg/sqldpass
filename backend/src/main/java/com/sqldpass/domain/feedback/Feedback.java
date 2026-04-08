package com.sqldpass.domain.feedback;

import java.time.LocalDateTime;

import com.sqldpass.persistent.feedback.FeedbackStatus;
import com.sqldpass.persistent.feedback.FeedbackType;

import lombok.Getter;

@Getter
public class Feedback {

    private final Long id;
    private final FeedbackType type;
    private final Long memberId;
    private final Long questionId;
    private final String content;
    private final String pageUrl;
    private final FeedbackStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Feedback(Long id, FeedbackType type, Long memberId, Long questionId,
                    String content, String pageUrl, FeedbackStatus status,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.memberId = memberId;
        this.questionId = questionId;
        this.content = content;
        this.pageUrl = pageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
