-- 연속 학습(streak) 기능을 위해 member 테이블 확장
ALTER TABLE member
    ADD COLUMN current_streak INT NOT NULL DEFAULT 0,
    ADD COLUMN longest_streak INT NOT NULL DEFAULT 0,
    ADD COLUMN last_solve_date DATE NULL;
