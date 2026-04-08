-- 본문 normalize → SHA-256 hex (64자). 모의고사 회차 간/내 중복 검증용.
-- idempotent: 이전 부팅에서 ALTER만 성공하고 schema_history 기록 전에 죽은 경우 재시도 안전.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question'
      AND COLUMN_NAME = 'content_hash'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE question ADD COLUMN content_hash VARCHAR(64) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question'
      AND INDEX_NAME = 'idx_question_content_hash'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_question_content_hash ON question (content_hash)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
