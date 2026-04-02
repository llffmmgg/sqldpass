CREATE TABLE generation_lock (
    id         INT         NOT NULL DEFAULT 1,
    running    BOOLEAN     NOT NULL DEFAULT FALSE,
    started_at DATETIME(6) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO generation_lock (id, running) VALUES (1, FALSE);
