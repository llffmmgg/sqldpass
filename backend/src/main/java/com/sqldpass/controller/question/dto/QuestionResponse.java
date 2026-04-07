package com.sqldpass.controller.question.dto;

import com.sqldpass.domain.question.Question;
import com.sqldpass.persistent.question.QuestionType;

public record QuestionResponse(Long id, Long subjectId, String content, QuestionType questionType) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getSubjectId(),
                question.getContent(),
                question.getQuestionType());
    }
}
