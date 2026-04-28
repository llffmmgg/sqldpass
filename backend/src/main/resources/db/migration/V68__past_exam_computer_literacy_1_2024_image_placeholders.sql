-- 컴활 1급 2024년 1, 2회 — 그림 placeholder 보강 + Q45 정답 정정
--
-- V63/V64 에서 [그림: ...] 텍스트 placeholder 가 사용자에게 그대로 노출되어
-- 시트/대화 상자/도식 정보 없이는 풀이가 어려운 상태였다. 16개 문항을
-- 마크다운 표 또는 SVG 이미지로 보강한다.
--
-- 추가로 V64 Q45 의 ⚠️ 정답 재확인 필요 안내 단락을 정리하고, 일반 표준 정답
-- (③ 프린터 선택은 [페이지 설정]이 아닌 [인쇄] 대화 상자의 기능)으로 정정한다.
-- V63 Q21 채우기 핸들 정답 보기는 원문 정답표를 그대로 유지한다 (정답 ②).

SET @cl1_24_1 := (
    SELECT id FROM mock_exam
    WHERE exam_type = 'COMPUTER_LITERACY_1' AND kind = 'PAST_EXAM'
      AND exam_year = 2024 AND exam_round = 1
    LIMIT 1
);

SET @cl1_24_2 := (
    SELECT id FROM mock_exam
    WHERE exam_type = 'COMPUTER_LITERACY_1' AND kind = 'PAST_EXAM'
      AND exam_year = 2024 AND exam_round = 2
    LIMIT 1
);

-- ====================================================================
-- 컴활 1급 2024년 1회 (V63)
-- ====================================================================

-- Q21 채우기 핸들 → SVG
UPDATE question
SET content = REPLACE(content,
        '[그림: A1:C1 영역에 5, 3, 1이 입력되어 있으며, 이 범위를 선택한 뒤 F1까지 채우기 핸들을 드래그하는 상황]',
        '![A1:C1에 5, 3, 1이 입력되어 있고 채우기 핸들로 F1까지 드래그하는 워크시트](/exam-images/computer-literacy-1-2024-1/q21.svg)'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_1 AND display_order = 21;

-- Q30 OFFSET → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: B3 셀을 기준으로 행 -1, 열 +2만큼 이동한 위치의 값이 ‘박태훈’인 워크시트]',
        '|     | A    | B          | C    | D          |\n|-----|------|------------|------|------------|\n| 1   | 사번 | 이름       | 지역 | 비고       |\n| 2   | A001 | 윤혜주     | 서울 | **박태훈** |\n| 3   | A002 | (B3 기준)  | 고양 |            |'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_1 AND display_order = 30;

