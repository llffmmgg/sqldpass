CREATE TABLE solve (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    subject_id    BIGINT       NOT NULL,
    total_count   INT          NOT NULL,
    correct_count INT          NOT NULL,
    score         INT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_solve_member_id (member_id),
    CONSTRAINT fk_solve_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_solve_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE solve_answer (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    solve_id        BIGINT      NOT NULL,
    question_id     BIGINT      NOT NULL,
    selected_option TINYINT     NOT NULL,
    correct_option  TINYINT     NOT NULL,
    is_correct      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_solve_answer_solve_id (solve_id),
    INDEX idx_solve_answer_question_id (question_id),
    CONSTRAINT fk_solve_answer_solve FOREIGN KEY (solve_id) REFERENCES solve (id),
    CONSTRAINT fk_solve_answer_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT chk_selected_option CHECK (selected_option BETWEEN 1 AND 4),
    CONSTRAINT chk_correct_option CHECK (correct_option BETWEEN 1 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
