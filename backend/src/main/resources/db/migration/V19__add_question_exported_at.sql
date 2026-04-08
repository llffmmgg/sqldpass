-- 어드민 검증용 Markdown export 마크 컬럼
-- NULL = 미export (다음 다운로드 대상)
-- NOT NULL = 마지막 export 시각

ALTER TABLE question ADD COLUMN exported_at TIMESTAMP NULL;
CREATE INDEX idx_question_exported_at ON question (exported_at);
