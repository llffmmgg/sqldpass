-- KG이니시스 신용카드 결제 customer.email 수집용. V10 에서 DROP 됐던 컬럼 재추가.
-- nullable + UNIQUE 없음 — 기존 회원은 다음 로그인 시 점진적 백필.
-- email_verified=true 인 경우에만 채워짐 (GoogleOAuthClient 정책).
ALTER TABLE member ADD COLUMN email VARCHAR(255) NULL AFTER nickname;
