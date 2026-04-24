-- SQLD 제56회 — content 안 [복원 메모:] 블록 정리
-- 풀이 성립 가능한 6문항 은 제거, 원문 정보가 꼭 필요한 8문항 은 blockquote notice 로 전환.
-- QuestionContent.tsx 의 blockquote 스타일(amber 경고 톤)을 재사용.

SET @mid := (
    SELECT id FROM mock_exam
    WHERE exam_type = 'SQLD' AND kind = 'PAST_EXAM' AND exam_round = 56
    LIMIT 1
);

-- =========================================================
-- 제거 (6문항) — 지문·보기만으로 풀이 성립
-- =========================================================

-- Q14 — 드모르간 분석으로 보기 충분
UPDATE question
SET content = REPLACE(content, '\n\n[복원 메모: 원래 테이블이 주어지고 빈칸을 채우는 문제였으며, 2번 선지는 정확하지 않음]', ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 14;

-- Q34 — 지문에 "셀프조인" 이미 명시
UPDATE question
SET content = REPLACE(content, '\n\n[복원 메모: 사원 테이블을 자기 자신과 조인하는 셀프조인 문제]', ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 34;

-- Q43 — 메모가 단순 정답 힌트
UPDATE question
SET content = REPLACE(content, '\n\n[복원 메모: WHERE 키 > 180 조건이 있는 3번이 정답으로 기록됨]', ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 43;

-- Q47 — 메모가 단순 정답 힌트
UPDATE question
SET content = REPLACE(content, '\n\n[복원 메모: ORDER BY에 DESC가 없어서 DESC로 수정해야 하며 4번이 정답으로 기록됨]', ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 47;

-- Q48 — 메모가 단순 정답 힌트
UPDATE question
SET content = REPLACE(content, '\n\n[복원 메모: ROLLUP()이 정답 후보로 기록됨]', ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 48;

-- Q49 — 메모가 보기 목록과 중복
UPDATE question
SET content = REPLACE(content, '\n\n[복원 메모: GROUPING SETS()와 CUBE()가 보기로 기록됨]', ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 49;

-- =========================================================
-- Notice 전환 (8문항) — blockquote 로 사용자에게 안내
-- =========================================================

-- Q13 — JOIN 결과 건수 비교 안내
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: INNER JOIN, LEFT OUTER JOIN, RIGHT OUTER JOIN은 같은 결과 건수이고 FULL OUTER JOIN만 결과 건수가 다른 문제]',
        '> ⚠️ **안내**: 원문 결과 테이블이 소실되어 표기할 수 없습니다. 복원본 기준으로 INNER JOIN / LEFT OUTER JOIN / RIGHT OUTER JOIN 은 결과 건수가 같고, FULL OUTER JOIN 만 결과 건수가 다른 문제였습니다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 13;

-- Q26 — 집합 연산자 지문 누락
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 원문 지문과 3, 4번 보기가 누락됨]',
        '> ⚠️ **안내**: 원문 지문과 일부 보기(3, 4번) 가 소실된 문항입니다. 풀이에 참고만 해주세요.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 26;

-- Q40 — PARK/KIM/LEE 원문 누락
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 원문 테이블과 쿼리 조건이 누락됨]',
        '> ⚠️ **안내**: 원문 테이블과 쿼리 조건이 소실된 문항입니다. 제시된 보기만으로 정답을 특정하기 어렵습니다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 40;

-- Q41 — AVG/MIN 쿼리 누락
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 원문 테이블과 정확한 쿼리가 누락됨]',
        '> ⚠️ **안내**: 원문 테이블과 정확한 쿼리 본문이 소실된 문항입니다. 복원된 보기 기준으로만 참고해주세요.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 41;

-- Q42 — 조인 쿼리 조건 안내
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 부서.부서ID = 사원.부서ID AND 등급 >= 2 조건이 포함된 조인 쿼리]',
        '> ⚠️ **안내**: 원문 쿼리가 일부 소실되었습니다. 조인 조건은 `부서.부서ID = 사원.부서ID AND 등급 >= 2` 였다고 기록되어 있습니다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 42;

-- Q44 — 쿼리 설명 안내
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 팀별 최단신의 팀명과 키를 출력하는 쿼리. 조건에 맞지 않으면 모두 NULL로 업데이트해야 하며 4번이 정답으로 기록됨]',
        '> ⚠️ **안내**: 원문 쿼리가 소실되었습니다. 복원본 기준으로 "팀별 최단신의 팀명·키를 출력하되 조건 불일치 시 NULL 로 업데이트" 하는 쿼리였다고 기록되어 있습니다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 44;

-- Q45 — 쿼리·테이블 누락
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 원문 쿼리와 테이블이 누락됨]',
        '> ⚠️ **안내**: 원문 쿼리와 테이블이 소실된 문항입니다. 결과 값을 확정할 수 없습니다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 45;

-- Q46 — 완전 누락
UPDATE question
SET content = REPLACE(content,
        '[복원 메모: 지문과 보기가 모두 누락됨]',
        '> ⚠️ **안내**: 지문과 보기가 모두 소실된 문항입니다. 원문 재확보 후 재등록 예정입니다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 46;
