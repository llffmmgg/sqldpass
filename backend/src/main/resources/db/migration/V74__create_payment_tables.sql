-- PortOne 결제 시도 기록 + 모의고사 잠금 해제 권리.
-- 카드사 심사용 닉네임 화이트리스트 게이트는 application.yaml 의 payment.reviewer-nicknames 로 분리 관리 (DB 스키마 변경 없음).

CREATE TABLE payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id VARCHAR(80) NOT NULL,
    member_id BIGINT NOT NULL,
    mock_exam_id BIGINT NULL,
    product_name VARCHAR(120) NOT NULL,
    amount INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    pg_response TEXT NULL,
    paid_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_payment_id UNIQUE (payment_id),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_payment_member_status (member_id, status),
    INDEX idx_payment_mock_exam (mock_exam_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE mock_exam_purchase (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    mock_exam_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    purchased_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_mock_exam_purchase_member_exam UNIQUE (member_id, mock_exam_id),
    CONSTRAINT fk_mock_exam_purchase_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_mock_exam_purchase_payment FOREIGN KEY (payment_id) REFERENCES payment (id),
    INDEX idx_mock_exam_purchase_member (member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
