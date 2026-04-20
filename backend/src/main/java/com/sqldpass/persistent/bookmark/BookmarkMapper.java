package com.sqldpass.persistent.bookmark;

import com.sqldpass.domain.bookmark.Bookmark;

public class BookmarkMapper {

    private BookmarkMapper() {
    }

    public static Bookmark toDomain(BookmarkEntity entity) {
        return new Bookmark(
                entity.getId(),
                entity.getMemberId(),
                entity.getQuestionId(),
                entity.getCreatedAt()
        );
    }
}
