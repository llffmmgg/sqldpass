-- 업그레이드 prorate (잔여 일수 비례 차감) 회계 보존용 컬럼.
-- amount 는 실 결제 금액 (PG 청구액). 옛 row 는 base_amount=amount, prorate_discount=0 으로 backfill.

ALTER TABLE payment
    ADD COLUMN base_amount INT NULL AFTER amount,
    ADD COLUMN prorate_discount INT NOT NULL DEFAULT 0 AFTER base_amount;

UPDATE payment SET base_amount = amount WHERE base_amount IS NULL;
ALTER TABLE payment MODIFY COLUMN base_amount INT NOT NULL;
