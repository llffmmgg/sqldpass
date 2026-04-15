-- 데이터분석 준전문가(ADsP) 자격증 추가
-- 구조: 단일 루트("데이터분석 준전문가(ADsP)") + 3개 과목 자식
--
-- 표준 출제 기준 (한국데이터산업진흥원 ADsP):
-- 1과목: 데이터 이해 (10문항)
-- 2과목: 데이터 분석 기획 (10문항)
-- 3과목: 데이터 분석 (30문항)
-- 총 50문항, 4지선다(2024년 제40회부터 전 문항 객관식)
-- 합격 기준: 평균 60점 이상 + 과목별 40점 이상(과락 방지)

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at)
VALUES (NULL, '데이터분석 준전문가(ADsP)', 7, NOW(6), NOW(6));

SET @adsp_root = LAST_INSERT_ID();

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at) VALUES
    (@adsp_root, '데이터 이해', 1, NOW(6), NOW(6)),
    (@adsp_root, '데이터 분석 기획', 2, NOW(6), NOW(6)),
    (@adsp_root, '데이터 분석', 3, NOW(6), NOW(6));
