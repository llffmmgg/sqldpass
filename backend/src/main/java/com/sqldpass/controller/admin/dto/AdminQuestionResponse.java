package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamEntity;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
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
        VerificationCategory verificationCategory,
        /** 본 문제가 수록된 모의고사. 자유 풀이면 null. */
        MockExamRef mockExam) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AdminQuestionResponse from(QuestionEntity entity) {
        MockExamEntity exam = entity.getMockExam();
        MockExamRef ref = exam == null ? null
                : new MockExamRef(exam.getId(), exam.getName(), exam.getExamType(),
                                  exam.getSequence(), exam.getVisibility());
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
                entity.getVerificationCategory(),
                ref);
    }

    public record MockExamRef(Long id, String name, ExamType examType, int sequence, MockExamVisibility visibility) {}

    private static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return MAPPER.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(raw);
        }
    }
}
