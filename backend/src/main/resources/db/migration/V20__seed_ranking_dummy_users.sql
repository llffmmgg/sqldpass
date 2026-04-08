-- 랜딩 페이지 랭킹 초기 노출용 더미 사용자 + 풀이 데이터
-- 운영 초반 빈 랭킹이 어색하지 않도록 10~29점 분포로 15명 시드.
-- 진짜 사용자가 풀이 누적할수록 자연스럽게 밀려 내려가는 구조.
--
-- provider='SEED'로 구분 → 향후 정리 필요 시 WHERE provider='SEED'로 일괄 삭제 가능.

INSERT INTO member (provider, provider_id, nickname, created_at, updated_at) VALUES
('SEED', 'seed-001', '도전중인감자',  NOW(6), NOW(6)),
('SEED', 'seed-002', 'SQL입문생',     NOW(6), NOW(6)),
('SEED', 'seed-003', '코드한입',      NOW(6), NOW(6)),
('SEED', 'seed-004', '쿼리탐험가',    NOW(6), NOW(6)),
('SEED', 'seed-005', '주말코더',      NOW(6), NOW(6)),
('SEED', 'seed-006', '디버그러버',    NOW(6), NOW(6)),
('SEED', 'seed-007', '인덱스마니아',  NOW(6), NOW(6)),
('SEED', 'seed-008', '조인의달인',    NOW(6), NOW(6)),
('SEED', 'seed-009', '서브쿼리덕후',  NOW(6), NOW(6)),
('SEED', 'seed-010', '트랜잭션맨',    NOW(6), NOW(6)),
('SEED', 'seed-011', '스키마장인',    NOW(6), NOW(6)),
('SEED', 'seed-012', 'NULL탐험가',    NOW(6), NOW(6)),
('SEED', 'seed-013', 'GROUPBY러',     NOW(6), NOW(6)),
('SEED', 'seed-014', '정처기준비생',  NOW(6), NOW(6)),
('SEED', 'seed-015', '모의고사매니아', NOW(6), NOW(6));

-- 각 시드 사용자에게 풀이 1건씩 — correct_count는 10~29 분포
-- subject_id=5 ('SQL 기본')은 V2 시드에서 보장됨
INSERT INTO solve (member_id, subject_id, total_count, correct_count, score, created_at, updated_at)
SELECT id, 5, 30,
       CASE provider_id
         WHEN 'seed-001' THEN 13
         WHEN 'seed-002' THEN 26
         WHEN 'seed-003' THEN 11
         WHEN 'seed-004' THEN 22
         WHEN 'seed-005' THEN 18
         WHEN 'seed-006' THEN 15
         WHEN 'seed-007' THEN 27
         WHEN 'seed-008' THEN 21
         WHEN 'seed-009' THEN 10
         WHEN 'seed-010' THEN 19
         WHEN 'seed-011' THEN 24
         WHEN 'seed-012' THEN 14
         WHEN 'seed-013' THEN 28
         WHEN 'seed-014' THEN 17
         WHEN 'seed-015' THEN 12
       END AS correct_count,
       CASE provider_id
         WHEN 'seed-001' THEN 43
         WHEN 'seed-002' THEN 87
         WHEN 'seed-003' THEN 37
         WHEN 'seed-004' THEN 73
         WHEN 'seed-005' THEN 60
         WHEN 'seed-006' THEN 50
         WHEN 'seed-007' THEN 90
         WHEN 'seed-008' THEN 70
         WHEN 'seed-009' THEN 33
         WHEN 'seed-010' THEN 63
         WHEN 'seed-011' THEN 80
         WHEN 'seed-012' THEN 47
         WHEN 'seed-013' THEN 93
         WHEN 'seed-014' THEN 57
         WHEN 'seed-015' THEN 40
       END AS score,
       NOW(6), NOW(6)
FROM member
WHERE provider = 'SEED';
