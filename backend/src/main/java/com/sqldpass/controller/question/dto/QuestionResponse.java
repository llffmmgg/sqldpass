package com.sqldpass.controller.question.dto;

import com.sqldpass.domain.question.Question;

public record QuestionResponse(Long id, Long subjectId, String content) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(question.getId(), question.getSubjectId(), question.getContent());
    }
}
