-- SQLD 제56회 — explanation 안 "**복원 보강 안내**" 단락 제거
-- V66 에서 자기완결형 문항으로 보강하면서 explanation 끝에 작업 메모성
-- "**복원 보강 안내**\n원문 ... 보강한 문항이다." 단락이 사용자 노출 텍스트로 그대로 남았다.
-- 풀이 후 해설을 펼치면 "원문이 소실되었다" 안내가 그대로 보이는 회귀를 정리한다.
--
-- Q43 은 V66 에서 해당 단락을 추가하지 않았으므로 제외.
-- Q44 는 지문 "결과와 동일한 것은?" 과 보기 4번 "처리하는 쿼리" 의 톤이
-- 어긋나 지문도 함께 다듬는다.

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
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 ERD 이미지가 소실되어 관계 조건을 텍스트로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 7;

-- Q13
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 결과 표가 소실되어 동일 개념을 풀 수 있는 자기완결형 예시로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 13;

-- Q26
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 지문과 일부 보기가 소실되어 동일 주제를 묻는 자기완결형 문항으로 보강했다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 26;

-- Q40
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 테이블과 조건이 소실되어 기존 보기 구조에 맞는 예시 데이터와 쿼리로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 40;

-- Q41
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 테이블과 쿼리 본문이 소실되어 기존 정답 방향을 유지하는 예시 데이터로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 41;

-- Q42
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 쿼리 일부가 소실되어 기록에 남은 조인 조건을 기준으로 완전한 SQL 형태로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 42;

-- Q44 — 안내 단락 제거 + 지문/보기 톤 정리
UPDATE question
SET content = '다음 UPDATE 쿼리의 동작에 대한 설명으로 옳은 것은?\n\n```sql\nUPDATE 선수 A\nSET (팀명, 키) = (\n    SELECT B.팀명, B.키\n    FROM 선수 B\n    WHERE B.팀명 = A.팀명\n      AND B.포지션 = ''G''\n      AND B.키 = (\n          SELECT MIN(C.키)\n          FROM 선수 C\n          WHERE C.팀명 = A.팀명\n            AND C.포지션 = ''G''\n      )\n);\n```\n\n① 조건에 맞는 행의 기존 값을 그대로 유지한다.\n② 팀별 최장신 선수의 팀명과 키로 갱신한다.\n③ 전체 선수 중 최단신 한 명의 값으로만 갱신한다.\n④ 같은 팀에 포지션 ''G'' 인 선수가 없으면 해당 행의 팀명·키가 NULL 로 갱신된다.',
    explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 쿼리 일부가 소실되어 동일 개념을 설명할 수 있는 완전한 상관 서브쿼리 예시로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 44;

-- Q45
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 쿼리와 테이블이 소실되어 결과 계산 개념이 유지되도록 예시 데이터로 보강한 문항이다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 45;

-- Q46
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 전체가 소실된 문항으로, 동일 난이도의 핵심 SQLD 개념 문항으로 보강했다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 46;

-- Q49
UPDATE question
SET explanation = REPLACE(
        explanation,
        '\n\n**복원 보강 안내**\n원문 지문이 소실되어 기존 보기 구조를 유지한 자기완결형 GROUP BY 확장 문항으로 보강했다.',
        ''),
    updated_at = NOW(6)
WHERE mock_exam_id = @mid AND display_order = 49;
