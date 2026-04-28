-- 모의고사 NEW 뱃지 트리거용 이벤트 타임스탬프
-- published_at: DRAFT → PUBLISHED 첫 전환 시각
-- past_exam_linked_at: 기출 복원으로 승격된 시각

ALTER TABLE mock_exam
    ADD COLUMN published_at        DATETIME(6) NULL COMMENT 'DRAFT->PUBLISHED 최초 전환 시각',
    ADD COLUMN past_exam_linked_at DATETIME(6) NULL COMMENT '기출 복원으로 승격된 시각';

-- 백필: 이미 공개 상태인 회차는 created_at 으로 채워 NEW 인플레이션 방지
UPDATE mock_exam
   SET published_at = created_at
 WHERE visibility IN ('PUBLISHED', 'PREMIUM')
   AND published_at IS NULL;

UPDATE mock_exam
   SET past_exam_linked_at = created_at
 WHERE kind = 'PAST_EXAM'
   AND past_exam_linked_at IS NULL;
