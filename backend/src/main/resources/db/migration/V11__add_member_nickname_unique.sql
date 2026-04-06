-- 닉네임 유니크 제약 추가 (랭킹 기능을 위한 사전 조치)
-- 기존 데이터에 중복이 있으면 마이그레이션 실패하므로, 먼저 중복 확인 후 적용
ALTER TABLE member ADD CONSTRAINT uk_member_nickname UNIQUE (nickname);
