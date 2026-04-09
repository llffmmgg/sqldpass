-- 사용자 개인 인앱 알림. 건의사항 해결 등 특정 사용자 대상 이벤트를 저장.
-- FK 미설정 — 회원 탈퇴 시 별도 cleanup 쿼리에서 일괄 삭제 처리.

CREATE TABLE notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL COMMENT '수신자',
    type        VARCHAR(40)  NOT NULL COMMENT '예: FEEDBACK_RESOLVED',
    title       VARCHAR(200) NOT NULL,
    body        TEXT         NULL,
    link        VARCHAR(500) NULL COMMENT '클릭 시 이동 경로',
    ref_id      BIGINT       NULL COMMENT '참조 엔티티 id (예: feedbackId)',
    read_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notification_member_created (member_id, created_at),
    INDEX idx_notification_member_unread (member_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
