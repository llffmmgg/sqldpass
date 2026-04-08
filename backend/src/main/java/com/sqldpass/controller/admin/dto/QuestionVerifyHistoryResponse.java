package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionVerificationRunEntity;

public record QuestionVerifyHistoryResponse(
        Long runId,
        ExamType examType,
        Long subjectId,
        String subjectName,
        int limitRequested,
        boolean forceRecheck,
        int processedCount,
        int suspiciousCount,
        int fixedCount,
        int unfixableCount,
        int errorCount,
        LocalDateTime completedAt
) {
    public static QuestionVerifyHistoryResponse from(QuestionVerificationRunEntity entity) {
        return new QuestionVerifyHistoryResponse(
                entity.getId(),
                entity.getExamType(),
                entity.getSubject() != null ? entity.getSubject().getId() : null,
                entity.getSubjectName(),
                entity.getLimitRequested(),
                entity.isForceRecheck(),
                entity.getProcessedCount(),
                entity.getSuspiciousCount(),
                entity.getFixedCount(),
                entity.getUnfixableCount(),
                entity.getErrorCount(),
                entity.getCompletedAt());
    }
}
