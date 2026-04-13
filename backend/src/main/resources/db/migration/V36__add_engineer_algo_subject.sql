-- 정보처리기사 실기 — "자료구조/알고리즘" 카테고리 추가
-- 기존 루트 "정보처리기사 실기" 아래에 자식 과목으로 추가

INSERT INTO subject (parent_id, name, display_order, created_at, updated_at)
SELECT id, '자료구조/알고리즘', 10, NOW(6), NOW(6)
FROM subject
WHERE name = '정보처리기사 실기' AND parent_id IS NULL;
