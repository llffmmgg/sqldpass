package com.sqldpass.domain.question;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Explanation {

    private final Long id;
    private final String content;

    @Builder
    public Explanation(Long id, String content) {
        this.id = id;
        this.content = content;
    }
}
