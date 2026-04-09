-- 어드민 LLM 검증 결과 카테고리 컬럼 추가.
-- enum: NONE / AUTO_FIXED / MANUAL_REVIEW / ERROR
-- 어드민이 카테고리별로 문제 목록을 조회하고 즉시 수정할 수 있도록 영속화한다.

ALTER TABLE question
    ADD COLUMN verification_category VARCHAR(20) NOT NULL DEFAULT 'NONE'
        COMMENT '마지막 검증 결과 카테고리 — 수정 시 NONE 리셋';

CREATE INDEX idx_question_verification_category
    ON question (verification_category);
