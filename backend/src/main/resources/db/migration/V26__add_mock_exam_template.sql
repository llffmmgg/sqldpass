-- 정처기 실기 모의고사 분포 템플릿 식별자.
-- SQLD/컴활 1급은 NULL 유지. 기존 정처기 모의고사도 NULL 유지(랜덤 생성된 회차라 역추론 안함).

ALTER TABLE mock_exam
    ADD COLUMN template VARCHAR(32) NULL COMMENT '정처기 실기 분포 템플릿 (PROGRAMMING_HEAVY/THEORY_HEAVY/BALANCED/DB_HEAVY)';
