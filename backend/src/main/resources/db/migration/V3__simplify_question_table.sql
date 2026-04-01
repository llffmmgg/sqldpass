-- question 테이블에 correct_option, explanation 컬럼 추가
ALTER TABLE question
    ADD COLUMN correct_option TINYINT NOT NULL DEFAULT 1 AFTER content,
    ADD COLUMN explanation TEXT NULL AFTER correct_option;

ALTER TABLE question
    ADD CONSTRAINT chk_question_correct_option CHECK (correct_option BETWEEN 1 AND 4);

-- question_option, explanation 테이블 삭제
DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS explanation;
