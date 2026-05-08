-- 안드로이드 앱 Google Play Billing 결제 지원 — payment 테이블에 provider/token 추가.
-- 기존 PortOne(웹) 결제는 provider='PORTONE' 으로 자동 마이그레이트.
-- Play Billing 결제는 provider='PLAY_BILLING', purchase_token 에 Google 영수증 토큰 보관.
-- RTDN webhook 이 토큰으로 payment 를 lookup 하므로 인덱스 추가.

ALTER TABLE payment
    ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'PORTONE' AFTER status,
    ADD COLUMN purchase_token VARCHAR(512) NULL AFTER provider,
    ADD INDEX idx_payment_purchase_token (purchase_token);
