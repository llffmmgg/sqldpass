-- SQLD 제56회 Q47, Q48 — 자기완결형 재구성
-- V45 가 [복원 메모] 만 제거한 뒤 V66 reconstruction 에 포함되지 않아
-- 본문이 메타 요약("…문제", "…관련 문제 중 첫 번째") 만 남은 두 문항을
-- V66 패턴(content/correct_option/explanation 동시 갱신)으로 보강한다.
-- explanation 끝의 "**복원 보강 안내**" 단락은 V67 정리 패턴을 따라 처음부터 포함하지 않는다.
--
-- Q47 — ORDER BY DESC 누락 시나리오. 인접 Q27 의 (가)/(나)/(다)/(라) 패턴을 따르되
--        직원별 매출 데이터로 컨텍스트를 차별화한다. 정답 4 유지.
-- Q48 — ROLLUP 그룹 집합 식별. 인접 Q49(narrative 형태)와 구분되도록 (A, B), (A), ()
--        그룹 집합을 직접 명시하는 형태로 묻는다. 정답 2 유지.

SET @mid := (
    SELECT id
    FROM mock_exam
    WHERE exam_type = 'SQLD'
      AND kind = 'PAST_EXAM'
      AND exam_round = 56
    LIMIT 1
);

-- Q47
UPDATE question
SET content = '매출액이 높은 순서대로 직원별 총 매출액을 출력하려고 한다. 아래 SQL 에서 고쳐야 할 부분은?\n\n**<SALES>**\n| EMP_ID | AMOUNT |\n|---:|---:|\n| 101 | 3000 |\n| 102 | 5000 |\n| 101 | 2500 |\n| 103 | 4000 |\n| 102 | 1500 |\n\n```sql\nSELECT EMP_ID, SUM(AMOUNT)   -- (가)\n  FROM SALES                 -- (나)\n WHERE AMOUNT > 0            -- (다)\n GROUP BY EMP_ID\n ORDER BY SUM(AMOUNT)        -- (라)\n```\n\n① (가)\n② (나)\n③ (다)\n④ (라)',
    correct_option = 4,
    explanation = '정답: 4. 매출액이 높은 순서대로 정렬하려면 ORDER BY SUM(AMOUNT) DESC 처럼 내림차순을 명시해야 한다. ORDER BY 의 기본 정렬 방향은 ASC(오름차순) 이므로 DESC 를 빼면 매출 합계가 작은 직원부터 출력된다.\n\n**오답 풀이**\n- 1번: (가) SELECT 절은 직원 ID 와 매출 합계를 정확히 조회하고 있어 수정할 필요가 없다.\n- 2번: (나) FROM 절의 테이블 지정은 문제 의도와 무관하다.\n- 3번: (다) WHERE 절은 행 필터링 조건이며 정렬 방향과 관련이 없다.\n- 4번: (라) ORDER BY 절에 DESC 가 누락되어 있으므로 ORDER BY SUM(AMOUNT) DESC 로 고쳐야 한다.\n\n**보충 개념**\nORDER BY 의 기본값은 ASC(오름차순) 이다. ''높은 순서'', ''큰 값부터'' 같은 요구사항은 DESC 를 반드시 명시해야 한다. SELECT 절에서 부여한 별칭을 ORDER BY 에서 그대로 사용할 수도 있다 (예: SELECT SUM(AMOUNT) AS TOTAL … ORDER BY TOTAL DESC).',
    topic = 'ORDER BY',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 47;

-- Q48
UPDATE question
SET content = '아래와 같이 `(A, B)` 상세 집계, `(A)` 상위 단계 소계, 전체 합계 `()` 의 세 그룹 집합만 생성하는 GROUP BY 확장 기능으로 가장 적절한 것은?\n\n**<생성되는 그룹 집합>**\n- `(A, B)` — 상세 집계\n- `(A)` — 상위 단계 소계\n- `()` — 전체 합계\n\n① CUBE(A, B)\n② ROLLUP(A, B)\n③ GROUPING SETS((A, B), (B), ())\n④ GROUPING(A, B)',
    correct_option = 2,
    explanation = '정답: 2. ROLLUP(A, B) 는 지정한 컬럼 순서에 따라 `(A, B)`, `(A)`, `()` 의 계층적 그룹 집합을 차례로 생성한다.\n\n**오답 풀이**\n- 1번: CUBE(A, B) 는 가능한 모든 조합인 `(A, B)`, `(A)`, `(B)`, `()` 네 그룹을 생성하므로 `(B)` 가 추가로 포함된다.\n- 2번: ROLLUP(A, B) 의 결과 그룹 집합이 문제의 요구와 정확히 일치한다.\n- 3번: GROUPING SETS((A, B), (B), ()) 는 `(A)` 가 빠지고 대신 `(B)` 가 포함되어 요구와 다르다.\n- 4번: GROUPING(A, B) 는 집계 행 여부를 1/0 으로 판별하는 함수이며 그룹 집합을 생성하는 절이 아니다.\n\n**보충 개념**\nROLLUP 은 가장 왼쪽 컬럼부터 차례로 묶음을 줄여나가는 계층 집계이므로 (부서, 직무) 별 상세 → (부서) 별 소계 → 전체 합계처럼 상위 단계로 올라가는 리포트에 적합하다. CUBE 는 가능한 모든 조합, GROUPING SETS 는 임의 조합을 직접 명시할 때 사용한다.',
    topic = 'ROLLUP',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 48;
