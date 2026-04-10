package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.question.QuestionEntity;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.persistent.question.VerificationCategory;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public record AdminQuestionResponse(
        Long id,
        Long subjectId,
        String subjectName,
        String content,
        QuestionType questionType,
        Integer correctOption,
        String answer,
        List<String> keywords,
        String explanation,
        String summary,
        LocalDateTime createdAt,
        LocalDateTime verifiedAt,
        VerificationCategory verificationCategory) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AdminQuestionResponse from(QuestionEntity entity) {
        return new AdminQuestionResponse(
                entity.getId(),
                entity.getSubject().getId(),
                entity.getSubject().getName(),
                entity.getContent(),
                entity.getQuestionType() != null ? entity.getQuestionType() : QuestionType.MCQ,
                entity.getCorrectOption(),
                entity.getAnswer(),
                parseKeywords(entity.getKeywords()),
                entity.getExplanation(),
                entity.getSummary(),
                entity.getCreatedAt(),
                entity.getVerifiedAt(),
                entity.getVerificationCategory());
    }

    private static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return MAPPER.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(raw);
        }
    }
}
