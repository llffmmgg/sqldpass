-- SQLD 제55회 9번 문제 — SVG 박스 안 정답 단어를 (가)/(나)/(다)/(라) 자리표시자로 치환
-- 지문이 "다음 그림의 빈칸에 들어갈 ..." 인데 SVG 박스에 정답 단어가 그대로 노출되어 있어
-- 빈칸 문제가 성립하지 않던 것을 보정한다. 보기 ①~④, 정답, 해설은 그대로 유지.

SET @mid := (
    SELECT id FROM mock_exam
    WHERE exam_type = 'SQLD' AND kind = 'PAST_EXAM' AND exam_round = 55
    LIMIT 1
);

UPDATE question
SET content = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(content,
        'aria-label="엔터티 인스턴스 속성 속성값의 관계"',
        'aria-label="데이터 모델링 구성 요소의 계층 관계 빈칸"'),
        'class="txt">엔터티</text>',   'class="txt">(가)</text>'),
        'class="txt">인스턴스</text>', 'class="txt">(나)</text>'),
        'class="txt">속성</text>',     'class="txt">(다)</text>'),
        'class="txt">속성값</text>',   'class="txt">(라)</text>'),
        'class="small">학생 엔터티 → 홍길동 인스턴스</text>',
        'class="small">학생 (가) → 홍길동 (나)</text>'),
        'class="small">학번 속성 → 20240001 속성값</text>',
        'class="small">학번 (다) → 20240001 (라)</text>'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 9;
