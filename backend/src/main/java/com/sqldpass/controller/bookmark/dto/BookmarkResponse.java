package com.sqldpass.controller.bookmark.dto;

import java.time.LocalDateTime;

import com.sqldpass.persistent.bookmark.BookmarkEntity;
import com.sqldpass.persistent.question.QuestionEntity;

public record BookmarkResponse(
        Long questionId,
        String questionContent,
        Long subjectId,
        String subjectName,
        LocalDateTime createdAt) {

    public static BookmarkResponse from(BookmarkEntity bookmark, QuestionEntity question) {
        return new BookmarkResponse(
                question.getId(),
                question.getContent(),
                question.getSubject().getId(),
                question.getSubject().getName(),
                bookmark.getCreatedAt()
        );
    }
}
