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
        /** "쉬움" / "보통" / "어려움" / "혼합" / null(데이터 없음) */
        String difficultyLabel,
        /** 0.0~1.0 정규화된 평균 난이도 (시험 종류마다 스케일이 달라 클라이언트에서 비교 용이) */
        Double avgDifficultyNormalized
) {
    public static MockExamSummaryResponse from(MockExam mockExam) {
        Double normalized = normalize(mockExam.getExamType(), mockExam.getAvgDifficulty());
        String label = computeLabel(normalized, mockExam.getMinDifficulty(), mockExam.getMaxDifficulty());

        return new MockExamSummaryResponse(
                mockExam.getId(),
                mockExam.getName(),
                mockExam.getExamType(),
                mockExam.getSequence(),
                mockExam.getTotalQuestions(),
                mockExam.getCreatedAt(),
                label,
                normalized);
    }

    /**
     * 시험 종류별 난이도 스케일을 0.0~1.0으로 정규화한다.
     * - SQLD: 0(기본) ~ 2(고난도) → avg / 2.0
     * - 정처기 실기: 1(기본) ~ 5(고난도) → (avg - 1) / 4.0
     */
    private static Double normalize(ExamType examType, Double avg) {
        if (avg == null) return null;
        if (examType == ExamType.ENGINEER_PRACTICAL) {
            return Math.max(0.0, Math.min(1.0, (avg - 1.0) / 4.0));
        }
        // SQLD 및 기본
        return Math.max(0.0, Math.min(1.0, avg / 2.0));
    }

    /**
     * 정규화 값 + 분포 폭으로 라벨 결정.
     * - max - min 이 4 이상이면 분포가 매우 넓다고 보고 "혼합"
     * - 그 외 정규화 값으로 쉬움/보통/어려움 분류
     */
    private static String computeLabel(Double normalized, Integer min, Integer max) {
        if (normalized == null) return null;
        if (min != null && max != null && (max - min) >= 4) return "혼합";
        if (normalized < 0.34) return "쉬움";
        if (normalized < 0.67) return "보통";
        return "어려움";
    }
}
