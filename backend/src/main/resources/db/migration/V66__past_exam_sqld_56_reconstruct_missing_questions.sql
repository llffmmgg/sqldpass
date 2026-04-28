-- SQLD 제56회 기출 복원 보강
-- 원문 일부가 소실된 문항 중 풀이가 성립하지 않던 항목을
-- 개념과 기존 정답 방향을 유지하는 범위에서 자기완결형 문항으로 보강한다.

SET @mid := (
    SELECT id
    FROM mock_exam
    WHERE exam_type = 'SQLD'
      AND kind = 'PAST_EXAM'
      AND exam_round = 56
    LIMIT 1
);

-- Q7
UPDATE question
SET content = '다음 관계 조건을 보고 옳지 않은 것을 고르시오.\n\n**<관계 조건>**\n- 병원은 0명 이상의 의사를 둘 수 있다.\n- 의사는 반드시 1개의 병원에 소속된다.\n- 의사는 0건 이상의 수술을 집도할 수 있다.\n- 수술은 담당 의사 없이 등록될 수 있다.\n- ㄱ은 병원-의사 관계, ㄴ은 의사-수술 관계, ㄷ은 병원-수술을 직접 연결한 관계이다.\n\n① ㄷ은 ㄱ과 ㄴ을 합한 것과 의미가 같다.\n② 의사가 없는 병원이 있을 수 있다.\n③ 수술이 없는 의사가 있을 수 있다.\n④ 의사가 집도하지 않는 수술이 있을 수 있다.',
    correct_option = 1,
    explanation = '정답: 1. 병원-수술을 직접 연결한 관계(ㄷ)는 병원-의사, 의사-수술 두 관계를 단순히 합친 것과 같지 않다.\n\n**오답 풀이**\n- 1번: 직접 관계는 중간 엔터티인 의사 정보와 참여 조건을 생략하므로 ㄱ과 ㄴ의 합성과 동일하지 않다.\n- 2번: 병원이 0명 이상의 의사를 둘 수 있다고 했으므로 가능하다.\n- 3번: 의사는 0건 이상의 수술을 집도할 수 있다고 했으므로 가능하다.\n- 4번: 수술은 담당 의사 없이 등록될 수 있다고 했으므로 가능하다.\n\n**보충 개념**\nERD 해석에서는 관계 수뿐 아니라 선택성, 필수 참여, 중간 엔터티 유무까지 함께 봐야 한다. 직접 관계와 경유 관계는 의미가 달라질 수 있다.\n\n**복원 보강 안내**\n원문 ERD 이미지가 소실되어 관계 조건을 텍스트로 보강한 문항이다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 7;

-- Q13
UPDATE question
SET content = '다음 두 테이블에 대해 결과 건수가 가장 많은 조인을 고르시오.\n\n**<A>**\n| ID |\n|---:|\n| 1 |\n| 2 |\n| 3 |\n\n**<B>**\n| ID |\n|---:|\n| 2 |\n| 3 |\n| 4 |\n\n① SELECT * FROM A INNER JOIN B ON A.ID = B.ID\n② SELECT * FROM A LEFT OUTER JOIN B ON A.ID = B.ID\n③ SELECT * FROM A RIGHT OUTER JOIN B ON A.ID = B.ID\n④ SELECT * FROM A FULL OUTER JOIN B ON A.ID = B.ID',
    correct_option = 4,
    explanation = '정답: 4. A와 B에 각각만 존재하는 값 1, 4까지 모두 포함하는 FULL OUTER JOIN의 결과 건수가 가장 많다.\n\n**오답 풀이**\n- 1번: 공통값 2, 3만 반환하므로 2건이다.\n- 2번: A의 모든 행을 보존하므로 3건이다.\n- 3번: B의 모든 행을 보존하므로 3건이다.\n- 4번: 양쪽의 미매칭 행까지 포함하므로 4건이다.\n\n**보충 개념**\nINNER JOIN은 교집합, LEFT/RIGHT OUTER JOIN은 한쪽 기준 보존, FULL OUTER JOIN은 양쪽 기준 보존이다.\n\n**복원 보강 안내**\n원문 결과 표가 소실되어 동일 개념을 풀 수 있는 자기완결형 예시로 보강한 문항이다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 13;

