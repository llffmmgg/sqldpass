-- 결제 row 에 plan 컬럼 추가 (어떤 구독권을 산 결제인지 추적).
-- 옛 mock_exam_purchase 흐름 row 는 plan = NULL 로 남겨두기 위해 nullable.

ALTER TABLE payment
    ADD COLUMN plan VARCHAR(20) NULL AFTER product_name;
