CREATE TABLE bookmark (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    question_id BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookmark_member_question (member_id, question_id),
    INDEX idx_bookmark_member_id (member_id),
    CONSTRAINT fk_bookmark_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_bookmark_question FOREIGN KEY (question_id) REFERENCES question (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