-- Q35 부분합 → 마크다운 표 + 안내
UPDATE question
SET content = REPLACE(content,
        '[그림: 부서명 기준으로 정렬된 데이터에 부분합이 적용되어 있고, 데이터 아래에 요약이 표시되며 개요 수준 3이 선택된 상태]',
        '| 부서명 | 매출 |\n|--------|-----:|\n| 영업1부 | 100 |\n| 영업1부 | 150 |\n| **영업1부 최대** | **150** |\n| **영업1부 최소** | **100** |\n| 영업2부 | 200 |\n| 영업2부 | 250 |\n| **영업2부 최대** | **250** |\n| **영업2부 최소** | **200** |\n| **총 최대** | **250** |\n| **총 최소** | **100** |\n\n> 정렬: 부서명 오름차순 / 요약 위치: 데이터 아래 / 개요 수준: 3 (개요 기호 3 선택됨)'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_1 AND display_order = 35;

-- Q39 DSUM → 마크다운 표 두 개
UPDATE question
SET content = REPLACE(content,
        '[그림: A1:D7 범위의 데이터베이스에서 B1:B2 조건 범위가 ‘부서=영업1부’를 나타내며, 4번째 필드인 2/4분기 값의 합계를 구하는 시트]',
        '**<DB 범위 A1:D7>**\n\n|   | A      | B       | C       | D       |\n|---|--------|---------|--------:|--------:|\n| 1 | 이름   | 부서    | 1/4분기 | 2/4분기 |\n| 2 | 김민서 | 영업1부 | 30      | 25      |\n| 3 | 이지훈 | 영업2부 | 35      | 40      |\n| 4 | 박서연 | 영업1부 | 40      | 30      |\n| 5 | 한가람 | 영업3부 | 28      | 50      |\n| 6 | 정유진 | 영업2부 | 22      | 35      |\n| 7 | 한도현 | 영업3부 | 18      | 45      |\n\n**<조건 범위 B1:B2>**\n\n|   | B       |\n|---|---------|\n| 1 | 부서    |\n| 2 | 영업1부 |'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_1 AND display_order = 39;

-- Q41 참조 무결성 ERD → SVG
UPDATE question
SET content = REPLACE(content,
        '[그림: <고객> 테이블의 고객번호가 기본키이고, <제품정보> 테이블의 고객번호가 외래키로 연결된 1:N 관계]',
        '![고객(고객번호 PK)과 제품정보(고객번호 FK)가 1:N 관계로 연결된 ERD](/exam-images/computer-literacy-1-2024-1/q41.svg)'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_1 AND display_order = 41;

-- Q48 보고서 → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: 주문일 2022-01-03, 2022-01-05를 기준으로 그룹화되어 있고, 각 주문일별 소계가 그룹 바닥글에 표시된 보고서]',
        '**<일자별주문내역 보고서>**\n\n| 주문일 | 순번 | 제품명 | 수량 | 판매가격 | 유통기한 |\n|---|---:|---|---:|---:|---|\n| 2022-01-03 | 1 | 아메리카노 | 2 | 3,000 | 2024-12-31 |\n|            | 2 | (중복 숨김) | 1 | 3,500 | 2024-11-30 |\n|            |   | **소계**    |   | **6,500** |          |\n| 2022-01-05 | 1 | 카페라떼   | 3 | 4,000 | 2025-01-15 |\n|            | 2 | (중복 숨김) | 2 | 4,500 | 2025-02-20 |\n|            |   | **소계**    |   | **21,000** |         |\n\n> 그룹화: 주문일 / 소계 위치: 그룹 바닥글 / 순번: 컨트롤 원본 =1, 누적 합계 = 그룹'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_1 AND display_order = 48;

-- ====================================================================
-- 컴활 1급 2024년 2회 (V64)
-- ====================================================================

-- Q21 여행시간 합계 → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: 여행 출발 계획표에서 여행시간 11:00, 12:00, 8:00, 7:00의 합계가 D7 셀에 38:00으로 표시되어야 하는 상황]',
        '**<여행 출발 계획표>**\n\n|   | A | B | C | D (여행시간) |\n|---|---|---|---|---:|\n| 3 | ... | ... | ... | 11:00 |\n| 4 | ... | ... | ... | 12:00 |\n| 5 | ... | ... | ... |  8:00 |\n| 6 | ... | ... | ... |  7:00 |\n| 7 |   |   | **합계** | **38:00** |'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 21;

-- Q27 TEXT/이메일 ID 추출 → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: B3 셀에 daehun@hanmail.net이 있고, C3 셀에 @ 앞의 daehun을 대문자 DAEHUN으로 표시하려는 워크시트]',
        '|   | A | B                       | C        |\n|---|---|-------------------------|----------|\n| 1 |   | 전자우편                | ID       |\n| 2 |   |                         |          |\n| 3 |   | daehun@hanmail.net      | **DAEHUN** |\n| 4 |   | mina@gmail.com          | MINA     |\n| 5 |   | jihoon@naver.com        | JIHOON   |\n| 6 |   | seoyeon@daum.net        | SEOYEON  |'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 27;

-- Q32 데이터 테이블 → 마크다운 표 + 안내
UPDATE question
SET content = REPLACE(content,
        '[그림: B4:B8에 할인율 10%, 20%, 30%, 40%, 50%가 세로로 입력되어 있고 C3에는 =D2*A3 수식이 있으며, 데이터 테이블 대화 상자의 행 입력 셀/열 입력 셀을 설정하는 상황]',
        '**<워크시트>**\n\n|   | A      | B    | C        | D    |\n|---|--------|-----:|---------:|----:|\n| 2 |        |      |          | 100  |\n| 3 | 가중치 |      | =D2*A3  |      |\n| 4 |        | 10%  |          |      |\n| 5 |        | 20%  |          |      |\n| 6 |        | 30%  |          |      |\n| 7 |        | 40%  |          |      |\n| 8 |        | 50%  |          |      |\n\n> [B3:C8] 셀을 지정한 후 [데이터] → [가상 분석] → [데이터 표] 메뉴를 실행하여 행/열 입력 셀을 설정한다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 32;

-- Q33 시나리오 요약 → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: 시나리오 요약 보고서에서 현재 값, 할인율 증가, 할인율 감소 시나리오가 표시되고, 변경 셀은 $A$3, 결과 셀은 $D$2인 상황]',
        '**<시나리오 요약>**\n\n|             | 현재 값 | 할인율 증가 | 할인율 감소 |\n|-------------|--------:|------------:|------------:|\n| **변경 셀** |         |             |             |\n| $A$3        |   20%   |    30%      |    10%      |\n| **결과 셀** |         |             |             |\n| $D$2        |  8,000  |   7,000     |   9,000     |\n\n> 변경 셀: $A$3 / 결과 셀: $D$2'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 33;

