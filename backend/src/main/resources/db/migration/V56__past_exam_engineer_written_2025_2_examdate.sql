-- ENGINEER_WRITTEN 2025년 제2회 기출 복원 — exam_date 보완
--
-- V55 적재 시 사용자 입력 examDate 가 빈 문자열이라 NULL 로 들어갔다.
-- 큐넷 일정 기준 정보처리기사 필기 2025년 정기 2회 시험일 = 2025-05-10.

UPDATE mock_exam
   SET exam_date = '2025-05-10',
       updated_at = NOW(6)
 WHERE exam_type = 'ENGINEER_WRITTEN'
   AND kind = 'PAST_EXAM'
   AND exam_year = 2025
   AND exam_round = 2
   AND exam_date IS NULL;
