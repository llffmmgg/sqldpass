-- 게시판 — 1차: 합격 후기 카테고리 (어드민 승인 필요)
-- 추후 NOTICE/QNA 등 카테고리 추가 시 enum 만 늘리면 됨.

CREATE TABLE post (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    member_id    BIGINT       NOT NULL,
    category     VARCHAR(30)  NOT NULL,                    -- 'PASS_REVIEW' (enum, 추후 확장)
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING / PUBLISHED
    cert_key     VARCHAR(30)  NULL,                        -- SQLD/ENGINEER_PRACTICAL/... (PASS_REVIEW 만 NOT NULL)
    title        VARCHAR(120) NOT NULL,
    content      TEXT         NOT NULL,                    -- markdown
    view_count   BIGINT       NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_post_status_category_created (status, category, created_at),
    INDEX idx_post_cert (cert_key),
    INDEX idx_post_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_comment (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    post_id      BIGINT      NOT NULL,
    member_id    BIGINT      NOT NULL,
    content      TEXT        NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)   REFERENCES post(id)   ON DELETE CASCADE,
    CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    INDEX idx_comment_post_created (post_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
