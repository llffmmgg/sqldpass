package com.sqldpass.domain.question;

import lombok.Getter;

@Getter
public class Question {

    private final Long id;
    private final Long subjectId;
    private final String content;
    private final int correctOption;
    private final String explanation;

    public Question(Long id, Long subjectId, String content, int correctOption, String explanation) {
        this.id = id;
        this.subjectId = subjectId;
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
    }
}
