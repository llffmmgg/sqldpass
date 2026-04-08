-- 컴퓨터활용능력 1급 필기 자격증 추가
-- 구조: 단일 루트("컴퓨터활용능력 1급 필기") + 3개 과목 자식
--       정처기 실기와 동일 패턴 (parent 트리 2단계)
--
-- 표준 출제 기준 (대한상공회의소 컴활 1급 필기):
-- 1과목: 컴퓨터 일반 (20문항)
-- 2과목: 스프레드시트 일반 (20문항)
-- 3과목: 데이터베이스 일반 (20문항)
-- 총 60문항, 4지선다, 합격 기준 60점/100점, 과락 40점

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at)
VALUES (NULL, '컴퓨터활용능력 1급 필기', 4, NOW(6), NOW(6));

SET @cl1_root = LAST_INSERT_ID();

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at) VALUES
    (@cl1_root, '컴퓨터 일반', 1, NOW(6), NOW(6)),
    (@cl1_root, '스프레드시트 일반', 2, NOW(6), NOW(6)),
    (@cl1_root, '데이터베이스 일반', 3, NOW(6), NOW(6));
