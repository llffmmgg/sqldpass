-- 어드민 수동 구독 발급 (보상·이벤트·환불 후 재발급) 을 위해
-- subscription.payment_id 를 nullable 로 변경. 결제 row 가 없는 발급의 경우 null.

ALTER TABLE subscription MODIFY COLUMN payment_id BIGINT NULL;
