INSERT INTO subject (id, parent_id, name, display_order, created_at, updated_at) VALUES
(1, NULL, '1과목: 데이터 모델링의 이해', 1, NOW(6), NOW(6)),
(2, NULL, '2과목: SQL 기본 및 활용', 2, NOW(6), NOW(6)),
(3, 1, '데이터 모델링의 이해', 1, NOW(6), NOW(6)),
(4, 1, '데이터 모델과 SQL', 2, NOW(6), NOW(6)),
(5, 2, 'SQL 기본', 1, NOW(6), NOW(6)),
(6, 2, 'SQL 활용', 2, NOW(6), NOW(6)),
(7, 2, '관리 구문', 3, NOW(6), NOW(6));
