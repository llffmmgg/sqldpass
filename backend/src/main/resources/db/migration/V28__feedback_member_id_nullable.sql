-- 회원 탈퇴(hard delete) 시 작성 피드백을 익명 보존하기 위해 member_id 를 nullable 로 전환.
-- 탈퇴 처리에서 member_id 를 NULL 로 세팅 → 어드민 화면에 '탈퇴한 회원' 으로 표시.

ALTER TABLE feedback
    MODIFY COLUMN member_id BIGINT NULL COMMENT '작성 회원 (탈퇴 시 NULL)';
