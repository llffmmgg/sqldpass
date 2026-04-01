package com.sqldpass.domain.solve;

import lombok.Getter;

@Getter
public class SolveAnswer {

    private final Long id;
    private final Long questionId;
    private final int selectedOption;
    private final int correctOption;
    private final boolean correct;

    public SolveAnswer(Long id, Long questionId, int selectedOption, int correctOption, boolean correct) {
        this.id = id;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.correctOption = correctOption;
        this.correct = correct;
    }
}
