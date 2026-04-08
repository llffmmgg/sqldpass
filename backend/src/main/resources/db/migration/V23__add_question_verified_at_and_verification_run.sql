ALTER TABLE question
    ADD COLUMN verified_at DATETIME NULL AFTER exported_at;

CREATE INDEX idx_question_verified_at ON question (verified_at);

CREATE TABLE question_verification_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_type VARCHAR(30) NULL,
    subject_id BIGINT NULL,
    subject_name VARCHAR(100) NULL,
    limit_requested INT NOT NULL,
    force_recheck BIT NOT NULL,
    processed_count INT NOT NULL,
    suspicious_count INT NOT NULL,
    completed_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_question_verification_run_subject
        FOREIGN KEY (subject_id) REFERENCES subject (id)
);

CREATE INDEX idx_question_verification_run_completed_at
    ON question_verification_run (completed_at DESC);
