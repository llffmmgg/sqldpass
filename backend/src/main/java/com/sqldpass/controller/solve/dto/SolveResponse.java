package com.sqldpass.controller.solve.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.domain.solve.Solve;

public record SolveResponse(
        Long id, Long subjectId, Long mockExamId, int totalCount, int correctCount,
        int score, LocalDateTime solvedAt, List<SolveAnswerResponse> answers,
        /** 현재 연속 학습 일수 (풀이 제출 직후 반영된 값). */
        Integer currentStreak,
        /** 이번 풀이로 도달한 마일스톤(7·30·100·365). 없으면 null. */
        Integer milestoneReached) {

    public static SolveResponse from(Solve solve) {
        List<SolveAnswerResponse> answers = solve.getAnswers().stream()
                .map(SolveAnswerResponse::from).toList();
        return new SolveResponse(
                solve.getId(), solve.getSubjectId(), solve.getMockExamId(), solve.getTotalCount(),
                solve.getCorrectCount(), solve.getScore(), solve.getSolvedAt(), answers,
                null, null);
    }

    public static SolveResponse from(Solve solve, Integer currentStreak, Integer milestoneReached) {
        List<SolveAnswerResponse> answers = solve.getAnswers().stream()
                .map(SolveAnswerResponse::from).toList();
        return new SolveResponse(
                solve.getId(), solve.getSubjectId(), solve.getMockExamId(), solve.getTotalCount(),
                solve.getCorrectCount(), solve.getScore(), solve.getSolvedAt(), answers,
                currentStreak, milestoneReached);
    }
}
