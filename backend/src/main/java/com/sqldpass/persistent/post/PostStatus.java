package com.sqldpass.persistent.post;

/** 게시글 상태.
 * - PENDING: 어드민 승인 대기 (PASS_REVIEW 등 승인 필요한 카테고리)
 * - PUBLISHED: 게시판에 노출
 */
public enum PostStatus {
    PENDING,
    PUBLISHED
}
