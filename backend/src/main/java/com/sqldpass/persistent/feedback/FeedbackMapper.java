package com.sqldpass.persistent.feedback;

import com.sqldpass.domain.feedback.Feedback;

public final class FeedbackMapper {

    private FeedbackMapper() {
    }

    public static Feedback toDomain(FeedbackEntity entity) {
        return new Feedback(
                entity.getId(),
                entity.getType(),
                entity.getMemberId(),
                entity.getQuestionId(),
                entity.getContent(),
                entity.getPageUrl(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
