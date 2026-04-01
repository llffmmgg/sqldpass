package com.sqldpass.domain.question;

import lombok.Builder;
import lombok.Getter;

@Getter
public class QuestionOption {

    private final Long id;
    private final int optionNumber;
    private final String content;
    private final boolean correct;

    @Builder
    public QuestionOption(Long id, int optionNumber, String content, boolean correct) {
        this.id = id;
        this.optionNumber = optionNumber;
        this.content = content;
        this.correct = correct;
    }
}
