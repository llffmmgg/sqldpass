package com.sqldpass.domain.question;

import java.util.List;

import lombok.Getter;

@Getter
public class Question {

    private final Long id;
    private final Long subjectId;
    private final String content;
    private final List<QuestionOption> options;
    private final Explanation explanation;

    public Question(Long id, Long subjectId, String content, List<QuestionOption> options, Explanation explanation) {
        this.id = id;
        this.subjectId = subjectId;
        this.content = content;
        this.options = options != null ? options : List.of();
        this.explanation = explanation;
    }
}
