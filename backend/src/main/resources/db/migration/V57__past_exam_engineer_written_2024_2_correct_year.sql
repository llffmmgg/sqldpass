-- 정처기 필기 — V55 가 잘못된 연도(2025)로 적재한 회차를 실제 연도(2024)로 정정.
--
-- 사용자가 보낸 입력 JSON 의 examYear 가 2025 였으나 실제 시험은
-- 2024년 정기 기사 2회(2024-05-15)이다. V55 파일은 이미 (2024, 2) 로 수정했지만
-- 운영 DB 에 이미 success=1 로 기록된 V55 는 Flyway repair() 후에도 재실행되지 않으므로
-- 본 마이그레이션이 운영 DB 의 stale (2025, 2) row 의 메타를 직접 정정한다.
--
-- 신규 환경에서는 V55 가 처음부터 (2024, 2) 로 적재하여 본 UPDATE 의 WHERE 절이
-- 매칭하지 않으므로 no-op 으로 안전하게 통과한다.

UPDATE mock_exam
   SET exam_year = 2024,
       exam_date = '2024-05-15',
       name = '정보처리기사 필기 2024년 2회 복원',
       updated_at = NOW(6)
 WHERE exam_type = 'ENGINEER_WRITTEN'
   AND kind = 'PAST_EXAM'
   AND exam_year = 2025
   AND exam_round = 2;
