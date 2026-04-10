-- 정보처리기사 필기 과목 트리 (5과목, 각 20문항 = 총 100문항)
INSERT INTO subject (parent_id, name, display_order, created_at, updated_at)
VALUES (NULL, '정보처리기사 필기', 10, NOW(6), NOW(6));

SET @ew_root = LAST_INSERT_ID();

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at) VALUES
    (@ew_root, '소프트웨어 설계',       1, NOW(6), NOW(6)),
    (@ew_root, '소프트웨어 개발',       2, NOW(6), NOW(6)),
    (@ew_root, '데이터베이스 구축',     3, NOW(6), NOW(6)),
    (@ew_root, '프로그래밍 언어 활용',  4, NOW(6), NOW(6)),
    (@ew_root, '정보시스템 구축 관리',  5, NOW(6), NOW(6));
