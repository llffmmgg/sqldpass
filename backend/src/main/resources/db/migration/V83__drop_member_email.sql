-- V82 (member.email) 정리 — 회원 도메인에 결제용 PII 미저장 정책으로 회귀.
-- KG이니시스 customer 정보는 V84 에서 payment 테이블에 결제 시점 수집/저장한다.
ALTER TABLE member DROP COLUMN email;
