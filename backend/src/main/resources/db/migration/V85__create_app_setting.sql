-- 어드민에서 런타임에 토글 가능한 단순 key-value 설정 저장소.
-- 첫 사용처: 결제창 노출 정책(payment.checkout_open_to_all).
-- 추후 다른 토글성 설정도 같은 테이블 재사용 가능.
CREATE TABLE app_setting (
    setting_key   VARCHAR(64)  NOT NULL PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL
);

-- 초기값 true = 모든 사용자에게 결제창 노출(정식 오픈 전환).
-- false 로 토글하면 PaymentProperties.reviewerNicknames 화이트리스트로 폴백.
INSERT INTO app_setting (setting_key, setting_value, created_at, updated_at)
VALUES ('payment.checkout_open_to_all', 'true', NOW(6), NOW(6));
