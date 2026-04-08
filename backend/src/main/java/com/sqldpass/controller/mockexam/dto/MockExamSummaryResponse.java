package com.sqldpass.controller.mockexam.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.persistent.mockexam.ExamType;

public record MockExamSummaryResponse(
        Long id,
        String name,
        ExamType examType,
        int sequence,
        int totalQuestions,
        LocalDateTime createdAt,
        /** "쉬움" / "보통" / "어려움" / "매우 어려움" / null(데이터 없음) */
        String difficultyLabel
) {
    public static MockExamSummaryResponse from(MockExam mockExam) {
        Double normalized = normalize(mockExam.getExamType(), mockExam.getAvgDifficulty());
        String label = computeLabel(mockExam.getExamType(), normalized);

        return new MockExamSummaryResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getExamType(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt(),
                label);
    }

    /**
     * 시험 종류별 난이도 스케일을 0.0~1.0으로 정규화한다.
     * - SQLD: 0(기본) ~ 2(고난도) → avg / 2.0
     * - 정처기 실기: 1(쉬움) ~ 3(어려움) → (avg - 1) / 2.0
     */
    private static Double normalize(ExamType examType, Double avg) {
        if (avg == null) return null;
        if (examType == ExamType.ENGINEER_PRACTICAL) {
            return Math.max(0.0, Math.min(1.0, (avg - 1.0) / 2.0));
        }
        // SQLD 및 기본
        return Math.max(0.0, Math.min(1.0, avg / 2.0));
    }

    /**
     * 정규화 값(0~1)을 라벨로 분류. 시험 종류별로 단계 수가 다름.
     * - 정처기 실기: 3단계 (쉬움/보통/어려움)
     * - SQLD: 4단계 (쉬움/보통/어려움/매우 어려움)
     */
    private static String computeLabel(ExamType examType, Double normalized) {
        if (normalized == null) return null;
        if (examType == ExamType.ENGINEER_PRACTICAL) {
            if (normalized < 0.34) return "쉬움";
            if (normalized < 0.67) return "보통";
            return "어려움";
        }
        // SQLD 4단계 유지
        if (normalized < 0.25) return "쉬움";
        if (normalized < 0.5) return "보통";
        if (normalized < 0.75) return "어려움";
        return "매우 어려움";
    }
}
