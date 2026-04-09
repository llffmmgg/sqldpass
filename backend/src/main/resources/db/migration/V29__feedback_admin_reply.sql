-- 어드민이 피드백에 직접 답변을 작성할 수 있도록 컬럼 추가.
-- 답변 저장 시 status 가 자동 RESOLVED 로 전이되며 작성자에게 인앱 알림이 발송된다.

ALTER TABLE feedback
    ADD COLUMN admin_reply TEXT NULL COMMENT '어드민 답변',
    ADD COLUMN replied_at  DATETIME(6) NULL COMMENT '답변 작성/수정 시각';
