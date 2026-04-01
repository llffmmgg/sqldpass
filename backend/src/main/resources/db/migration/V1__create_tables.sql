CREATE TABLE member (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    provider      VARCHAR(20)  NOT NULL,
    provider_id   VARCHAR(100) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NULL,
    profile_image VARCHAR(500) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_provider (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subject (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id     BIGINT       NULL,
    name          VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    subject_id BIGINT      NOT NULL,
    content    TEXT        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_question_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    INDEX idx_question_subject_id (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question_option (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    question_id   BIGINT       NOT NULL,
    option_number TINYINT      NOT NULL,
    content       VARCHAR(500) NOT NULL,
    is_correct    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_question_option_question FOREIGN KEY (question_id) REFERENCES question (id),
    UNIQUE KEY uk_question_option_number (question_id, option_number),
    CONSTRAINT chk_option_number CHECK (option_number BETWEEN 1 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE explanation (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    question_id BIGINT      NOT NULL,
    content     TEXT        NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_explanation_question FOREIGN KEY (question_id) REFERENCES question (id),
    UNIQUE KEY uk_explanation_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
