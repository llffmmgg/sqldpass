package com.sqldpass.controller.solve.dto;

import java.time.LocalDateTime;

import com.sqldpass.domain.solve.Solve;

public record SolveSummaryResponse(
        Long id, Long subjectId, int totalCount, int correctCount, int score, LocalDateTime solvedAt) {

    public static SolveSummaryResponse from(Solve solve) {
        return new SolveSummaryResponse(
                solve.getId(), solve.getSubjectId(), solve.getTotalCount(),
                solve.getCorrectCount(), solve.getScore(), solve.getSolvedAt());
    }
}
