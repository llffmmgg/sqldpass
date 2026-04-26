-- 비회원 풀이 일일 한도용 IP 단위 quota 카운터.
-- (ip, solve_date) PK 라 새 날짜에 자동으로 새 행 → 자정 리셋 효과.
-- 기존 anonymous_solve_count (V41, 일별 전체 합계) 는 admin 통계용으로 보존.

CREATE TABLE anonymous_solve_ip_quota (
    ip          VARCHAR(64) NOT NULL,
    solve_date  DATE        NOT NULL,
    used_count  INT         NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (ip, solve_date),
    INDEX idx_ip_quota_date (solve_date)
);
