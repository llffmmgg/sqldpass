-- 검증 회차에 자동 수정/수동 검토 카운트 추가. idempotent (V22/V23 패턴).

SET @c1 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question_verification_run'
      AND COLUMN_NAME = 'fixed_count'
);
SET @sql := IF(@c1 = 0,
    'ALTER TABLE question_verification_run ADD COLUMN fixed_count INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c2 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question_verification_run'
      AND COLUMN_NAME = 'unfixable_count'
);
SET @sql := IF(@c2 = 0,
    'ALTER TABLE question_verification_run ADD COLUMN unfixable_count INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c3 := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question_verification_run'
      AND COLUMN_NAME = 'error_count'
);
SET @sql := IF(@c3 = 0,
    'ALTER TABLE question_verification_run ADD COLUMN error_count INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
