-- 본문 normalize → SHA-256 hex (64자). 모의고사 회차 간/내 중복 검증용.
ALTER TABLE question
    ADD COLUMN content_hash VARCHAR(64) NULL;

CREATE INDEX idx_question_content_hash ON question (content_hash);
