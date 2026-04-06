-- 모의고사 시스템 — 진짜 SQLD처럼 50문항 한 세트 풀이
-- mock_exam: 모의고사 세트 (모의고사 1, 2, 3...)
-- mock_exam_question: 모의고사에 포함된 문제 매핑 (50문항)
-- solve: mock_exam_id 컬럼 추가, subject_id nullable로 변경 (모의고사 풀이는 단일 과목 아님)

CREATE TABLE mock_exam (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    sequence    INT          NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mock_exam_sequence (sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mock_exam_question (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    mock_exam_id  BIGINT NOT NULL,
    question_id   BIGINT NOT NULL,
    display_order INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_meq_question (mock_exam_id, question_id),
    UNIQUE KEY uk_meq_order (mock_exam_id, display_order),
    CONSTRAINT fk_meq_exam FOREIGN KEY (mock_exam_id) REFERENCES mock_exam(id) ON DELETE CASCADE,
    CONSTRAINT fk_meq_question FOREIGN KEY (question_id) REFERENCES question(id),
    INDEX idx_meq_mock_exam_id (mock_exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- solve.subject_id NULL 허용 (모의고사 풀이는 단일 과목이 아님)
ALTER TABLE solve MODIFY COLUMN subject_id BIGINT NULL;

-- solve.mock_exam_id 컬럼 추가
ALTER TABLE solve
    ADD COLUMN mock_exam_id BIGINT NULL,
    ADD CONSTRAINT fk_solve_mock_exam FOREIGN KEY (mock_exam_id) REFERENCES mock_exam(id) ON DELETE SET NULL,
    ADD INDEX idx_solve_mock_exam_id (mock_exam_id);