-- Q26
UPDATE question
SET content = '다음 중 중복을 제거한 합집합을 반환하는 집합 연산자는?\n\n① UNION\n② UNION ALL\n③ INTERSECT\n④ MINUS',
    correct_option = 1,
    explanation = '정답: 1. UNION은 두 SELECT 결과를 합치면서 중복 행을 제거한다.\n\n**오답 풀이**\n- 1번: 중복 제거한 합집합이다.\n- 2번: 중복을 제거하지 않는 합집합이다.\n- 3번: 교집합을 반환한다.\n- 4번: 차집합을 반환한다.\n\n**보충 개념**\n집합 연산자는 컬럼 수와 대응 데이터 타입이 맞아야 사용할 수 있다. 중복 제거 여부는 UNION과 UNION ALL의 핵심 차이이다.\n\n**복원 보강 안내**\n원문 지문과 일부 보기가 소실되어 동일 주제를 묻는 자기완결형 문항으로 보강했다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 26;

-- Q40
UPDATE question
SET content = '다음 SQL의 실행 결과로 옳은 것은?\n\n**<PLAYER>**\n| NAME | TEAM | HEIGHT |\n|---|---|---:|\n| PARK | Tigers | 190 |\n| KIM | Bears | 180 |\n| LEE | Tigers | NULL |\n\n```sql\nSELECT NAME\nFROM PLAYER\nWHERE TEAM = ''Tigers''\n  AND HEIGHT > 185;\n```\n\n① PARK\n② PARK, KIM\n③ PARK, LEE\n④ PARK, KIM, LEE',
    correct_option = 1,
    explanation = '정답: 1. TEAM이 Tigers이면서 HEIGHT가 185보다 큰 행은 PARK뿐이다.\n\n**오답 풀이**\n- 1번: 조건을 모두 만족하는 유일한 행이다.\n- 2번: KIM은 TEAM 조건을 만족하지 않는다.\n- 3번: LEE는 HEIGHT가 NULL이므로 `> 185` 조건을 만족하지 않는다.\n- 4번: KIM과 LEE 모두 제외된다.\n\n**보충 개념**\n비교 연산에서 NULL은 TRUE가 아니라 UNKNOWN으로 처리되므로 WHERE 절에서 제외된다.\n\n**복원 보강 안내**\n원문 테이블과 조건이 소실되어 기존 보기 구조에 맞는 예시 데이터와 쿼리로 보강한 문항이다.',
    topic = 'SQL 결과',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 40;

-- Q41
UPDATE question
SET content = '다음 SQL의 실행 결과로 옳은 것은?\n\n**<EMP>**\n| BONUS | SALARY |\n|---:|---:|\n| 1000 | 3000 |\n| 1000 | 2500 |\n| NULL | 4000 |\n\n```sql\nSELECT AVG(BONUS), MIN(SALARY)\nFROM EMP;\n```\n\n① 1000, 2000\n② 1000, 3000\n③ 1000, 2500\n④ NULL, 2500',
    correct_option = 3,
    explanation = '정답: 3. AVG(BONUS)는 NULL을 제외하고 (1000 + 1000) / 2 = 1000이고, MIN(SALARY)는 2500이다.\n\n**오답 풀이**\n- 1번: SALARY 최솟값이 2000이 아니다.\n- 2번: MIN(SALARY)는 3000이 아니라 2500이다.\n- 3번: AVG와 MIN 계산이 모두 맞다.\n- 4번: AVG는 집계 대상에 NULL이 일부 포함되어도 전체 결과가 NULL이 되지 않는다.\n\n**보충 개념**\nAVG, MIN, MAX, SUM 등 대부분의 집계 함수는 NULL을 제외하고 계산한다.\n\n**복원 보강 안내**\n원문 테이블과 쿼리 본문이 소실되어 기존 정답 방향을 유지하는 예시 데이터로 보강한 문항이다.',
    topic = '집계 함수',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 41;

