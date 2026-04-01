package com.sqldpass.domain.solve;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;

@Getter
public class Solve {

    private final Long id;
    private final Long memberId;
    private final Long subjectId;
    private final int totalCount;
    private final int correctCount;
    private final int score;
    private final LocalDateTime solvedAt;
    private final List<SolveAnswer> answers;

    public Solve(Long id, Long memberId, Long subjectId, int totalCount, int correctCount, int score,
                 LocalDateTime solvedAt, List<SolveAnswer> answers) {
        this.id = id;
        this.memberId = memberId;
        this.subjectId = subjectId;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.score = score;
        this.solvedAt = solvedAt;
        this.answers = answers != null ? answers : List.of();
    }
}
