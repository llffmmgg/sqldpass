-- 미니 모의고사 풀 마킹 컬럼
-- NULL          = 미니 풀 후보 (다음 미니 생성 시 복제 대상)
-- NOT NULL      = 한 번 미니로 복제·사용됨 → 다음 미니에서 제외
--
-- 풀이 자체는 원본 문제 그대로 두고, 미니 모의고사용 QuestionEntity 는 별도로 복제해
-- 새 mock_exam_id (kind=MINI) 에 link 한다. 이 컬럼은 원본에만 세팅되어 다음 회차 생성 시
-- 풀 중복을 방지한다.

ALTER TABLE question
    ADD COLUMN included_in_mini_at DATETIME(6) NULL COMMENT '미니 모의고사로 복제된 시각. NULL 이면 다음 미니 풀 후보.';

CREATE INDEX idx_question_mini_pool
    ON question (subject_id, included_in_mini_at);
