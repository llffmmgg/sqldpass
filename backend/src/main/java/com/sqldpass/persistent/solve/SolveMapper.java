package com.sqldpass.persistent.solve;

import java.util.List;

import com.sqldpass.domain.solve.Solve;
import com.sqldpass.domain.solve.SolveAnswer;

public class SolveMapper {

    private SolveMapper() {
    }

    public static Solve toDomain(SolveEntity entity) {
        List<SolveAnswer> answers = entity.getAnswers().stream()
                .map(SolveMapper::toDomain)
                .toList();

        return new Solve(
                entity.getId(),
                entity.getMember().getId(),
                entity.getSubject() != null ? entity.getSubject().getId() : null,
                entity.getMockExam() != null ? entity.getMockExam().getId() : null,
                entity.getTotalCount(),
                entity.getCorrectCount(),
                entity.getScore(),
                entity.getCreatedAt(),
                answers
        );
    }

    public static SolveAnswer toDomain(SolveAnswerEntity entity) {
        return new SolveAnswer(
                entity.getId(),
                entity.getQuestion().getId(),
                entity.getSelectedOption(),
                entity.getCorrectOption(),
                entity.isCorrect()
        );
    }
}
