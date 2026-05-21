-- 계정 삭제 시 결제/권한/구독 이력 row 는 환불·세무·CS 근거로 보존하되,
-- member row hard delete 를 막지 않도록 회원 참조만 NULL 처리할 수 있게 연다.
-- FK 는 그대로 유지한다. MySQL FK 는 child key 가 NULL 이면 참조 검사를 수행하지 않는다.

ALTER TABLE payment
    MODIFY COLUMN member_id BIGINT NULL;

ALTER TABLE mock_exam_purchase
    MODIFY COLUMN member_id BIGINT NULL;

ALTER TABLE subscription
    MODIFY COLUMN member_id BIGINT NULL;

ALTER TABLE subscription_history
    MODIFY COLUMN member_id BIGINT NULL;
