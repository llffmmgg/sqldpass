package com.sqldpass.controller.mockexam.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.mockexam.MockExamVisibility;

public record MockExamSummaryResponse(
        Long id,
        String name,
        ExamType examType,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt,
        String difficultyLabel,
        boolean solved,
        Integer bestCorrectCount,
        Integer bestTotalCount,
        String templateKey,
        String templateLabel,
        MockExamVisibility visibility,
        boolean expertVerified,
        MockExamKind kind,
        Integer examYear,
        Integer examRound,
        LocalDate examDate
) {
    public static MockExamSummaryResponse from(MockExam mockExam) {
        return from(mockExam, null, null);
    }

    public static MockExamSummaryResponse from(MockExam mockExam, Integer bestCorrect, Integer bestTotal) {
        Double normalized = normalize(mockExam.getAvgDifficulty());
        String label = computeLabel(normalized);
        boolean solved = bestCorrect != null && bestTotal != null;
        EngineerExamTemplate template = mockExam.getTemplate();
        String templateKey = template != null ? template.name() : null;
        String templateLabel = template != null ? template.getDisplayName() : null;

        return new MockExamSummaryResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getExamType(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt(),
                label,
                solved,
                bestCorrect,
                bestTotal,
                templateKey,
                templateLabel,
                mockExam.getVisibility(),
                mockExam.isExpertVerified(),
                mockExam.getKind(),
                mockExam.getExamYear(),
                mockExam.getExamRound(),
                mockExam.getExamDate());
    }

    /**
     * All mock-exam types now use the same 1~4 difficulty scale.
     */
    private static Double normalize(Double avg) {
        if (avg == null) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, (avg - 1.0) / 3.0));
    }

    private static String computeLabel(Double normalized) {
        if (normalized == null) {
            return null;
        }
        if (normalized < 0.25) {
            return "\uC26C\uC6C0";
        }
        if (normalized < 0.5) {
            return "\uBCF4\uD1B5";
        }
        if (normalized < 0.75) {
            return "\uC5B4\uB824\uC6C0";
        }
        return "\uB9E4\uC6B0 \uC5B4\uB824\uC6C0";
    }
}
