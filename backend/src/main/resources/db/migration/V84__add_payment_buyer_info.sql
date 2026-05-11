-- KG이니시스 PortOne V2 PC 일반결제 customer 필수 필드 보관용.
-- 결제 시점에 사용자가 모달에 입력한 정보를 PaymentEntity 에 동봉 저장 →
-- 영수증 발송·CS 식별·환불 응대에 사용. 회원 정보와는 분리.
-- 기존 row 는 NULL 유지 (이전 결제 영향 없음).
ALTER TABLE payment
    ADD COLUMN buyer_name         VARCHAR(50)  NULL AFTER member_id,
    ADD COLUMN buyer_email        VARCHAR(255) NULL AFTER buyer_name,
    ADD COLUMN buyer_phone_number VARCHAR(20)  NULL AFTER buyer_email;