-- Q42
UPDATE question
SET content = '다음 쿼리에 대한 설명 중 옳은 것은?\n\n```sql\nSELECT 사원.사원명, 부서.부서명\nFROM 사원\n     JOIN 부서\n       ON 부서.부서ID = 사원.부서ID\n      AND 사원.등급 >= 2;\n```\n\n① 2등급 이상의 사원을 모두 출력한다.\n② 부서에 소속되지 않은 사원은 제외한다.\n③ 모든 부서와 모든 사원을 조건 없이 출력한다.\n④ 등급 조건은 조인 결과에 영향을 주지 않는다.',
    correct_option = 2,
    explanation = '정답: 2. INNER JOIN 조건을 만족하지 못하는 사원은 결과에서 제외되므로, 부서ID가 매칭되지 않는 사원은 출력되지 않는다.\n\n**오답 풀이**\n- 1번: 등급 조건을 만족해도 부서와 매칭되지 않으면 제외된다.\n- 2번: 조인 조건을 만족하지 못하면 출력되지 않는다.\n- 3번: 조인 조건이 있으므로 카테시안 곱이 아니다.\n- 4번: `사원.등급 >= 2` 조건은 결과 행 수에 직접 영향을 준다.\n\n**보충 개념**\nINNER JOIN의 ON 조건은 조인 대상 행을 제한한다. WHERE 절에 있든 ON 절에 있든 논리적으로 결과를 줄이는 조건이 될 수 있다.\n\n**복원 보강 안내**\n원문 쿼리 일부가 소실되어 기록에 남은 조인 조건을 기준으로 완전한 SQL 형태로 보강한 문항이다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 42;

-- Q43
UPDATE question
SET content = '키가 180 초과인 농구선수를 구하는 문제\n\n① 키 조건 없이 모든 농구선수를 조회하는 쿼리\n② WHERE 키 < 180 조건을 사용하는 쿼리\n③ WHERE 키 > 180 조건을 사용하는 쿼리\n④ GROUP BY 키 조건만 사용하는 쿼리',
    explanation = '정답: 3. ''180 초과''는 `> 180` 조건으로 표현해야 한다.\n\n**오답 풀이**\n- 1번: 키 조건이 없으면 원하는 대상만 조회할 수 없다.\n- 2번: 180보다 작은 선수를 조회하므로 반대 조건이다.\n- 3번: 180 초과 조건을 정확히 표현한다.\n- 4번: GROUP BY는 집계용 절이며 행 필터링 조건을 대신할 수 없다.\n\n**보충 개념**\n''이상''은 `>=`, ''초과''는 `>`로 표현한다. 지문 표현과 SQL 비교 연산자를 일치시키는 것이 중요하다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 43;

-- Q44
UPDATE question
SET content = '다음 쿼리의 결과와 동일한 것은?\n\n```sql\nUPDATE 선수 A\nSET (팀명, 키) = (\n    SELECT B.팀명, B.키\n    FROM 선수 B\n    WHERE B.팀명 = A.팀명\n      AND B.포지션 = ''G''\n      AND B.키 = (\n          SELECT MIN(C.키)\n          FROM 선수 C\n          WHERE C.팀명 = A.팀명\n            AND C.포지션 = ''G''\n      )\n);\n```\n\n① 조건에 맞는 행만 유지하고 나머지는 기존 값을 유지하는 쿼리\n② 팀별 최장신의 팀명과 키를 출력하는 쿼리\n③ 전체 선수 중 최단신 한 명만 출력하는 쿼리\n④ 조건에 맞지 않는 경우 NULL로 처리하는 쿼리',
    correct_option = 4,
    explanation = '정답: 4. 상관 서브쿼리 결과가 없는 행은 UPDATE 대상 컬럼이 NULL로 설정되므로, 조건에 맞는 가드가 없는 팀은 NULL 처리된다.\n\n**오답 풀이**\n- 1번: 서브쿼리 결과가 없을 때 기존 값을 유지하지 않는다.\n- 2번: MIN(키)를 사용하므로 최장신이 아니라 최단신 기준이다.\n- 3번: 팀별 상관 조건이 있으므로 전체 최단신 한 명만 구하는 것이 아니다.\n- 4번: 조건에 맞는 값이 없으면 NULL이 들어간다.\n\n**보충 개념**\nUPDATE의 스칼라 서브쿼리는 행별로 평가된다. 일치하는 결과가 없으면 대상 컬럼이 NULL로 바뀔 수 있다는 점을 주의해야 한다.\n\n**복원 보강 안내**\n원문 쿼리 일부가 소실되어 동일 개념을 설명할 수 있는 완전한 상관 서브쿼리 예시로 보강한 문항이다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 44;

