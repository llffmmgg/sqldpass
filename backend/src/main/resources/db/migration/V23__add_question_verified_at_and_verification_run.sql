-- 어드민 LLM 직접 검증 완료 시각 + 검증 실행 이력 테이블.
-- idempotent: 이전 부팅에서 일부만 적용되고 schema_history 기록 전에 죽어도 안전하게 재시도.

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question'
      AND COLUMN_NAME = 'verified_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE question ADD COLUMN verified_at DATETIME NULL AFTER exported_at',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question'
      AND INDEX_NAME = 'idx_question_verified_at'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_question_verified_at ON question (verified_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS question_verification_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_type VARCHAR(30) NULL,
    subject_id BIGINT NULL,
    subject_name VARCHAR(100) NULL,
    limit_requested INT NOT NULL,
    force_recheck BIT NOT NULL,
    processed_count INT NOT NULL,
    suspicious_count INT NOT NULL,
    completed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_question_verification_run_subject
        FOREIGN KEY (subject_id) REFERENCES subject (id)
);

SET @run_idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question_verification_run'
      AND INDEX_NAME = 'idx_question_verification_run_completed_at'
);
SET @sql := IF(@run_idx_exists = 0,
    'CREATE INDEX idx_question_verification_run_completed_at ON question_verification_run (completed_at DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
