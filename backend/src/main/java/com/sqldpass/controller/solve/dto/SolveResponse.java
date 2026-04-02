package com.sqldpass.controller.solve.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.domain.solve.Solve;

public record SolveResponse(
        Long id, Long subjectId, int totalCount, int correctCount,
        int score, LocalDateTime solvedAt, List<SolveAnswerResponse> answers) {

    public static SolveResponse from(Solve solve) {
        List<SolveAnswerResponse> answers = solve.getAnswers().stream()
                .map(SolveAnswerResponse::from).toList();
        return new SolveResponse(
                solve.getId(), solve.getSubjectId(), solve.getTotalCount(),
                solve.getCorrectCount(), solve.getScore(), solve.getSolvedAt(), answers);
    }
}
