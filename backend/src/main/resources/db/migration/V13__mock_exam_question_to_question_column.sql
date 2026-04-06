-- 모의고사-문제 관계를 조인 테이블(mock_exam_question)에서
-- question 테이블의 직접 컬럼(mock_exam_id, display_order)으로 전환.
--
-- 배경: 한 문제는 최대 한 모의고사에만 등장해야 하므로 M:N이 아닌 1:N이 맞음.
-- 이 마이그레이션으로 스키마 레벨에서 모의고사 간 문제 중복이 원천 차단됨.
-- 일반 문제풀이 목록은 mock_exam_id로 필터링하지 않으므로 편성 문제도 그대로 노출.

-- 1) question에 컬럼/FK/인덱스 추가
ALTER TABLE question
    ADD COLUMN mock_exam_id  BIGINT NULL,
    ADD COLUMN display_order INT    NULL,
    ADD CONSTRAINT fk_question_mock_exam
        FOREIGN KEY (mock_exam_id) REFERENCES mock_exam(id) ON DELETE SET NULL,
    ADD INDEX idx_question_subject_mock (subject_id, mock_exam_id);

-- 2) 기존 mock_exam_question 데이터를 question으로 이관
--    같은 question_id가 여러 mock_exam에 존재하면 가장 작은 mock_exam_id(= 가장 오래된 모의고사)만 남김.
UPDATE question q
JOIN (
    SELECT question_id,
           MIN(mock_exam_id) AS mock_exam_id
    FROM mock_exam_question
    GROUP BY question_id
) pick ON pick.question_id = q.id
JOIN mock_exam_question meq
      ON meq.question_id = pick.question_id
     AND meq.mock_exam_id = pick.mock_exam_id
SET q.mock_exam_id  = meq.mock_exam_id,
    q.display_order = meq.display_order;

-- 3) 조인 테이블 드롭
DROP TABLE mock_exam_question;
