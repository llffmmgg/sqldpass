-- SQLD 제56회 Q4, Q7 — ERD 이미지 첨부 + Q7 정답(1) 확정
-- [그림: ...] 텍스트 설명을 실제 이미지 마크다운 ![](/exam-images/sqld-56/qN.png) 로 교체.
-- Q7 은 이미지 확보로 정답(①)과 카디널리티 기반 해설을 확정, ⚠️ 표시 제거.

SET @mock_exam_id := (
    SELECT id FROM mock_exam
    WHERE exam_type = 'SQLD'
      AND kind = 'PAST_EXAM'
      AND exam_round = 56
    LIMIT 1
);

-- Q4: 고객-상품-주문-주문상품 ERD
UPDATE question
SET content = '다음 설명 중 옳지 않은 것은?\n\n![고객-상품-주문-주문상품 ERD](/exam-images/sqld-56/q4.png)\n\n① 고객과 상품은 기본 엔터티다.\n② 주문은 사건 엔터티다.\n③ 주문 상품 엔터티는 주문과 상품의 관계에 의해 생성된다.\n④ 주문 상품 엔터티는 독립적으로 발생하는 기본 엔터티다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mock_exam_id
  AND display_order = 4;

-- Q7: 병원-의사-수술 ERD + 정답 1 확정
UPDATE question
SET content = '주어진 ERD에 대한 설명으로 옳지 않은 것은?\n\n![병원-의사-수술 ERD. ㄱ, ㄴ, ㄷ 관계 표기](/exam-images/sqld-56/q7.png)\n\n① ㄷ은 ㄱ과 ㄴ을 합한 것과 의미가 같다.\n② 의사가 없는 병원이 있을 수 있다.\n③ 수술이 없는 의사가 있을 수 있다.\n④ 의사가 집도하지 않는 수술이 있을 수 있다.',
    correct_option = 1,
    explanation = '정답: 1. ㄷ은 병원과 수술을 직접 연결하는 관계로, ㄱ(병원-의사)과 ㄴ(의사-수술)을 거쳐 도출되는 의미와 반드시 같다고 볼 수 없다. 병원이 직접 주관하는 수술과 의사를 경유해 집도되는 수술은 서로 다른 업무 규칙을 표현할 수 있다.\n\n**오답 풀이**\n- 1번: ㄷ 관계는 ㄱ+ㄴ 조합과 의미가 반드시 같지는 않으므로 옳지 않다. (정답)\n- 2번: 병원-의사(ㄱ) 관계에서 의사 쪽이 선택 참여이므로, 소속 의사가 아직 없는 병원이 존재할 수 있다.\n- 3번: 의사-수술(ㄴ) 관계에서 수술 쪽이 선택 참여이므로, 수술을 집도하지 않은 의사가 존재할 수 있다.\n- 4번: 의사-수술(ㄴ) 관계에서 의사 쪽이 선택 참여이므로, 의사가 집도하지 않는 수술이 존재할 수 있다.\n\n**보충 개념**\n중복 관계 여부를 판단할 때는 두 경로가 의미상 같은 사실을 표현하는지, 다른 업무 규칙을 표현하는지 확인해야 한다. 구조가 비슷해 보여도 맥락이 다르면 다른 관계로 남겨두는 것이 타당하다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @mock_exam_id
  AND display_order = 7;
