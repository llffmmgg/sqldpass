package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;

import com.sqldpass.persistent.question.QuestionEntity;

public record AdminQuestionResponse(
        Long id,
        Long subjectId,
        String subjectName,
        String content,
        Integer correctOption,
        String explanation,
        String summary,
        LocalDateTime createdAt) {

    public static AdminQuestionResponse from(QuestionEntity entity) {
        return new AdminQuestionResponse(
                entity.getId(),
                entity.getSubject().getId(),
                entity.getSubject().getName(),
                entity.getContent(),
                entity.getCorrectOption(),
                entity.getExplanation(),
                entity.getSummary(),
                entity.getCreatedAt());
    }
}
