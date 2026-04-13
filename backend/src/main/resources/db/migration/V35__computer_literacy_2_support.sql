-- 컴퓨터활용능력 2급 필기 자격증 추가
-- 구조: 단일 루트("컴퓨터활용능력 2급 필기") + 2개 과목 자식
--
-- 표준 출제 기준 (대한상공회의소 컴활 2급 필기):
-- 1과목: 컴퓨터 일반 (20문항)
-- 2과목: 스프레드시트 일반 (20문항)
-- 총 40문항, 4지선다, 합격 기준 60점/100점, 과락 40점
-- (1급과 달리 데이터베이스 일반 과목이 없음)

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at)
VALUES (NULL, '컴퓨터활용능력 2급 필기', 6, NOW(6), NOW(6));

SET @cl2_root = LAST_INSERT_ID();

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at) VALUES
    (@cl2_root, '컴퓨터 일반', 1, NOW(6), NOW(6)),
    (@cl2_root, '스프레드시트 일반', 2, NOW(6), NOW(6));
