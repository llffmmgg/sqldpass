-- 사이트 공지사항 (admin이 수정할 수 있는 상단 배너 / 진입 모달).
-- display_type 별로 active=true인 가장 최근 1건이 노출 대상.

CREATE TABLE site_notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    display_type VARCHAR(16)  NOT NULL COMMENT 'BANNER / MODAL',
    title        VARCHAR(200) NULL COMMENT '모달 헤더 (배너는 비어있을 수 있음)',
    body         TEXT         NOT NULL,
    active       TINYINT(1)   NOT NULL DEFAULT 0,
    version      INT          NOT NULL DEFAULT 1 COMMENT '수정 시 +1 → 프론트 dismiss 키 무효화',
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_site_notice_active (display_type, active, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
