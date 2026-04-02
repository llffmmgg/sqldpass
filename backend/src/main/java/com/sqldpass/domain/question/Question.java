package com.sqldpass.domain.question;

import lombok.Getter;

@Getter
public class Question {

    private final Long id;
    private final Long subjectId;
    private final String content;
    private final int correctOption;
    private final String explanation;
    private final String summary;
    private final String topic;
    private final Integer difficulty;

    public Question(Long id, Long subjectId, String content, int correctOption, String explanation) {
        this(id, subjectId, content, correctOption, explanation, null, null, null);
    }

    public Question(Long id, Long subjectId, String content, int correctOption, String explanation,
                    String summary, String topic, Integer difficulty) {
        this.id = id;
        this.subjectId = subjectId;
        this.content = content;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.summary = summary;
        this.topic = topic;
        this.difficulty = difficulty;
    }
}
