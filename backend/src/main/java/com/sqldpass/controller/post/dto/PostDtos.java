package com.sqldpass.controller.post.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.post.PostCategory;
import com.sqldpass.persistent.post.PostCommentEntity;
import com.sqldpass.persistent.post.PostEntity;
import com.sqldpass.persistent.post.PostStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 게시판 (post) 관련 Request/Response DTO 모음.
 * 사용자용·어드민용 공용. record 로 단순 매핑.
 */
public final class PostDtos {

    private PostDtos() {}

    /** 새 글 제출 (사용자). PASS_REVIEW 면 cert 필수, 백엔드에서 검증. */
    public record PostSubmitRequest(
            @NotNull PostCategory category,
            ExamType cert,
            @NotBlank @Size(max = 120) String title,
            @NotBlank String content
    ) {}

    /** 글 수정 (작성자 본인). 카테고리·cert 변경 X. */
    public record PostEditRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank String content
    ) {}

    /** 댓글 작성/수정. */
    public record CommentRequest(
            @NotBlank String content
    ) {}

    /** 게시판 목록 카드. */
    public record PostSummaryResponse(
            Long id,
            PostCategory category,
            PostStatus status,
            ExamType cert,
            String title,
            long viewCount,
            int commentCount,
            String authorNickname,
            LocalDateTime createdAt
    ) {
        public static PostSummaryResponse from(PostEntity p, int commentCount) {
            return new PostSummaryResponse(
                    p.getId(),
                    p.getCategory(),
                    p.getStatus(),
                    p.getCertKey(),
                    p.getTitle(),
                    p.getViewCount(),
                    commentCount,
                    p.getMember().getNickname(),
                    p.getCreatedAt()
            );
        }
    }

    /** 글 상세 — 본문 + 댓글 포함. */
    public record PostDetailResponse(
            Long id,
            PostCategory category,
            PostStatus status,
            ExamType cert,
            String title,
            String content,
            long viewCount,
            String authorNickname,
            Long authorId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<CommentResponse> comments
    ) {
        public static PostDetailResponse from(PostEntity p, List<PostCommentEntity> comments) {
            return new PostDetailResponse(
                    p.getId(),
                    p.getCategory(),
                    p.getStatus(),
                    p.getCertKey(),
                    p.getTitle(),
                    p.getContent(),
                    p.getViewCount(),
                    p.getMember().getNickname(),
                    p.getMember().getId(),
                    p.getCreatedAt(),
                    p.getUpdatedAt(),
                    comments.stream().map(CommentResponse::from).toList()
            );
        }
    }

    public record CommentResponse(
            Long id,
            String content,
            String authorNickname,
            Long authorId,
            LocalDateTime createdAt
    ) {
        public static CommentResponse from(PostCommentEntity c) {
            return new CommentResponse(
                    c.getId(),
                    c.getContent(),
                    c.getMember().getNickname(),
                    c.getMember().getId(),
                    c.getCreatedAt()
            );
        }
    }

    /** 페이지 응답. */
    public record PostPageResponse(
            List<PostSummaryResponse> items,
            int page,
            int size,
            long total,
            int totalPages
    ) {}
}
