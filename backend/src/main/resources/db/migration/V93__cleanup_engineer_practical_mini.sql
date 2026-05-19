-- 정처기 실기 미니(8문 분포) 회차 전체 정리. 10문 신분포 재생성 전 슬레이트 초기화.
-- SQLD/ADsP/컴활/필기 MINI 는 영향 없음 (exam_type = 'ENGINEER_PRACTICAL' 로 한정).

-- 1) 정처기 실기 미니 회차에 link 된 복제 문제의 풀이 응답 삭제 (FK 위반 방지)
DELETE FROM solve_answer
 WHERE question_id IN (
       SELECT id FROM question
        WHERE mock_exam_id IN (
              SELECT id FROM mock_exam
               WHERE kind = 'MINI' AND exam_type = 'ENGINEER_PRACTICAL'
        )
 );

-- 2) 풀이 세션의 mock_exam_id 만 NULL 로 — 풀이 횟수/이력은 유지
UPDATE solve
   SET mock_exam_id = NULL,
       updated_at = NOW(6)
 WHERE mock_exam_id IN (
       SELECT id FROM mock_exam
        WHERE kind = 'MINI' AND exam_type = 'ENGINEER_PRACTICAL'
 );

-- 3) 미니 회차의 복제 question row 삭제 (원본은 다른 mock_exam_id 라 영향 없음)
DELETE FROM question
 WHERE mock_exam_id IN (
       SELECT id FROM mock_exam
        WHERE kind = 'MINI' AND exam_type = 'ENGINEER_PRACTICAL'
 );

-- 4) ENGINEER_PRACTICAL MINI mock_exam 자체 삭제
DELETE FROM mock_exam
 WHERE kind = 'MINI' AND exam_type = 'ENGINEER_PRACTICAL';

-- 5) ENGINEER_PRACTICAL 정규 회차 원본 문제의 마킹 리셋 → 풀에 부활
UPDATE question
   SET included_in_mini_at = NULL,
       updated_at = NOW(6)
 WHERE included_in_mini_at IS NOT NULL
   AND mock_exam_id IN (
       SELECT id FROM mock_exam
        WHERE exam_type = 'ENGINEER_PRACTICAL'
   );
