package com.sqldpass.controller.wronganswer.dto;

import java.time.LocalDateTime;

import com.sqldpass.persistent.solve.WrongAnswerProjection;

public record WrongAnswerResponse(
        Long questionId, String questionContent, Long subjectId, String subjectName, int wrongCount, LocalDateTime lastWrongAt) {

    public static WrongAnswerResponse from(WrongAnswerProjection projection) {
        return new WrongAnswerResponse(
                projection.getQuestionId(), projection.getQuestionContent(),
                projection.getSubjectId(), projection.getSubjectName(),
                projection.getWrongCount(), projection.getLastWrongAt());
    }
}
