package com.sqldpass.controller.solve;

import com.sqldpass.domain.solve.SolveAnswer;

public record SolveAnswerResponse(Long questionId, int selectedOption, int correctOption, boolean correct) {

    public static SolveAnswerResponse from(SolveAnswer answer) {
        return new SolveAnswerResponse(
                answer.getQuestionId(), answer.getSelectedOption(), answer.getCorrectOption(), answer.isCorrect()
        );
    }
}
