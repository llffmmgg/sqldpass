-- 비회원(익명) 풀이 카운터.
-- DB 에 풀이 로우를 저장하지 않고, 날짜 단위 집계만 유지.
CREATE TABLE anonymous_solve_count (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    solve_date DATE NOT NULL UNIQUE,
    count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);
