package com.sqldpass.service.generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 토픽별 Few-shot 예시 (HTML 기출 기반).
 * 각 토픽당 기본(0)/심화(1)/고난도(2) 3개씩.
 */
class TopicExamples {

    // 토픽 → [기본, 심화, 고난도] 예시 JSON
    static final Map<String, List<String>> EXAMPLES = new LinkedHashMap<>();

    static {
        EXAMPLES.put("데이터 모델링 개요", List.of(
            """
            {"content":"데이터 모델링의 3단계를 올바른 순서로 나열한 것은?\\n\\n① 개념 → 논리 → 물리\\n② 논리 → 개념 → 물리\\n③ 물리 → 논리 → 개념\\n④ 개념 → 물리 → 논리","correctOption":1,"explanation":"모델링은 개념(추상화) → 논리(정확한 Key·속성·관계 표현) → 물리(실제 DB 구현) 순서로 진행됩니다.","summary":"데이터 모델링 3단계 순서","difficulty":0}""",
            """
            {"content":"3층 스키마(Three-Level Schema)에서 '모든 사용자 관점을 통합한 조직 전체 관점의 데이터 구조'에 해당하는 것은?\\n\\n① 외부 스키마\\n② 개념 스키마\\n③ 내부 스키마\\n④ 뷰 스키마","correctOption":2,"explanation":"외부 스키마: 개별 사용자 관점. 개념 스키마: 조직 전체 통합 관점. 내부 스키마: 물리적 저장 구조.","summary":"3층 스키마에서 개념 스키마의 역할","difficulty":1}""",
            """
            {"content":"다음 중 데이터 독립성에 대한 설명으로 가장 부적절한 것은?\\n\\n① 논리적 독립성은 개념 스키마 변경 시 외부 스키마에 영향이 없는 것이다\\n② 물리적 독립성은 내부 스키마 변경 시 개념 스키마에 영향이 없는 것이다\\n③ 외부/개념 사상은 물리적 독립성을 보장한다\\n④ 개념/내부 사상은 물리적 독립성을 보장한다","correctOption":3,"explanation":"외부/개념 사상 → 논리적 독립성, 개념/내부 사상 → 물리적 독립성입니다. ③이 사상과 독립성을 잘못 연결.","summary":"사상(Mapping)과 데이터 독립성 유형의 연결","difficulty":2}"""
        ));

        EXAMPLES.put("엔터티", List.of(
            """
            {"content":"다음 중 엔터티의 분류가 아닌 것은?\\n\\n① 기본 엔터티\\n② 중심 엔터티\\n③ 행위 엔터티\\n④ 속성 엔터티","correctOption":4,"explanation":"엔터티는 기본(독립적), 중심(기본에서 파생), 행위(2개 이상 부모에서 파생)로 분류. '속성 엔터티'는 없는 분류.","summary":"엔터티 3분류 — 속성 엔터티는 없음","difficulty":0}""",
            """
            {"content":"엔터티의 특성으로 가장 부적절한 것은?\\n\\n① 업무에서 필요로 하는 정보여야 한다\\n② 유일한 식별자에 의해 식별 가능해야 한다\\n③ 두 개 이상의 인스턴스가 있어야 한다\\n④ 속성 없이도 엔터티가 될 수 있다","correctOption":4,"explanation":"엔터티는 반드시 속성을 가져야 합니다.","summary":"엔터티 필수 조건 — 속성 보유 여부","difficulty":1}""",
            """
            {"content":"다음 중 엔터티에 대한 설명으로 가장 부적절한 것은?\\n\\n① 엔터티는 업무 프로세스에 의해 이용되어야 한다\\n② 엔터티는 반드시 다른 엔터티와 1개 이상의 관계가 있어야 한다\\n③ 기본 엔터티는 독립적으로 생성되며 다른 엔터티의 부모 역할을 한다\\n④ 행위 엔터티는 하나의 부모 엔터티로부터만 파생된다","correctOption":4,"explanation":"행위 엔터티는 2개 이상의 부모 엔터티로부터 발생합니다.","summary":"행위 엔터티의 부모 엔터티 수","difficulty":2}"""
        ));

        EXAMPLES.put("속성", List.of(
            """
            {"content":"다음 중 파생 속성에 해당하는 것은?\\n\\n① 주민등록번호\\n② 주문일자\\n③ 합계금액\\n④ 상품코드","correctOption":3,"explanation":"기본 속성: 업무에서 직접 추출. 설계 속성: 모델링 과정에서 생성. 파생 속성: 다른 속성에서 계산(합계금액).","summary":"기본/설계/파생 속성 구분","difficulty":0}""",
            """
            {"content":"속성의 특성으로 가장 부적절한 것은?\\n\\n① 하나의 속성은 하나의 값만 가진다\\n② 하나의 엔터티는 두 개 이상의 속성을 갖는다\\n③ 속성은 주식별자에 함수적으로 종속되어야 한다\\n④ 파생 속성이 많을수록 데이터 정합성이 향상된다","correctOption":4,"explanation":"파생 속성이 많으면 정합성이 오히려 저하됩니다.","summary":"파생 속성과 데이터 정합성의 관계","difficulty":1}""",
            """
            {"content":"다음 중 속성, 엔터티, 인스턴스, 속성값의 관계로 올바른 것은?\\n\\n① 하나의 속성은 여러 엔터티에 동시에 포함될 수 있다\\n② 하나의 인스턴스는 각 속성에 대해 반드시 하나의 값을 갖는다\\n③ 속성은 인스턴스의 개수와 관계없이 변할 수 있다\\n④ 하나의 속성이 여러 개의 값을 가질 수 있다","correctOption":2,"explanation":"하나의 인스턴스는 각 속성에 대해 하나의 속성값을 가집니다(원자값).","summary":"인스턴스와 속성값의 1:1 관계 — 원자값 원칙","difficulty":2}"""
        ));

        EXAMPLES.put("관계", List.of(
            """
            {"content":"ERD에서 관계의 표기 요소가 아닌 것은?\\n\\n① 관계명\\n② 관계차수(Cardinality)\\n③ 관계선택성(Optionality)\\n④ 관계인덱스","correctOption":4,"explanation":"관계의 3가지 표기 요소는 관계명, 관계차수(1:1, 1:M, M:N), 관계선택성(필수/선택).","summary":"관계의 3가지 표기 요소","difficulty":0}""",
            """
            {"content":"두 엔터티 간 M:N 관계를 해소하는 방법으로 올바른 것은?\\n\\n① 외래키를 양쪽에 추가한다\\n② 관계 자체를 삭제한다\\n③ 연결(교차) 엔터티를 추가하여 1:M 관계 2개로 분리한다\\n④ M:N 관계는 물리 모델에서 그대로 구현 가능하다","correctOption":3,"explanation":"M:N 관계는 연결(교차) 엔터티를 생성하여 1:M 관계 2개로 분리합니다.","summary":"M:N 관계 해소 — 교차 엔터티","difficulty":1}""",
            """
            {"content":"다음 중 순환(Recursive/Self) 관계에 대한 설명으로 올바른 것은?\\n\\n① 두 개의 서로 다른 엔터티 간에만 존재할 수 있다\\n② 하나의 엔터티 내에서 자기 자신과 관계를 맺는 것이다\\n③ 순환 관계에서는 식별자 관계만 가능하다\\n④ 순환 관계는 계층형 질의로 조회할 수 없다","correctOption":2,"explanation":"순환 관계는 하나의 엔터티가 자기 자신과 관계를 맺는 것입니다(예: 사원-관리자).","summary":"순환 관계의 정의와 계층형 질의 연관","difficulty":2}"""
        ));

        EXAMPLES.put("식별자", List.of(
            """
            {"content":"주식별자의 특징이 아닌 것은?\\n\\n① 유일성\\n② 최소성\\n③ 불변성\\n④ NULL 허용","correctOption":4,"explanation":"주식별자는 유일성, 최소성, 불변성, NOT NULL의 특성. NULL은 절대 허용되지 않습니다.","summary":"주식별자 4가지 특성 — NOT NULL 필수","difficulty":0}""",
            """
            {"content":"인조 식별자에 대한 설명으로 올바른 것은?\\n\\n① 업무적으로 자연 발생하는 식별자이다\\n② 본래 식별자가 복잡할 때 인위적으로 만든 식별자이다\\n③ 외래키를 식별자로 사용하는 것이다\\n④ 대체 식별자와 동일한 개념이다","correctOption":2,"explanation":"인조 식별자는 복잡한 복합키 대신 인위적으로 만든 일련번호 등.","summary":"인조 식별자의 정의와 사용 이유","difficulty":1}""",
            """
            {"content":"식별자 관계와 비식별자 관계에 대한 설명으로 가장 부적절한 것은?\\n\\n① 식별자 관계에서 부모 PK는 자식의 PK에 포함된다\\n② 비식별자 관계에서 부모 PK는 자식의 일반 속성(FK)이 된다\\n③ 식별자 관계는 ERD에서 실선, 비식별자 관계는 점선으로 표현한다\\n④ 비식별자 관계에서 자식은 반드시 부모가 존재해야만 생성된다","correctOption":4,"explanation":"비식별자 관계에서는 부모 없이도 자식이 존재할 수 있습니다(FK에 NULL 허용).","summary":"식별자/비식별자 관계에서 FK NULL 허용 여부","difficulty":2}"""
        ));

        EXAMPLES.put("정규화", List.of(
            """
            {"content":"정규화의 목적으로 가장 올바른 것은?\\n\\n① 데이터 조회 성능 향상\\n② 데이터 중복 제거 및 이상현상 방지\\n③ 테이블 수 최소화\\n④ 인덱스 효율성 향상","correctOption":2,"explanation":"정규화의 핵심 목적은 데이터 중복 제거와 삽입/수정/삭제 이상현상 방지입니다.","summary":"정규화의 핵심 목적","difficulty":0}""",
            """
            {"content":"다음 테이블이 위반하는 정규형은?\\n\\n[주문] 주문번호(PK), 고객번호, 고객명, 주문일자\\n※ 고객명은 고객번호에 의해 결정됨\\n\\n① 제1정규형\\n② 제2정규형\\n③ 제3정규형\\n④ BCNF","correctOption":3,"explanation":"주문번호→고객번호→고객명으로 이행적 함수 종속이 존재 → 제3정규형 위반.","summary":"이행적 함수 종속과 3NF 위반 판별","difficulty":1}""",
            """
            {"content":"다음 테이블이 위반하는 정규형과 해결 방법은?\\n\\n[수강] 학번(PK), 과목코드(PK), 교수명, 성적\\n※ 한 과목은 한 교수만 담당\\n※ 교수명 → 과목코드\\n\\n① 2NF 위반 → 부분 함수 종속 제거\\n② 3NF 위반 → 이행적 종속 제거\\n③ BCNF 위반 → 결정자가 후보키가 아닌 종속 제거\\n④ 정규화 위반 없음","correctOption":3,"explanation":"교수명이 결정자이지만 후보키가 아닙니다 → BCNF 위반.","summary":"BCNF 위반 조건 — 결정자가 후보키인지 판별","difficulty":2}"""
        ));

        EXAMPLES.put("성능 데이터 모델링", List.of(
            """
            {"content":"반정규화의 목적으로 올바른 것은?\\n\\n① 데이터 무결성 강화\\n② 조회 성능 향상을 위한 의도적 중복 허용\\n③ 저장 공간 절약\\n④ 정규화 오류 수정","correctOption":2,"explanation":"반정규화는 성능 향상을 위해 의도적으로 중복, 통합, 분리를 수행하는 것.","summary":"반정규화의 목적 — 조회 성능 향상","difficulty":0}""",
            """
            {"content":"분산 데이터베이스의 투명성 중 '데이터 저장 장소를 몰라도 되는 것'은?\\n\\n① 분할 투명성\\n② 위치 투명성\\n③ 중복 투명성\\n④ 병행 투명성","correctOption":2,"explanation":"위치 투명성: 저장 장소 몰라도 됨. 분할 투명성: 분할 사실 몰라도 됨.","summary":"분산 DB 투명성 유형 — 위치 투명성","difficulty":1}""",
            """
            {"content":"다음 중 테이블 파티셔닝 기법과 설명이 올바르지 않은 것은?\\n\\n① Range: 값의 범위 기준\\n② Hash: 해시 함수 적용\\n③ List: 특정 값 목록 기준\\n④ Composite: 하나의 기준만 반복 적용","correctOption":4,"explanation":"Composite 파티셔닝은 여러 기준을 조합하여 분할하는 것입니다.","summary":"파티셔닝 유형 — Composite의 정의","difficulty":2}"""
        ));

        EXAMPLES.put("DDL", List.of(
            """
            {"content":"다음 중 DDL에 해당하지 않는 것은?\\n\\n① CREATE\\n② ALTER\\n③ TRUNCATE\\n④ UPDATE","correctOption":4,"explanation":"DDL은 CREATE, ALTER, DROP, RENAME, TRUNCATE. UPDATE는 DML.","summary":"DDL vs DML 구분 — UPDATE는 DML","difficulty":0}""",
            """
            {"content":"TRUNCATE와 DELETE의 차이로 올바르지 않은 것은?\\n\\n① TRUNCATE는 DDL, DELETE는 DML\\n② TRUNCATE는 로그를 남기지 않아 복구 불가\\n③ TRUNCATE는 WHERE절을 사용할 수 있다\\n④ TRUNCATE가 DELETE보다 빠르다","correctOption":3,"explanation":"TRUNCATE는 WHERE절 사용 불가. 테이블 전체만 삭제 가능.","summary":"TRUNCATE의 WHERE절 사용 불가","difficulty":1}""",
            """
            {"content":"CTAS에 대한 설명으로 가장 부적절한 것은?\\n\\n① 원본 데이터를 복사하여 새 테이블 생성\\n② NOT NULL 제약조건은 복사된다\\n③ PRIMARY KEY 제약조건도 함께 복사된다\\n④ 데이터 타입은 동일하게 복사된다","correctOption":3,"explanation":"CTAS는 NOT NULL만 복사. PK, FK, INDEX 등은 복사되지 않습니다.","summary":"CTAS에서 복사되는 제약조건 범위","difficulty":2}"""
        ));

        EXAMPLES.put("DML", List.of(
            """
            {"content":"다음 SQL의 결과는?\\n\\n[EMP] NAME: 김,이,박 / SAL: 100,200,300\\n\\nSELECT COUNT(*), SUM(SAL), AVG(SAL) FROM EMP;\\n\\n① 3, 600, 200\\n② 3, 600, 600\\n③ 1, 600, 200\\n④ 오류","correctOption":1,"explanation":"COUNT(*)=3건, SUM=600, AVG=200.","summary":"집계 함수 COUNT, SUM, AVG 기본","difficulty":0}""",
            """
            {"content":"MERGE문에 대한 설명으로 올바르지 않은 것은?\\n\\n① MATCHED 시 UPDATE 수행\\n② NOT MATCHED 시 INSERT 수행\\n③ 하나의 SQL로 INSERT와 UPDATE 동시 처리\\n④ MERGE문에서 DELETE는 사용 불가","correctOption":4,"explanation":"Oracle MERGE문에서 WHEN MATCHED 절 안에서 DELETE도 사용 가능합니다.","summary":"MERGE문에서 DELETE 사용 가능 여부","difficulty":1}""",
            """
            {"content":"다음 SQL 실행 후 결과는? (Oracle)\\n\\n[TAB] COL1: 1,2,3,4,5\\n\\nDELETE FROM TAB WHERE ROWNUM <= 3;\\nSELECT MIN(COL1) FROM TAB;\\n\\n① 1\\n② 4\\n③ 3\\n④ NULL","correctOption":2,"explanation":"ROWNUM<=3으로 처음 3건(1,2,3) 삭제. 남은 데이터: 4,5. MIN=4.","summary":"ROWNUM과 DELETE 조합 실행 결과","difficulty":2}"""
        ));

        EXAMPLES.put("TCL", List.of(
            """
            {"content":"다음 중 TCL이 아닌 것은?\\n\\n① COMMIT\\n② ROLLBACK\\n③ SAVEPOINT\\n④ GRANT","correctOption":4,"explanation":"TCL은 COMMIT, ROLLBACK, SAVEPOINT. GRANT는 DCL.","summary":"TCL vs DCL 구분","difficulty":0}""",
            """
            {"content":"다음 실행 후 남는 데이터 건수는?\\n\\nINSERT INTO T VALUES(1);\\nINSERT INTO T VALUES(2);\\nCOMMIT;\\nINSERT INTO T VALUES(3);\\nSAVEPOINT SP1;\\nINSERT INTO T VALUES(4);\\nROLLBACK TO SP1;\\nCOMMIT;\\n\\n① 2건\\n② 3건\\n③ 4건\\n④ 1건","correctOption":2,"explanation":"1,2 COMMIT 확정. 3→SP1→4→ROLLBACK TO SP1(4취소)→COMMIT. 최종: 1,2,3 = 3건.","summary":"SAVEPOINT와 ROLLBACK TO 실행 결과","difficulty":1}""",
            """
            {"content":"다음 실행 후 SELECT SUM(C2) FROM T;의 결과는?\\n\\nCREATE TABLE T(C1 INT PRIMARY KEY, C2 INT CHECK(C2>100));\\nINSERT INTO T VALUES(1, 200);\\nINSERT INTO T VALUES(2, 300);\\nSAVEPOINT S1;\\nINSERT INTO T VALUES(3, 50); -- CHECK 위반\\nINSERT INTO T VALUES(4, 400);\\nROLLBACK TO S1;\\nINSERT INTO T VALUES(5, 150);\\nCOMMIT;\\n\\n① 650\\n② 500\\n③ 1050\\n④ 850","correctOption":1,"explanation":"(1,200)✓ (2,300)✓ →S1→ (3,50)CHECK위반→실패 → (4,400)✓ → ROLLBACK TO S1 → 3,4모두 취소 → (5,150)✓. SUM=200+300+150=650.","summary":"CHECK 제약위반 + SAVEPOINT ROLLBACK 복합 실행 결과","difficulty":2}"""
        ));

        EXAMPLES.put("WHERE절/조건", List.of(
            """
            {"content":"다음 SQL의 결과 건수는?\\n\\n[TAB] COL1: 10, 20, 30, 40, 50\\n\\nSELECT * FROM TAB WHERE COL1 BETWEEN 20 AND 40;\\n\\n① 2건\\n② 3건\\n③ 4건\\n④ 5건","correctOption":2,"explanation":"BETWEEN은 양쪽 포함. 20,30,40 → 3건.","summary":"BETWEEN의 경계값 포함 여부","difficulty":0}""",
            """
            {"content":"다음 SQL의 결과 건수는?\\n\\n[TAB] COL1: 'ABC','ABCD','AB','ABX','ABBC'\\n\\nSELECT * FROM TAB WHERE COL1 LIKE 'AB_C%';\\n\\n① 1건\\n② 2건\\n③ 3건\\n④ 0건","correctOption":1,"explanation":"_는 정확히 1글자. AB+1글자+C+나머지 → 'ABBC'만 해당 → 1건.","summary":"LIKE 패턴에서 _와 %의 정확한 동작","difficulty":1}""",
            """
            {"content":"다음 SQL의 결과는?\\n\\nSELECT * FROM TAB WHERE COL1 IN ('A','B', NULL);\\n\\n① A, B, NULL인 행 모두 반환\\n② A, B인 행만 반환\\n③ 오류 발생\\n④ 전체 행 반환","correctOption":2,"explanation":"IN에 NULL 포함해도 NULL 행은 반환 안 됨. COL1=NULL은 항상 UNKNOWN → FALSE.","summary":"IN 절에서 NULL 포함 시 동작","difficulty":2}"""
        ));

        EXAMPLES.put("함수", List.of(
            """
            {"content":"다음 SQL의 결과는? (Oracle)\\n\\nSELECT LENGTH('SQL 개발'), SUBSTR('SQL 개발', 2, 3) FROM DUAL;\\n\\n① 6, QL \\n② 5, QL 개\\n③ 6, QL 개\\n④ 7, QL ","correctOption":2,"explanation":"LENGTH=5(공백포함). SUBSTR(2,3)=2번째부터 3글자.","summary":"LENGTH와 SUBSTR의 한글·공백 처리","difficulty":0}""",
            """
            {"content":"다음 SQL의 결과는? (Oracle)\\n\\nSELECT ROUND(156.78, -2), TRUNC(156.78, -1), MOD(15, 4) FROM DUAL;\\n\\n① 200, 150, 3\\n② 100, 150, 3\\n③ 200, 100, 3\\n④ 200, 150, 4","correctOption":1,"explanation":"ROUND(156.78,-2)=200. TRUNC(156.78,-1)=150. MOD(15,4)=3.","summary":"ROUND, TRUNC의 음수 자릿수와 MOD","difficulty":1}""",
            """
            {"content":"다음 SQL의 결과는? (Oracle)\\n\\nSELECT DECODE(COL1, 'A', DECODE(COL2, 1, 'O', 'X'), 'Z') FROM DUAL\\n-- COL1='A', COL2=2\\n\\n① O\\n② X\\n③ Z\\n④ NULL","correctOption":2,"explanation":"외부 DECODE: COL1='A' 매칭 → 내부 DECODE. COL2=2는 1과 불일치 → 디폴트 'X'.","summary":"중첩 DECODE의 실행 흐름","difficulty":2}"""
        ));

        EXAMPLES.put("NULL 처리 함수", List.of(
            """
            {"content":"다음 SQL의 결과는?\\n\\nSELECT NVL(NULL, 'DEFAULT') FROM DUAL;\\n\\n① NULL\\n② DEFAULT\\n③ 오류\\n④ 빈 문자열","correctOption":2,"explanation":"NVL(expr1, expr2): expr1이 NULL이면 expr2 반환.","summary":"NVL 함수 기본 동작","difficulty":0}""",
            """
            {"content":"다음 SQL의 결과는?\\n\\nSELECT COALESCE(NULL, NULL, 3, NULL, 5) FROM DUAL;\\n\\n① NULL\\n② 3\\n③ 5\\n④ 0","correctOption":2,"explanation":"COALESCE는 첫 번째 NULL이 아닌 값 반환. 3이 첫 번째 non-null.","summary":"COALESCE의 첫 번째 non-null 반환","difficulty":1}""",
            """
            {"content":"다음 두 SQL의 결과가 동일한 것은?\\n\\nSELECT CASE WHEN COL1='X' THEN NULL ELSE COL1 END FROM TAB;\\n\\n① NVL(COL1, 'X')\\n② NVL2(COL1, 'X', NULL)\\n③ NULLIF(COL1, 'X')\\n④ COALESCE(COL1, 'X')","correctOption":3,"explanation":"NULLIF(A,B): A=B이면 NULL, 아니면 A. CASE와 동일 로직.","summary":"NULLIF와 CASE WHEN의 등가 변환","difficulty":2}"""
        ));

        EXAMPLES.put("GROUP BY/HAVING", List.of(
            """
            {"content":"GROUP BY에 대한 설명으로 올바르지 않은 것은?\\n\\n① 그룹별 집계 수행\\n② HAVING은 그룹 조건\\n③ GROUP BY에 없는 칼럼도 SELECT 가능\\n④ WHERE는 집계 전, HAVING은 집계 후 필터링","correctOption":3,"explanation":"GROUP BY에 명시하지 않은 비집계 칼럼은 SELECT에 사용 불가.","summary":"GROUP BY와 SELECT 절의 컬럼 제약","difficulty":0}""",
            """
            {"content":"다음 SQL의 결과 행 수는?\\n\\n[EMP] DEPT: 10,10,10,20,20,30 / SAL: 100,200,300,400,500,600\\n\\nSELECT DEPT, AVG(SAL) FROM EMP GROUP BY DEPT HAVING AVG(SAL) >= 300;\\n\\n① 1행\\n② 2행\\n③ 3행\\n④ 6행","correctOption":2,"explanation":"DEPT10:200, DEPT20:450, DEPT30:600. AVG>=300 → 20,30 = 2행.","summary":"HAVING 조건에 의한 그룹 필터링","difficulty":1}""",
            """
            {"content":"다음 SQL의 실행 순서로 올바른 것은?\\n\\nSELECT DEPT, COUNT(*) AS CNT FROM EMP WHERE SAL>100 GROUP BY DEPT HAVING COUNT(*)>=2 ORDER BY CNT;\\n\\n① SELECT→FROM→WHERE→GROUP BY→HAVING→ORDER BY\\n② FROM→WHERE→GROUP BY→HAVING→SELECT→ORDER BY\\n③ FROM→WHERE→GROUP BY→SELECT→HAVING→ORDER BY\\n④ FROM→GROUP BY→WHERE→HAVING→SELECT→ORDER BY","correctOption":2,"explanation":"실행 순서: FROM→WHERE→GROUP BY→HAVING→SELECT→ORDER BY.","summary":"SQL 실행 순서 6단계","difficulty":2}"""
        ));

        EXAMPLES.put("ORDER BY", List.of(
            """
            {"content":"다음 SQL의 정렬 결과 첫 번째 행은?\\n\\n[TAB] NAME: 가,나,다 / SAL: 300,100,200\\n\\nSELECT * FROM TAB ORDER BY SAL DESC;\\n\\n① 가, 300\\n② 나, 100\\n③ 다, 200\\n④ 오류","correctOption":1,"explanation":"DESC → 높은 순: 300(가), 200(다), 100(나).","summary":"ORDER BY DESC 정렬 결과","difficulty":0}""",
            """
            {"content":"Oracle에서 NULL 포함 정렬 시 기본 동작은?\\n\\n[TAB] COL1: 3, NULL, 1, NULL, 2\\n\\nSELECT * FROM TAB ORDER BY COL1;\\n\\n① NULL이 처음, 나머지 오름차순\\n② 1,2,3 후 NULL이 마지막\\n③ NULL 행은 제외\\n④ 오류","correctOption":2,"explanation":"Oracle: ASC시 NULL이 마지막. SQL Server는 반대(ASC시 NULL이 처음).","summary":"Oracle vs SQL Server의 NULL 정렬 기본값","difficulty":1}""",
            """
            {"content":"다음 SQL에서 오류가 발생하는 것은?\\n\\n① SELECT DEPT FROM EMP GROUP BY DEPT ORDER BY DEPT;\\n② SELECT DEPT FROM EMP GROUP BY DEPT ORDER BY MAX(SAL);\\n③ SELECT DEPT FROM EMP GROUP BY DEPT ORDER BY NAME;\\n④ SELECT DEPT, SUM(SAL) S FROM EMP GROUP BY DEPT ORDER BY S;","correctOption":3,"explanation":"GROUP BY 사용 시 ORDER BY에는 GROUP BY 칼럼이거나 집계 함수만 가능. NAME은 둘 다 아님.","summary":"GROUP BY + ORDER BY에서 사용 가능한 컬럼","difficulty":2}"""
        ));

        EXAMPLES.put("JOIN", List.of(
            """
            {"content":"INNER JOIN의 특성으로 올바른 것은?\\n\\n① 양쪽 모든 행 포함\\n② 조건 일치 행만 포함\\n③ 한쪽 모든 행 포함\\n④ 조건 없이 사용 가능","correctOption":2,"explanation":"INNER JOIN은 양쪽 모두 조건이 일치하는 행만 결과에 포함.","summary":"INNER JOIN의 기본 동작","difficulty":0}""",
            """
            {"content":"다음 SQL의 결과 건수는?\\n\\n[A] ID:1,2,3 [B] ID:1,2,4\\n\\nSELECT * FROM A FULL OUTER JOIN B ON A.ID=B.ID;\\n\\n① 2건\\n② 3건\\n③ 4건\\n④ 6건","correctOption":3,"explanation":"매칭: (1,1),(2,2). A만: (3,NULL). B만: (NULL,4). 총 4건.","summary":"FULL OUTER JOIN 결과 건수 계산","difficulty":1}""",
            """
            {"content":"다음 두 SQL의 결과 차이로 올바른 것은?\\n\\n-- SQL1\\nSELECT * FROM A LEFT JOIN B ON A.ID=B.ID WHERE B.COL='X';\\n-- SQL2\\nSELECT * FROM A LEFT JOIN B ON A.ID=B.ID AND B.COL='X';\\n\\n① 결과가 동일\\n② SQL1은 사실상 INNER JOIN, SQL2는 LEFT JOIN 유지\\n③ SQL1은 LEFT JOIN 유지, SQL2는 INNER JOIN\\n④ 둘 다 오류","correctOption":2,"explanation":"WHERE절은 JOIN 후 적용 → B가 NULL인 행도 제거 → INNER JOIN. ON절은 JOIN 시 적용 → LEFT JOIN 유지.","summary":"OUTER JOIN에서 ON절과 WHERE절 위치 차이","difficulty":2}"""
        ));

        EXAMPLES.put("서브쿼리", List.of(
            """
            {"content":"SELECT절에 사용되는 서브쿼리의 명칭은?\\n\\n① 인라인 뷰\\n② 스칼라 서브쿼리\\n③ 상관 서브쿼리\\n④ 중첩 서브쿼리","correctOption":2,"explanation":"스칼라: SELECT절. 인라인 뷰: FROM절. 중첩: WHERE절.","summary":"서브쿼리 위치별 명칭 구분","difficulty":0}""",
            """
            {"content":"다음 SQL의 결과 건수는?\\n\\n[T1] COL1: 1,2,3 [T2] COL1: 1,NULL\\n\\nSELECT * FROM T1 WHERE COL1 NOT IN (SELECT COL1 FROM T2);\\n\\n① 0건\\n② 1건\\n③ 2건\\n④ 3건","correctOption":1,"explanation":"NOT IN에 NULL 포함 → 항상 0건. NOT EXISTS로 대체해야 합니다.","summary":"NOT IN + NULL의 함정 — 항상 0건","difficulty":1}""",
            """
            {"content":"다음 IN을 EXISTS로 변환한 것으로 올바른 것은?\\n\\nSELECT 사번 FROM 직원 A WHERE 부서코드 IN (SELECT 부서코드 FROM 부서 B WHERE B.위치='서울');\\n\\n① WHERE EXISTS (SELECT 1 FROM 부서 B WHERE B.위치='서울')\\n② WHERE EXISTS (SELECT 1 FROM 부서 B WHERE A.부서코드=B.부서코드 AND B.위치='서울')\\n③ WHERE EXISTS (SELECT 부서코드 FROM 부서 B WHERE B.위치='서울')\\n④ WHERE EXISTS (SELECT 1 FROM 부서 B WHERE A.부서코드=B.부서코드)","correctOption":2,"explanation":"IN→EXISTS 변환 시 연결 조건(A.부서코드=B.부서코드)과 원래 조건 모두 포함 필요.","summary":"IN → EXISTS 변환 시 연결 조건 포함","difficulty":2}"""
        ));

        EXAMPLES.put("집합연산자", List.of(
            """
            {"content":"UNION과 UNION ALL의 차이로 올바른 것은?\\n\\n① UNION 중복 제거, UNION ALL 중복 허용\\n② UNION ALL 중복 제거, UNION 중복 허용\\n③ 항상 동일\\n④ UNION ALL이 더 느림","correctOption":1,"explanation":"UNION: 중복 제거(정렬 발생). UNION ALL: 중복 포함(정렬 없음).","summary":"UNION vs UNION ALL 중복 처리","difficulty":0}""",
            """
            {"content":"두 과목을 '동시에' 수강하는 학생을 구하는 올바른 SQL은?\\n\\n① WHERE 과목코드=100 AND 과목코드=101\\n② WHERE 과목코드 IN (100,101)\\n③ SELECT 학번 WHERE 과목코드=100 INTERSECT SELECT 학번 WHERE 과목코드=101\\n④ WHERE 과목코드=100 OR 과목코드=101","correctOption":3,"explanation":"INTERSECT(교집합)로 각각의 결과에서 공통 학번을 찾아야 합니다.","summary":"INTERSECT를 이용한 동시 조건 검색","difficulty":1}""",
            """
            {"content":"다음 SQL의 결과 행수는?\\n\\n[T1] COL1: 1,1,2,3 [T2] COL1: 1,2,2,4\\n\\nSELECT COL1 FROM T1 UNION ALL SELECT COL1 FROM T2;\\n\\n① 4건\\n② 5건\\n③ 6건\\n④ 8건","correctOption":4,"explanation":"UNION ALL은 중복 제거 없이 모든 행 합침. 4+4=8건.","summary":"UNION ALL의 결과 행수 계산","difficulty":2}"""
        ));

        EXAMPLES.put("ROLLUP/CUBE/GROUPING", List.of(
            """
            {"content":"ROLLUP(A, B)가 생성하는 그룹핑 조합은?\\n\\n① (A,B), (A), (B), ()\\n② (A,B), (A), ()\\n③ (A,B), ()\\n④ (A,B), (B), ()","correctOption":2,"explanation":"ROLLUP은 오른쪽부터 제거하며 계층적 소계: (A,B)→(A)→().","summary":"ROLLUP의 그룹핑 생성 규칙","difficulty":0}""",
            """
            {"content":"CUBE(A,B)와 ROLLUP(A,B)의 결과 차이로 올바른 것은?\\n\\n① CUBE가 (B) 그룹핑을 추가로 생성\\n② ROLLUP이 더 많은 그룹핑 생성\\n③ 항상 동일\\n④ CUBE는 ORDER BY 자동 포함","correctOption":1,"explanation":"ROLLUP: 3가지. CUBE: (B) 포함 4가지.","summary":"CUBE vs ROLLUP의 그룹핑 개수 차이","difficulty":1}""",
            """
            {"content":"ROLLUP 결과에서 원본 NULL과 소계 NULL을 구분하는 방법은?\\n\\n① NVL로 구분\\n② DECODE로 구분\\n③ GROUPING(칼럼)=1이면 소계, 0이면 원본\\n④ IS NULL로 구분","correctOption":3,"explanation":"GROUPING(칼럼): ROLLUP/CUBE에 의한 NULL→1, 원본 NULL→0.","summary":"GROUPING 함수로 소계 NULL 구분","difficulty":2}"""
        ));

        EXAMPLES.put("윈도우 함수", List.of(
            """
            {"content":"RANK()와 DENSE_RANK()의 차이로 올바른 것은? (점수: 100,100,90)\\n\\n① RANK: 1,1,3 / DENSE_RANK: 1,1,2\\n② RANK: 1,1,2 / DENSE_RANK: 1,1,3\\n③ RANK: 1,2,3 / DENSE_RANK: 1,1,2\\n④ 항상 동일","correctOption":1,"explanation":"RANK: 동일 순위 후 건너뜀(1,1,3). DENSE_RANK: 연속(1,1,2).","summary":"RANK vs DENSE_RANK 순위 건너뜀 여부","difficulty":0}""",
            """
            {"content":"다음 SQL에서 직원 C의 CUMSUM은?\\n\\nNAME SAL (DEPT=10): A=100, B=200, C=300, D=400\\n\\nSUM(SAL) OVER(ORDER BY SAL ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)\\n\\n① 300\\n② 500\\n③ 600\\n④ 1000","correctOption":3,"explanation":"누적합: A:100, B:300, C:600, D:1000.","summary":"ROWS UNBOUNDED PRECEDING 누적합 계산","difficulty":1}""",
            """
            {"content":"ROWS와 RANGE의 차이로 SAL=1500 직원의 CNT가 다르게 나오는 경우는?\\n(SAL: 1000,1500,1500,2000)\\n\\nSQL1: COUNT(*) OVER(ORDER BY SAL ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING)\\nSQL2: COUNT(*) OVER(ORDER BY SAL RANGE BETWEEN 500 PRECEDING AND 500 FOLLOWING)\\n\\n① SQL1=3, SQL2=4\\n② SQL1=3, SQL2=3\\n③ SQL1=2, SQL2=4\\n④ SQL1=3, SQL2=2","correctOption":1,"explanation":"ROWS: 물리적 행 기준(앞1+본인+뒤1=3). RANGE: 값 기준(1000~2000=4건).","summary":"ROWS(행 기준) vs RANGE(값 기준) 차이","difficulty":2}"""
        ));

        EXAMPLES.put("계층형 질의", List.of(
            """
            {"content":"Oracle 계층형 질의에서 루트 노드를 지정하는 절은?\\n\\n① CONNECT BY\\n② START WITH\\n③ PRIOR\\n④ ORDER SIBLINGS BY","correctOption":2,"explanation":"START WITH: 루트 조건. CONNECT BY: 부모-자식 관계.","summary":"계층형 질의 절 역할 구분","difficulty":0}""",
            """
            {"content":"순방향(부모→자식) 탐색 쿼리는?\\n\\n① CONNECT BY PRIOR 상위사번 = 사번\\n② CONNECT BY PRIOR 사번 = 상위사번\\n③ CONNECT BY 사번 = 상위사번\\n④ CONNECT BY 상위사번 = PRIOR 상위사번","correctOption":2,"explanation":"PRIOR가 PK(사번)에 붙으면 순방향(부모→자식).","summary":"PRIOR 위치에 따른 탐색 방향","difficulty":1}""",
            """
            {"content":"다음 계층형 질의에서 LEVEL=3인 행은 몇 건?\\n\\n[ORG] 1 CEO NULL / 2 VP1 1 / 3 VP2 1 / 4 MGR1 2 / 5 MGR2 2 / 6 STAFF 4\\n\\nSTART WITH MGR_ID IS NULL CONNECT BY PRIOR ID=MGR_ID;\\n\\n① 1건\\n② 2건\\n③ 3건\\n④ 4건","correctOption":2,"explanation":"L1:CEO. L2:VP1,VP2. L3:MGR1,MGR2=2건. L4:STAFF.","summary":"계층 구조에서 LEVEL값 계산","difficulty":2}"""
        ));

        EXAMPLES.put("PIVOT/UNPIVOT", List.of(
            """
            {"content":"PIVOT의 기능으로 올바른 것은?\\n\\n① 행→열 변환\\n② 열→행 변환\\n③ 행과 열 동시 삭제\\n④ 테이블 분할","correctOption":1,"explanation":"PIVOT: 행→열. UNPIVOT: 열→행.","summary":"PIVOT과 UNPIVOT의 변환 방향","difficulty":0}""",
            """
            {"content":"다음 UNPIVOT의 결과 행수는?\\n\\n[TAB] NAME, 국어, 수학, 영어\\n김철수 90 85 70 / 이영희 80 95 60\\n\\nUNPIVOT (점수 FOR 과목 IN (국어, 수학, 영어));\\n\\n① 2건\\n② 3건\\n③ 6건\\n④ 9건","correctOption":3,"explanation":"2명 × 3과목 = 6건.","summary":"UNPIVOT 결과 행수 계산","difficulty":1}""",
            """
            {"content":"PIVOT SQL의 빈칸에 들어갈 것은?\\n\\nSELECT * FROM 성적 PIVOT (( ① ) FOR 과목 IN ('국어' AS 국어, '수학' AS 수학));\\n\\n① SUM(점수)\\n② 점수\\n③ GROUP BY 과목\\n④ PARTITION BY 과목","correctOption":1,"explanation":"PIVOT절에는 집계함수가 필수. GROUP BY는 PIVOT이 내부 처리.","summary":"PIVOT 절에서 집계함수 필수","difficulty":2}"""
        ));

        EXAMPLES.put("정규표현식", List.of(
            """
            {"content":"Oracle 정규표현식 함수가 아닌 것은?\\n\\n① REGEXP_LIKE\\n② REGEXP_SUBSTR\\n③ REGEXP_REPLACE\\n④ REGEXP_JOIN","correctOption":4,"explanation":"REGEXP_LIKE, REGEXP_SUBSTR, REGEXP_REPLACE, REGEXP_INSTR, REGEXP_COUNT. REGEXP_JOIN은 없음.","summary":"Oracle 정규표현식 함수 목록","difficulty":0}""",
            """
            {"content":"REGEXP_LIKE(COL1, '^[A-Z]{2}[0-9]+$')의 의미는?\\n\\n① 대문자 2자로 시작하고 숫자 1개 이상으로 끝남\\n② 대문자 2자 이상과 숫자로 구성\\n③ 대문자와 숫자가 번갈아 나옴\\n④ 영문자 2자와 숫자 0개 이상","correctOption":1,"explanation":"^[A-Z]{2}: 대문자 정확히 2자. [0-9]+: 숫자 1개 이상. $: 끝.","summary":"정규표현식 패턴 해석 — {n}과 +","difficulty":1}""",
            """
            {"content":"?, *, +의 의미가 올바르게 연결된 것은?\\n\\n① ?=0~1회, *=0회 이상, +=1회 이상\\n② ?=1회, *=0회 이상, +=2회 이상\\n③ ?=0~1회, *=1회 이상, +=0회 이상\\n④ ?=0회 이상, *=0~1회, +=1회 이상","correctOption":1,"explanation":"?=0또는1. *=0이상. +=1이상.","summary":"정규표현식 반복 연산자 ?, *, + 의미","difficulty":2}"""
        ));

        EXAMPLES.put("DCL", List.of(
            """
            {"content":"DCL에 해당하는 명령어는?\\n\\n① COMMIT\\n② CREATE\\n③ GRANT\\n④ SELECT","correctOption":3,"explanation":"DCL: GRANT, REVOKE. COMMIT=TCL, CREATE=DDL, SELECT=DML.","summary":"DCL 명령어 구분","difficulty":0}""",
            """
            {"content":"다음 SQL의 의미는?\\n\\nGRANT SELECT, INSERT ON EMP TO USER1 WITH GRANT OPTION;\\n\\n① USER1에게 조회/삽입 권한 부여\\n② 모든 테이블 권한 부여\\n③ USER1도 다른 사용자에게 권한 부여 가능\\n④ USER1 권한 회수","correctOption":3,"explanation":"WITH GRANT OPTION은 권한을 받은 사용자가 다른 사용자에게도 부여 가능.","summary":"WITH GRANT OPTION의 권한 연쇄 부여","difficulty":1}""",
            """
            {"content":"WITH GRANT OPTION 권한 REVOKE 시 특성으로 올바른 것은?\\n\\n① 해당 사용자만 회수, 연쇄 부여 유지\\n② 해당 사용자와 연쇄 부여 모두 회수\\n③ CASCADE 명시해야 연쇄 회수\\n④ REVOKE 불가","correctOption":2,"explanation":"WITH GRANT OPTION 회수 시 연쇄적으로 부여된 모든 권한도 함께 회수(CASCADE).","summary":"WITH GRANT OPTION 회수 시 CASCADE 동작","difficulty":2}"""
        ));

        EXAMPLES.put("옵티마이저/인덱스", List.of(
            """
            {"content":"비용 기반 옵티마이저(CBO)의 특성은?\\n\\n① 규칙에 따라 실행 계획 결정\\n② 통계 정보 기반 최적화\\n③ 인덱스 있으면 항상 사용\\n④ 현재 사용되지 않음","correctOption":2,"explanation":"CBO: 통계 정보(행 수, 값 분포 등) 기반. RBO: 규칙 기반.","summary":"CBO vs RBO 차이","difficulty":0}""",
            """
            {"content":"특정 범위 데이터 검색에 사용하는 인덱스 스캔 방식은?\\n\\n① 유니크 스캔\\n② 범위 스캔\\n③ 전체 스캔\\n④ 역순 범위 스캔","correctOption":2,"explanation":"유니크: 1건 조회. 범위: 특정 범위 검색. 전체: 인덱스 전체 읽기.","summary":"인덱스 스캔 방식별 특성","difficulty":1}""",
            """
            {"content":"인덱스에 대한 설명으로 가장 부적절한 것은?\\n\\n① 조회 성능 향상, DML 성능 저하 가능\\n② B-Tree 리프 노드는 같은 레벨\\n③ CBO에서 인덱스 있으면 항상 사용\\n④ 범위 스캔은 결과 없으면 0건 반환","correctOption":3,"explanation":"CBO는 비용 계산하여 테이블 스캔이 효율적이면 인덱스 사용 안 할 수 있음.","summary":"CBO에서 인덱스 사용 판단 — '항상'이 아님","difficulty":2}"""
        ));

        EXAMPLES.put("제약조건", List.of(
            """
            {"content":"PRIMARY KEY의 특성으로 올바른 것은?\\n\\n① NULL 허용, 중복 불가\\n② NULL 불가, 중복 허용\\n③ NULL 불가, 중복 불가\\n④ NULL 허용, 중복 허용","correctOption":3,"explanation":"PK = NOT NULL + UNIQUE. 테이블당 1개만 가능.","summary":"PK의 NOT NULL + UNIQUE 특성","difficulty":0}""",
            """
            {"content":"UNIQUE KEY에 대한 설명으로 올바르지 않은 것은?\\n\\n① 중복 불허\\n② NULL 허용 (Oracle)\\n③ 여러 개 생성 가능\\n④ 반드시 NOT NULL","correctOption":4,"explanation":"UNIQUE KEY는 NULL 허용. PK와 달리 NOT NULL 필수 아님.","summary":"UNIQUE KEY의 NULL 허용 여부","difficulty":1}""",
            """
            {"content":"다음 실행 후 최종 데이터는?\\n\\nCREATE TABLE T(C1 INT PK, C2 INT REFERENCES P(ID), C3 INT CHECK(C3 BETWEEN 1 AND 100));\\n-- P.ID: 10,20,30\\n\\nINSERT INTO T VALUES(1, 10, 50);\\nINSERT INTO T VALUES(2, 40, 70);\\nINSERT INTO T VALUES(3, 20, 150);\\nINSERT INTO T VALUES(1, 30, 80);\\n\\n① 1건\\n② 2건\\n③ 4건\\n④ 1건과 오류","correctOption":1,"explanation":"①성공. ②FK위반(40없음). ③CHECK위반(150>100). ④PK중복. 1건만 성공.","summary":"PK/FK/CHECK 복합 제약조건 위반 판별","difficulty":2}"""
        ));
    }

    private TopicExamples() {
    }
}
