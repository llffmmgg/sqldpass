-- 정보처리기사 실기 지원을 위한 스키마 확장
-- 1) question: 단답형/서술형 지원 (question_type, answer, keywords)
-- 2) mock_exam: exam_type 구분 (SQLD vs ENGINEER_PRACTICAL)
-- 3) solve_answer: 비-MCQ 채점 결과 저장
-- 4) subject: 정보처리기사 실기 카테고리 트리 추가

-- =========================================================
-- 1) question 테이블 확장
-- =========================================================
ALTER TABLE question
    ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'MCQ' AFTER content,
    ADD COLUMN answer TEXT NULL AFTER correct_option,
    ADD COLUMN keywords TEXT NULL COMMENT 'JSON array: short_answer alias 또는 descriptive 채점 키워드' AFTER answer;

-- correct_option을 nullable로 완화 (비-MCQ는 0 또는 NULL 저장)
ALTER TABLE question
    MODIFY COLUMN correct_option TINYINT NULL;

-- =========================================================
-- 2) mock_exam 테이블 확장
-- =========================================================
ALTER TABLE mock_exam
    ADD COLUMN exam_type VARCHAR(30) NOT NULL DEFAULT 'SQLD' AFTER name,
    ADD INDEX idx_mock_exam_type (exam_type);

-- =========================================================
-- 3) solve_answer 확장 — 비-MCQ 채점 기록
-- =========================================================
ALTER TABLE solve_answer
    ADD COLUMN user_answer_text TEXT NULL COMMENT '단답/서술형 사용자 답안',
    ADD COLUMN matched_keywords TEXT NULL COMMENT '채점 시 매치된 키워드 JSON',
    ADD COLUMN partial_score DECIMAL(4,3) NULL COMMENT '0.000~1.000 부분점수 (서술형)';

-- selected_option도 비-MCQ에선 NULL이 올 수 있도록
ALTER TABLE solve_answer
    MODIFY COLUMN selected_option INT NULL,
    MODIFY COLUMN correct_option INT NULL;

-- =========================================================
-- 4) subject: 정보처리기사 실기 트리
-- =========================================================
INSERT INTO subject (parent_id, name, display_order, created_at, updated_at)
VALUES (NULL, '정보처리기사 실기', 3, NOW(6), NOW(6));

SET @engineer_root = LAST_INSERT_ID();

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at) VALUES
    (@engineer_root, 'C언어',           1, NOW(6), NOW(6)),
    (@engineer_root, 'Java',            2, NOW(6), NOW(6)),
    (@engineer_root, 'Python',          3, NOW(6), NOW(6)),
    (@engineer_root, 'SQL',             4, NOW(6), NOW(6)),
    (@engineer_root, '소프트웨어 설계',  5, NOW(6), NOW(6)),
    (@engineer_root, '데이터베이스 이론', 6, NOW(6), NOW(6)),
    (@engineer_root, '네트워크/OS',     7, NOW(6), NOW(6)),
    (@engineer_root, '보안',            8, NOW(6), NOW(6)),
    (@engineer_root, '신기술 동향',      9, NOW(6), NOW(6));
