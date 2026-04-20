package com.sqldpass.domain.bookmark;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Bookmark {

    private final Long id;
    private final Long memberId;
    private final Long questionId;
    private final LocalDateTime createdAt;

    public Bookmark(Long id, Long memberId, Long questionId, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.questionId = questionId;
        this.createdAt = createdAt;
    }
}
