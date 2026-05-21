-- 회원별 일일 사용량 카운터 (무료 한도 트래킹).
-- 복합 PK (member_id, usage_date) — 매일 새 row 자연 생성으로 자정 리셋 효과.
-- 활성 구독자는 서비스 레이어에서 카운터 생성 자체를 스킵하므로 이 테이블엔 들어오지 않음.

CREATE TABLE daily_usage (
    member_id           BIGINT      NOT NULL,
    usage_date          DATE        NOT NULL,
    question_count      INT         NOT NULL DEFAULT 0,
    mock_session_count  INT         NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id, usage_date),
    INDEX idx_daily_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
