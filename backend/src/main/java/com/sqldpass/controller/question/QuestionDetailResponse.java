package com.sqldpass.controller.question;

import com.sqldpass.domain.question.Question;

public record QuestionDetailResponse(
        Long id,
        Long subjectId,
        String content,
        int correctOption,
        String explanation
) {

    public static QuestionDetailResponse from(Question question) {
        return new QuestionDetailResponse(
                question.getId(),
                question.getSubjectId(),
                question.getContent(),
                question.getCorrectOption(),
                question.getExplanation()
        );
    }
}
