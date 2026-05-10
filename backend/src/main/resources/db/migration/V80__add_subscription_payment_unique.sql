-- subscription.payment_id 에 UNIQUE 제약 — verify 재호출 시 중복 SubscriptionEntity 발급 방어.
-- V78 에서 nullable 로 바뀐 상태이며, MySQL 은 UNIQUE 컬럼의 NULL 다중 허용이라
-- admin 수동 발급(payment_id=null) 행이 여러 개 있어도 충돌하지 않는다.

ALTER TABLE subscription
    ADD CONSTRAINT uk_subscription_payment_id UNIQUE (payment_id);
