-- 자격증별(exam_type) 독립 sequence 지원
-- 이전: sequence 단일 유니크 → SQLD/정처기 섞인 번호 (예: SQLD 1, 정처기 2, SQLD 3 ...)
-- 이후: (exam_type, sequence) 복합 유니크 → 자격증별 1,2,3 독립 번호

-- 1) 기존 sequence 단독 유니크 제약 제거
ALTER TABLE mock_exam DROP INDEX uk_mock_exam_sequence;

-- 2) exam_type 별로 sequence 재번호 (MySQL 8 윈도우 함수)
UPDATE mock_exam m
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY exam_type ORDER BY sequence, id) AS rn
    FROM mock_exam
) r ON m.id = r.id
SET m.sequence = r.rn;

-- 3) 복합 유니크 제약 추가
ALTER TABLE mock_exam
    ADD CONSTRAINT uk_mock_exam_exam_type_sequence UNIQUE (exam_type, sequence);

-- 4) 이름 재포맷 — 자격증별 일관된 네이밍
UPDATE mock_exam
SET name = CONCAT('SQLD 모의고사 ', sequence, '회')
WHERE exam_type = 'SQLD';

UPDATE mock_exam
SET name = CONCAT('정보처리기사 실기 모의고사 ', sequence, '회')
WHERE exam_type = 'ENGINEER_PRACTICAL';
