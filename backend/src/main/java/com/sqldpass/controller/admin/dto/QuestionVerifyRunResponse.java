package com.sqldpass.controller.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;

public record QuestionVerifyRunResponse(
        ExamType examType,
        Long subjectId,
        String subjectName,
        int requestedLimit,
        boolean forceRecheck,
        int processedCount,
        int suspiciousCount,
        int fixedCount,
        int unfixableCount,
        int errorCount,
        LocalDateTime completedAt,
        List<QuestionVerifyResultResponse> suspiciousQuestions,
        List<QuestionVerifyHistoryResponse> recentRuns
) {
}