-- Q34 통합 문서 보호 → SVG
UPDATE question
SET content = REPLACE(content,
        '[그림: 통합 문서 보호 대화 상자에서 보호 대상 ‘구조’가 선택된 상태]',
        '![통합 문서 보호 대화 상자에서 보호 대상 구조 체크박스가 선택된 상태](/exam-images/computer-literacy-1-2024-2/q34.svg)'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 34;

-- Q37 근속년수 → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: A2:C5 범위에 성명, 입사일자, 거주지가 있으며, 이진수의 입사일자는 2022-02-26이다. B7 셀에는 이진수, B8 셀에는 근속년수를 구하려는 상황]',
        '|   | A        | B           | C      |\n|---|----------|-------------|--------|\n| 1 | 성명     | 입사일자    | 거주지 |\n| 2 | 김민서   | 2018-05-10  | 서울   |\n| 3 | 이지훈   | 2020-09-15  | 부산   |\n| 4 | 이진수   | 2022-02-26  | 대전   |\n| 5 | 박서연   | 2023-11-01  | 광주   |\n| 6 |          |             |        |\n| 7 | 성명     | 이진수      |        |\n| 8 | 근속년수 | **?**       |        |'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 37;

-- Q38 순환 참조 → 마크다운 표 + 안내
UPDATE question
SET content = REPLACE(content,
        '[그림: D2 셀에 수식을 입력하는데 계산 범위 B2:D2 안에 수식이 입력되는 D2 셀 자체가 포함된 상황]',
        '|   | A | B   | C   | D                  |\n|---|---|----:|----:|-------------------:|\n| 2 |   | 10  | 20  | **=SUM(B2:D2)** ⚠️ |\n\n> [D2] 셀의 계산 범위 [B2:D2]가 [D2] 자기 자신을 포함하므로 수식이 자기 자신을 참조한다.'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 38;

-- Q45 [페이지 설정] 대화 상자 — ⚠️ 안내 단락 제거 + 정답 ① → ③ 정정
UPDATE question
SET correct_option = 3,
    explanation = '정답: 3. 프린터 선택은 [인쇄] 대화 상자에서 하는 작업이며, [페이지 설정] 대화 상자에서는 다루지 않는다.\n\n**오답 풀이**\n- 1번: 용지 방향(세로/가로)은 [페이지 설정] → [페이지] 탭에서 설정한다.\n- 2번: 인쇄 여백은 [페이지 설정] → [여백] 탭에서 설정한다.\n- 3번: 프린터 선택은 [인쇄] 대화 상자에서 한다. (정답)\n- 4번: 머리글/바닥글은 [페이지 설정] → [머리글/바닥글] 탭에서 설정한다.\n\n**보충 개념**\n[페이지 설정]은 인쇄 시 용지, 여백, 방향, 머리글/바닥글 등 출력 형식을 조정하는 기능이다. 프린터 자체를 선택하거나 인쇄 매수를 지정하는 작업은 [인쇄] 대화 상자에서 한다.',
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 45;

-- Q49 Access 보고서 → 마크다운 표
UPDATE question
SET content = REPLACE(content,
        '[그림: ‘일자별주문내역’ 보고서에서 주문일별로 그룹이 나뉘고, 각 그룹 내에서 판매가격과 유통기한이 오름차순으로 정렬된 보고서]',
        '**<일자별주문내역 보고서>**\n\n| 주문일 | 제품명 | 판매가격 ↑ | 유통기한 ↑ |\n|---|---|---:|---|\n| **2022-01-03** | 아메리카노 | 3,000 | 2024-11-30 |\n|                | 카페모카   | 3,500 | 2024-12-31 |\n| **2022-01-05** | 카페라떼   | 4,000 | 2025-01-15 |\n|                | 에스프레소 | 4,500 | 2025-02-20 |\n\n> 그룹화: 주문일 / 정렬: 판매가격 오름차순 → 유통기한 오름차순'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 49;

-- Q55 우편 라벨 → SVG
UPDATE question
SET content = REPLACE(content,
        '[그림: 여러 사람의 주소와 이름이 라벨지 형태로 여러 칸에 배치된 우편물 레이블 출력물]',
        '![A4 라벨지에 이름과 주소가 격자로 배치된 우편물 레이블 출력물](/exam-images/computer-literacy-1-2024-2/q55.svg)'),
    updated_at = NOW(6)
WHERE mock_exam_id = @cl1_24_2 AND display_order = 55;
