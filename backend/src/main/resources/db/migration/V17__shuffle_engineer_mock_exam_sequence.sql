-- 정처기 실기 모의고사의 sequence(회차 번호)를 무작위로 재배치한다.
-- 기존 회차 번호가 생성 순서대로 매겨져 있어 사용자에게 노출되는 순서가 단조롭기 때문.
-- name 컬럼("정보처리기사 실기 모의고사 N회")도 새 sequence와 동기화한다.
--
-- 주의: (exam_type, sequence)에 unique constraint(uk_mock_exam_exam_type_sequence)가 걸려 있어
-- 직접 UPDATE 시 충돌이 발생한다. 따라서 기존 값을 일시적으로 음수 영역으로 옮긴 뒤
-- ROW_NUMBER() OVER (ORDER BY RAND())로 새 양수 sequence를 할당한다.

-- 1) 충돌 회피용 임시 음수 영역으로 이동
UPDATE mock_exam
SET sequence = -id - 100000
WHERE exam_type = 'ENGINEER_PRACTICAL';

-- 2) 랜덤 순서로 새 sequence 할당 + name 동기화
UPDATE mock_exam m
INNER JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY RAND()) AS new_seq
    FROM mock_exam
    WHERE exam_type = 'ENGINEER_PRACTICAL'
) ranked ON m.id = ranked.id
SET m.sequence = ranked.new_seq,
    m.name = CONCAT('정보처리기사 실기 모의고사 ', ranked.new_seq, '회');
