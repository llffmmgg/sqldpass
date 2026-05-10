-- 구독 이력 감사 테이블 — 환불·만료·관리자 발급/회수 이력을 보존한다.
-- expireManual 의 delete 패턴을 revoke + history insert 로 교체하기 위한 기반.
-- FK 미설정: 회원 hard delete 와 분리해 audit 데이터가 유지되도록 한다.

CREATE TABLE subscription_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    plan VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NULL,
    actor_admin_id BIGINT NULL,
    payment_id BIGINT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_history_member_occurred (member_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
