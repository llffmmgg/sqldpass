-- 모의고사 공개 상태: DRAFT(비공개) / PUBLISHED(공개) / PREMIUM(잠금).
-- 기존 회차는 PUBLISHED로 채워 무중단.
-- idempotent (V22~V24 패턴).

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mock_exam'
      AND COLUMN_NAME = 'visibility'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE mock_exam ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT ''PUBLISHED'' AFTER template',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mock_exam'
      AND INDEX_NAME = 'idx_mock_exam_visibility'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_mock_exam_visibility ON mock_exam (visibility)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
