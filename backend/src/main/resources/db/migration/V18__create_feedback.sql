-- 사용자 피드백 (문제 오류 신고, 사이트 버그/건의 제보) 저장 테이블.
-- FK는 일부러 안 검다 — 회원/문제 삭제돼도 피드백 기록은 보존(증거 보존 + 단순화).

CREATE TABLE feedback (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    type        VARCHAR(30)  NOT NULL COMMENT 'QUESTION_ERROR / BUG / FEATURE / OTHER',
    member_id   BIGINT       NOT NULL COMMENT '작성 회원 (로그인 필수)',
    question_id BIGINT       NULL COMMENT '문제 관련 피드백일 때만 세팅',
    content     TEXT         NOT NULL,
    page_url    VARCHAR(500) NULL COMMENT '제출한 페이지 URL',
    status      VARCHAR(20)  NOT NULL DEFAULT 'NEW' COMMENT 'NEW / REVIEWED / RESOLVED / WONTFIX',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_feedback_status (status),
    INDEX idx_feedback_question_id (question_id),
    INDEX idx_feedback_member_id (member_id),
    INDEX idx_feedback_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
