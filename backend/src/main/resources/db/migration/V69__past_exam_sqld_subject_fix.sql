-- SQLD 일부 기출 회차의 시드 작성 시 잘못 들어간 subject_id 정정.
-- 표준 SQLD 시험 구조: 1번~10번 = 1과목 (subject 3 또는 4) / 11번~50번 = 2과목 (subject 5,6,7).
-- 사용자 보고로 확인된 깨진 행:
--   55회 7번  — subject_id=7 (2과목 leaf "관리 구문") 로 잘못 입력 → 1과목으로 정정
--   57회 7번  — subject_id=6 (2과목 leaf "SQL 활용")   로 잘못 입력 → 1과목으로 정정
--   54회 10번 — subject_id=5 (2과목 leaf "SQL 기본")   로 잘못 입력 → 1과목으로 정정
--
-- 정확한 leaf("데이터 모델링의 이해" 3 vs "데이터 모델과 SQL" 4)까지는 본문 검증이 필요하므로
-- 가장 일반적인 1과목 leaf 인 subject_id=3 ("데이터 모델링의 이해") 으로 일괄 정정한다.
-- 합격/과락 판정은 leaf 의 parent("1과목: 데이터 모델링의 이해") 단위로 이루어지므로
-- 이 정정만으로 과목별 점수와 번호 그리드가 모두 정상화된다.

UPDATE question q
JOIN mock_exam m ON m.id = q.mock_exam_id
SET q.subject_id = 3,
    q.updated_at = NOW(6)
WHERE m.exam_type = 'SQLD'
  AND m.kind = 'PAST_EXAM'
  AND (
        (m.exam_round = 55 AND q.display_order = 7)
     OR (m.exam_round = 57 AND q.display_order = 7)
     OR (m.exam_round = 54 AND q.display_order = 10)
  )
  AND q.subject_id IN (5, 6, 7);
