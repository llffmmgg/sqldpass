-- 미니 모의고사 v1 구로직(출처 1:1:1 + 난이도 단일 필터)로 발급된 회차/복제 문제/풀이 흔적을 일괄 정리.
-- 신로직(과목 quota 만 보존, 출처/난이도 폐기) 적용 전 클린 슬레이트.
-- 사용자에게는 미니 회차가 노출된 적이 없어(별도 탭 미도입) 사용자 풀이 이력이 묶여있을 가능성은 낮지만,
-- 어드민 테스트 풀이가 있을 수 있으므로 SolveAnswer 만 제거하고 Solve 자체는 mock_exam_id 만 끊어 이력 보존.

-- 1) 미니 회차에 link 된 복제 문제들의 풀이 응답 행 삭제
--    (복제 question 을 곧 DELETE 할 거라 FK 위반 방지)
DELETE FROM solve_answer
 WHERE question_id IN (
       SELECT id FROM question
        WHERE mock_exam_id IN (SELECT id FROM mock_exam WHERE kind = 'MINI')
 );

-- 2) 미니 회차에 묶인 풀이 세션의 mock_exam_id 만 NULL 로 — 풀이 횟수/이력은 유지
UPDATE solve
   SET mock_exam_id = NULL,
       updated_at = NOW(6)
 WHERE mock_exam_id IN (SELECT id FROM mock_exam WHERE kind = 'MINI');

-- 3) 미니 회차의 복제 question row 삭제 (원본은 다른 mock_exam_id 라 영향 없음)
DELETE FROM question
 WHERE mock_exam_id IN (SELECT id FROM mock_exam WHERE kind = 'MINI');

-- 4) 미니 회차(mock_exam) 자체 삭제
DELETE FROM mock_exam WHERE kind = 'MINI';

-- 5) 원본 문제 마킹 리셋 → 신로직에서 모든 풀 후보로 부활
UPDATE question
   SET included_in_mini_at = NULL,
       updated_at = NOW(6)
 WHERE included_in_mini_at IS NOT NULL;
