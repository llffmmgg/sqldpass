-- 기출 복원 지원 — MockExam 에 kind/회차/연도/시험일 메타 추가
-- kind=AI  : 기존 AI 생성 모의고사 (기본값)
-- kind=PAST_EXAM : 전문가가 입력한 기출 복원 회차

ALTER TABLE mock_exam
    ADD COLUMN kind        VARCHAR(20) NOT NULL DEFAULT 'AI',
    ADD COLUMN exam_year   INT         NULL,
    ADD COLUMN exam_round  INT         NULL,
    ADD COLUMN exam_date   DATE        NULL;

-- 사용자 목록 조회 최적화 — kind + visibility + expert_verified 복합 인덱스
CREATE INDEX idx_mock_exam_kind_visibility
    ON mock_exam (exam_type, kind, visibility, expert_verified);