-- Q45
UPDATE question
SET content = '결과 값으로 옳은 것은?\n\n**<매출>**\n| 금액 |\n|---:|\n| 300 |\n| 500 |\n| NULL |\n\n```sql\nSELECT SUM(NVL(금액, 0)) AS RESULT\nFROM 매출;\n```\n\n① 800\n② 1600\n③ 2975\n④ 오류가 발생한다.',
    correct_option = 1,
    explanation = '정답: 1. NULL을 0으로 바꾼 뒤 합계를 구하므로 300 + 500 + 0 = 800이다.\n\n**오답 풀이**\n- 1번: NVL 처리 후 합계가 맞다.\n- 2번: 존재하지 않는 추가 값이 포함된 계산이다.\n- 3번: 데이터와 맞지 않는 결과이다.\n- 4번: NVL과 SUM 조합은 정상적으로 실행 가능하다.\n\n**보충 개념**\nSUM은 원래 NULL을 제외하지만, NVL을 함께 사용하면 NULL을 특정 값으로 바꿔 집계 기준을 명시할 수 있다.\n\n**복원 보강 안내**\n원문 쿼리와 테이블이 소실되어 결과 계산 개념이 유지되도록 예시 데이터로 보강한 문항이다.',
    topic = 'SQL 결과',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 45;

-- Q46
UPDATE question
SET content = '다음 중 NULL 값을 찾는 조건으로 옳은 것은?\n\n① COL1 IS NULL\n② COL1 = NULL\n③ COL1 <> NULL\n④ COL1 IN (NULL)',
    correct_option = 1,
    explanation = '정답: 1. NULL 비교는 `=`가 아니라 `IS NULL` 또는 `IS NOT NULL`을 사용해야 한다.\n\n**오답 풀이**\n- 1번: NULL 판별에 맞는 문법이다.\n- 2번: `= NULL`은 TRUE가 되지 않는다.\n- 3번: `<> NULL`도 TRUE가 되지 않는다.\n- 4번: `IN (NULL)`로는 NULL 값을 찾을 수 없다.\n\n**보충 개념**\nSQL의 NULL은 값이 아니라 미정 상태이므로 일반 비교 연산자로 판단하지 않는다.\n\n**복원 보강 안내**\n원문 전체가 소실된 문항으로, 동일 난이도의 핵심 SQLD 개념 문항으로 보강했다.',
    topic = 'NULL 비교',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 46;

-- Q49
UPDATE question
SET content = '다음 중 `(부서, 직무)`별 상세 집계와 `부서`별 소계, 전체 합계를 계층적으로 함께 출력하기에 가장 적절한 것은?\n\n① ROLLUP()\n② GROUPING()\n③ GROUPING SETS()\n④ CUBE()',
    correct_option = 1,
    explanation = '정답: 1. 계층적인 소계와 총계를 순서대로 만들 때는 ROLLUP이 가장 적절하다.\n\n**오답 풀이**\n- 1번: `(부서, 직무)`, `(부서)`, `()` 형태의 계층적 집계를 만들 수 있다.\n- 2번: GROUPING은 집계 행 여부를 판별하는 함수이다.\n- 3번: GROUPING SETS는 원하는 조합을 직접 나열할 때 주로 사용한다.\n- 4번: CUBE는 가능한 모든 조합의 소계를 생성한다.\n\n**보충 개념**\nROLLUP(A, B)은 상세 `(A, B)`와 상위 단계 `(A)`, 전체 합계 `()`를 순서대로 생성한다.\n\n**복원 보강 안내**\n원문 지문이 소실되어 기존 보기 구조를 유지한 자기완결형 GROUP BY 확장 문항으로 보강했다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mid
  AND display_order = 49;
