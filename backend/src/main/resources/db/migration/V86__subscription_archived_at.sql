-- 어드민이 통계 집계에서 분리(테스트 결제 정리 등)한 구독을 표시.
-- NULL = 정상, NOT NULL = archived(목록/통계 기본 제외).
-- 활성 구독은 archive 거부(AdminSubscriptionService.archive). row 보존 + history 에 ARCHIVED 기록.
ALTER TABLE subscription
    ADD COLUMN archived_at TIMESTAMP NULL COMMENT '운영자 정리 시점. NULL=정상.';

CREATE INDEX idx_subscription_archived_at ON subscription(archived_at);
