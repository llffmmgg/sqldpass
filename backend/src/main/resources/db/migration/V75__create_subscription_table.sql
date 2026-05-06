-- 4티어 구독 모델 (무료 / 3일권 / 한달권 / 무제한권).
-- 단발 결제 → expiresAt = now + plan.days. UNLIMITED 는 expiresAt = null.
-- 한 회원이 다건 보유 가능 (구매 누적). SubscriptionService 가 가장 강한 plan 우선 적용.

CREATE TABLE subscription (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    plan VARCHAR(20) NOT NULL,
    payment_id BIGINT NOT NULL,
    purchased_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_subscription_payment FOREIGN KEY (payment_id) REFERENCES payment (id),
    INDEX idx_subscription_member_expires (member_id, expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
