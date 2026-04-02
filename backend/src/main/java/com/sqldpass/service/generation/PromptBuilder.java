package com.sqldpass.service.generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptBuilder {

    static final String GENERATION_SYSTEM_PROMPT = """
            당신은 SQLD(SQL 개발자) 자격증 시험 출제위원입니다.
            주어진 토픽에 대해 4지선다 객관식 문제를 생성하세요.

            규칙:
            - 실제 SQLD 시험 난이도와 형식을 따를 것
            - 정답은 반드시 1개만 존재
            - "가장 적절한 것" 또는 "가장 적절하지 않은 것" 형식 사용
            - 선택지는 ①②③④ 기호 사용
            - 선택지는 실수하기 쉬운 함정을 포함할 것
            - 해설은 정답인 이유 + 각 오답인 이유를 모두 설명할 것
            - SQL 관련 문제는 실행 결과를 묻는 형태를 포함할 것
            - summary는 이 문제가 어떤 관점에서 출제되었는지 200자 이내로 요약
            - difficulty는 0(초급), 1(중급), 2(상급) 중 하나

            반드시 아래 JSON 형식으로만 응답하세요:
            {"content": "문제 본문\\n\\n① 선택지1\\n② 선택지2\\n③ 선택지3\\n④ 선택지4", "correctOption": 1, "explanation": "해설", "summary": "출제 관점 요약", "difficulty": 1}
            """;

    static final String VERIFICATION_SYSTEM_PROMPT = """
            당신은 SQLD 시험 문제 검수 전문가입니다.
            주어진 문제의 품질을 평가하세요.

            평가 항목:
            1. 정답이 실제로 맞는지
            2. 오답 선택지가 명확히 틀린지
            3. 문제 표현이 명확한지
            4. 난이도가 SQLD 시험에 적합한지

            반드시 아래 JSON 형식으로만 응답하세요:
            {"approved": true, "reason": "승인/거절 사유"}
            """;

    static final String FIX_SYSTEM_PROMPT = """
            당신은 SQLD 시험 문제 수정 전문가입니다.
            검증에서 지적된 사항을 반영하여 문제를 수정하세요.
            수정이 불가능하면 {"fixable": false}를 반환하세요.

            반드시 아래 JSON 형식으로만 응답하세요:
            {"content": "...", "correctOption": 1, "explanation": "...", "summary": "...", "difficulty": 1}
            """;

    // 주제(subject name) → 토픽 목록
    static final Map<String, List<String>> SUBJECT_TOPICS = new LinkedHashMap<>() {{
        put("데이터 모델링의 이해", List.of("데이터 모델링 개요", "엔터티", "속성", "관계", "식별자"));
        put("데이터 모델과 SQL", List.of("정규화", "성능 데이터 모델링"));
        put("SQL 기본", List.of("DDL", "DML", "TCL", "WHERE절/조건", "함수", "NULL 처리 함수", "GROUP BY/HAVING", "ORDER BY", "제약조건"));
        put("SQL 활용", List.of("JOIN", "서브쿼리", "집합연산자", "ROLLUP/CUBE/GROUPING", "윈도우 함수", "계층형 질의", "PIVOT/UNPIVOT", "정규표현식"));
        put("관리 구문", List.of("DCL", "옵티마이저/인덱스"));
    }};

    // 토픽별 Few-shot 예시 (HTML에서 중급 문제 1개씩 추출)
    private static final Map<String, String> TOPIC_EXAMPLES = new LinkedHashMap<>() {{
        put("데이터 모델링 개요", """
                {"content": "3층 스키마(Three-Level Schema)에서 '모든 사용자 관점을 통합한 조직 전체 관점의 데이터 구조'에 해당하는 것은?\\n\\n① 외부 스키마\\n② 개념 스키마\\n③ 내부 스키마\\n④ 뷰 스키마", "correctOption": 2, "explanation": "외부 스키마: 개별 사용자 관점(View). 개념 스키마: 조직 전체 통합 관점. 내부 스키마: 물리적 저장 구조. 3층 스키마의 목적은 데이터 독립성 확보입니다.", "summary": "3층 스키마에서 개념 스키마의 역할", "difficulty": 1}""");
        put("엔터티", """
                {"content": "엔터티의 특성으로 가장 부적절한 것은?\\n\\n① 업무에서 필요로 하는 정보여야 한다\\n② 유일한 식별자에 의해 식별 가능해야 한다\\n③ 두 개 이상의 인스턴스가 있어야 한다\\n④ 속성 없이도 엔터티가 될 수 있다", "correctOption": 4, "explanation": "엔터티는 반드시 속성을 가져야 합니다. 속성이 없는 엔터티는 존재할 수 없습니다.", "summary": "엔터티의 필수 조건 — 속성 보유 여부", "difficulty": 1}""");
        put("속성", """
                {"content": "속성의 특성으로 가장 부적절한 것은?\\n\\n① 하나의 속성은 하나의 값만 가진다\\n② 하나의 엔터티는 두 개 이상의 속성을 갖는다\\n③ 속성은 주식별자에 함수적으로 종속되어야 한다\\n④ 파생 속성이 많을수록 데이터 정합성이 향상된다", "correctOption": 4, "explanation": "파생 속성이 많으면 데이터 정합성이 오히려 저하됩니다. 원본 데이터가 바뀔 때 파생 속성도 동기화해야 하므로 불일치 위험이 커집니다.", "summary": "파생 속성과 데이터 정합성의 관계", "difficulty": 1}""");
        put("관계", """
                {"content": "두 엔터티 간 M:N 관계를 해소하는 방법으로 올바른 것은?\\n\\n① 외래키를 양쪽에 추가한다\\n② 관계 자체를 삭제한다\\n③ 연결(교차) 엔터티를 추가하여 1:M 관계 2개로 분리한다\\n④ M:N 관계는 물리 모델에서 그대로 구현 가능하다", "correctOption": 3, "explanation": "M:N 관계는 관계형 DB에서 직접 구현할 수 없으므로 연결(교차) 엔터티를 생성하여 1:M 관계 2개로 분리합니다.", "summary": "M:N 관계 해소 방법 — 교차 엔터티", "difficulty": 1}""");
        put("식별자", """
                {"content": "인조 식별자에 대한 설명으로 올바른 것은?\\n\\n① 업무적으로 자연 발생하는 식별자이다\\n② 본래 식별자가 복잡할 때 인위적으로 만든 식별자이다\\n③ 외래키를 식별자로 사용하는 것이다\\n④ 대체 식별자와 동일한 개념이다", "correctOption": 2, "explanation": "인조 식별자는 본질 식별자가 복잡한 복합키일 때 인위적으로 만든 일련번호 등의 식별자입니다.", "summary": "인조 식별자의 정의와 사용 이유", "difficulty": 1}""");
        put("정규화", """
                {"content": "다음 테이블이 위반하는 정규형은?\\n\\n[주문] 주문번호(PK), 고객번호, 고객명, 주문일자\\n※ 고객명은 고객번호에 의해 결정됨\\n\\n① 제1정규형\\n② 제2정규형\\n③ 제3정규형\\n④ BCNF", "correctOption": 3, "explanation": "PK가 주문번호 하나이므로 부분 함수 종속은 없어 2NF는 만족. 하지만 주문번호→고객번호→고객명으로 이행적 함수 종속이 존재 → 제3정규형 위반.", "summary": "이행적 함수 종속과 제3정규형 위반 판별", "difficulty": 1}""");
        put("성능 데이터 모델링", """
                {"content": "분산 데이터베이스의 투명성 중 '데이터가 어디에 저장되어 있는지 명시하지 않아도 되는 것'은?\\n\\n① 분할 투명성\\n② 위치 투명성\\n③ 중복 투명성\\n④ 병행 투명성", "correctOption": 2, "explanation": "위치 투명성: 저장 장소 몰라도 됨. 분할 투명성: 분할 사실 몰라도 됨. 중복 투명성: 중복 여부 몰라도 됨.", "summary": "분산 DB 투명성 유형 구분 — 위치 투명성", "difficulty": 1}""");
        put("DDL", """
                {"content": "CTAS(CREATE TABLE ... AS SELECT)에 대한 설명으로 가장 부적절한 것은?\\n\\n① 원본 테이블의 데이터를 복사하여 새 테이블을 생성한다\\n② NOT NULL 제약조건은 복사된다\\n③ PRIMARY KEY 제약조건도 함께 복사된다\\n④ 칼럼의 데이터 타입은 동일하게 복사된다", "correctOption": 3, "explanation": "CTAS는 NOT NULL 제약조건만 복사되고, PK, FK, INDEX, TRIGGER 등은 복사되지 않습니다.", "summary": "CTAS에서 제약조건 복사 범위", "difficulty": 1}""");
        put("DML", """
                {"content": "다음 MERGE문에 대한 설명으로 올바르지 않은 것은?\\n\\n① MATCHED 조건일 때 UPDATE를 수행한다\\n② NOT MATCHED 조건일 때 INSERT를 수행한다\\n③ 하나의 SQL로 INSERT와 UPDATE를 동시에 처리할 수 있다\\n④ MERGE문에서 DELETE는 사용할 수 없다", "correctOption": 4, "explanation": "Oracle에서 MERGE문의 WHEN MATCHED 절 안에서 DELETE도 사용 가능합니다.", "summary": "MERGE문에서 DELETE 사용 가능 여부", "difficulty": 1}""");
        put("TCL", """
                {"content": "다음 실행 후 테이블에 남는 데이터 건수는?\\n\\nINSERT INTO T VALUES(1);\\nINSERT INTO T VALUES(2);\\nCOMMIT;\\nINSERT INTO T VALUES(3);\\nSAVEPOINT SP1;\\nINSERT INTO T VALUES(4);\\nROLLBACK TO SP1;\\nCOMMIT;\\n\\n① 2건\\n② 3건\\n③ 4건\\n④ 1건", "correctOption": 2, "explanation": "1,2 COMMIT 확정. 3 삽입 → SP1 → 4 삽입 → ROLLBACK TO SP1(4 취소) → COMMIT. 최종: 1,2,3 = 3건.", "summary": "SAVEPOINT와 ROLLBACK TO의 실행 결과", "difficulty": 1}""");
        put("WHERE절/조건", """
                {"content": "다음 SQL의 결과는?\\n\\nSELECT * FROM TAB\\nWHERE COL1 IN ('A','B', NULL);\\n\\n① COL1이 A, B, NULL인 행 모두 반환\\n② COL1이 A, B인 행만 반환\\n③ 오류 발생\\n④ 전체 행 반환", "correctOption": 2, "explanation": "IN 리스트에 NULL이 포함되어도 NULL 행은 반환되지 않습니다. IN은 = 비교의 OR 연결인데, COL1=NULL은 항상 UNKNOWN → FALSE.", "summary": "IN 절에서 NULL 포함 시 동작", "difficulty": 1}""");
        put("함수", """
                {"content": "다음 SQL의 결과는? (Oracle)\\n\\nSELECT LENGTH('SQL 개발'), SUBSTR('SQL 개발', 2, 3) FROM DUAL;\\n\\n① 6, QL \\n② 5, QL 개\\n③ 6, QL 개\\n④ 7, QL ", "correctOption": 2, "explanation": "LENGTH는 문자 수(공백 포함) = 5. SUBSTR은 2번째부터 3글자 = 'QL '이 아니라 'QL 개'.", "summary": "LENGTH와 SUBSTR 함수의 한글 처리", "difficulty": 1}""");
        put("NULL 처리 함수", """
                {"content": "다음 SQL의 결과는?\\n\\nSELECT COALESCE(NULL, NULL, 3, NULL, 5) FROM DUAL;\\n\\n① NULL\\n② 3\\n③ 5\\n④ 오류", "correctOption": 2, "explanation": "COALESCE는 인자를 순서대로 평가하여 첫 번째 NULL이 아닌 값을 반환합니다. 3이 첫 번째 non-null 값.", "summary": "COALESCE 함수의 동작 — 첫 번째 non-null 반환", "difficulty": 1}""");
        put("GROUP BY/HAVING", """
                {"content": "다음 SQL에서 오류가 발생하는 것은?\\n\\n① SELECT DEPTNO, COUNT(*) FROM EMP GROUP BY DEPTNO\\n② SELECT DEPTNO, SAL FROM EMP GROUP BY DEPTNO\\n③ SELECT COUNT(*) FROM EMP\\n④ SELECT DEPTNO, MAX(SAL) FROM EMP GROUP BY DEPTNO", "correctOption": 2, "explanation": "GROUP BY 사용 시 SELECT에는 GROUP BY 컬럼이나 집계함수만 올 수 있습니다. SAL은 GROUP BY에 없으므로 오류.", "summary": "GROUP BY와 SELECT 절의 컬럼 제약", "difficulty": 1}""");
        put("ORDER BY", """
                {"content": "다음 중 ORDER BY에 대한 설명으로 올바르지 않은 것은?\\n\\n① SELECT 절에 없는 컬럼으로도 정렬 가능하다\\n② NULL은 Oracle에서 가장 큰 값으로 취급된다\\n③ 별칭(Alias)으로 정렬할 수 있다\\n④ GROUP BY를 사용하면 ORDER BY에 집계함수를 사용할 수 없다", "correctOption": 4, "explanation": "GROUP BY와 함께 ORDER BY에 집계함수 사용 가능합니다 (예: ORDER BY COUNT(*) DESC).", "summary": "ORDER BY에서 집계함수 사용 가능 여부", "difficulty": 1}""");
        put("JOIN", """
                {"content": "다음 중 NATURAL JOIN에 대한 설명으로 올바르지 않은 것은?\\n\\n① 같은 이름의 컬럼을 자동으로 조인한다\\n② ON 절을 사용할 수 없다\\n③ 조인 컬럼에 테이블 별칭을 사용할 수 있다\\n④ 같은 이름의 컬럼이 여러 개면 모두 조인 조건에 사용된다", "correctOption": 3, "explanation": "NATURAL JOIN에서는 조인 컬럼에 테이블 별칭(접두사)을 사용할 수 없습니다. A.COL1 형태 불가.", "summary": "NATURAL JOIN의 제약사항 — 별칭 사용 불가", "difficulty": 1}""");
        put("서브쿼리", """
                {"content": "다음 중 상관(Correlated) 서브쿼리에 대한 설명으로 올바른 것은?\\n\\n① 메인쿼리와 독립적으로 실행된다\\n② 서브쿼리가 먼저 실행된 후 메인쿼리에 결과를 전달한다\\n③ 메인쿼리의 각 행마다 서브쿼리가 반복 실행된다\\n④ IN 절에서만 사용할 수 있다", "correctOption": 3, "explanation": "상관 서브쿼리는 메인쿼리의 값을 참조하므로 메인쿼리의 각 행마다 반복 실행됩니다. 비상관 서브쿼리가 독립 실행.", "summary": "상관 서브쿼리와 비상관 서브쿼리의 실행 차이", "difficulty": 1}""");
        put("집합연산자", """
                {"content": "UNION과 UNION ALL의 차이로 올바른 것은?\\n\\n① UNION ALL은 중복을 제거한다\\n② UNION은 정렬을 수행하지 않는다\\n③ UNION은 중복을 제거하고, UNION ALL은 중복을 포함한다\\n④ 두 연산자의 결과는 항상 동일하다", "correctOption": 3, "explanation": "UNION은 중복 제거(내부적으로 정렬 발생), UNION ALL은 중복 포함(정렬 없음, 성능 우수).", "summary": "UNION vs UNION ALL의 중복 처리 차이", "difficulty": 1}""");
        put("ROLLUP/CUBE/GROUPING", """
                {"content": "ROLLUP(A, B)의 결과에 포함되지 않는 그룹은?\\n\\n① GROUP BY A, B\\n② GROUP BY A\\n③ GROUP BY B\\n④ 전체 합계", "correctOption": 3, "explanation": "ROLLUP(A,B)는 (A,B), (A), () 3가지 그룹을 생성합니다. GROUP BY B 단독은 포함되지 않습니다. CUBE(A,B)라면 (B)도 포함.", "summary": "ROLLUP의 그룹 생성 규칙", "difficulty": 1}""");
        put("윈도우 함수", """
                {"content": "RANK()와 DENSE_RANK()의 차이로 올바른 것은?\\n\\n① RANK()는 동일 순위 후 다음 순위를 건너뛴다\\n② DENSE_RANK()는 동일 순위 후 다음 순위를 건너뛴다\\n③ 두 함수의 결과는 항상 동일하다\\n④ RANK()는 NULL을 무시한다", "correctOption": 1, "explanation": "RANK(): 1,2,2,4 (건너뜀). DENSE_RANK(): 1,2,2,3 (건너뛰지 않음). ROW_NUMBER(): 1,2,3,4 (고유).", "summary": "RANK vs DENSE_RANK 순위 부여 차이", "difficulty": 1}""");
        put("계층형 질의", """
                {"content": "Oracle 계층형 질의에서 START WITH절의 역할은?\\n\\n① 정렬 기준을 지정한다\\n② 계층의 루트(시작점)를 지정한다\\n③ 연결 조건을 지정한다\\n④ 필터 조건을 지정한다", "correctOption": 2, "explanation": "START WITH: 루트 행 지정. CONNECT BY: 부모-자식 연결 조건. PRIOR: 방향(상→하 or 하→상) 지정.", "summary": "계층형 질의 START WITH의 역할", "difficulty": 1}""");
        put("PIVOT/UNPIVOT", """
                {"content": "PIVOT의 역할로 올바른 것은?\\n\\n① 행을 열로 변환한다\\n② 열을 행으로 변환한다\\n③ 데이터를 정렬한다\\n④ 중복을 제거한다", "correctOption": 1, "explanation": "PIVOT: 행→열 변환. UNPIVOT: 열→행 변환. PIVOT은 집계함수와 함께 사용하여 크로스탭 결과를 만듭니다.", "summary": "PIVOT과 UNPIVOT의 변환 방향", "difficulty": 1}""");
        put("정규표현식", """
                {"content": "REGEXP_LIKE(COL, '^[A-Z]{3}[0-9]{2}$')에 매칭되는 값은?\\n\\n① ABC12\\n② AB123\\n③ abc12\\n④ ABCD1", "correctOption": 1, "explanation": "^[A-Z]{3}: 대문자 정확히 3자로 시작. [0-9]{2}$: 숫자 정확히 2자로 끝남. ABC12만 해당.", "summary": "정규표현식 패턴 매칭 — 문자+숫자 조합", "difficulty": 1}""");
        put("DCL", """
                {"content": "GRANT와 REVOKE에 대한 설명으로 올바르지 않은 것은?\\n\\n① GRANT는 권한을 부여한다\\n② REVOKE는 권한을 회수한다\\n③ WITH GRANT OPTION으로 받은 권한은 다른 사용자에게 부여할 수 있다\\n④ WITH GRANT OPTION으로 부여된 권한을 회수하면 재부여된 권한은 유지된다", "correctOption": 4, "explanation": "WITH GRANT OPTION으로 부여된 권한을 REVOKE하면 해당 사용자가 다른 사용자에게 재부여한 권한도 함께 회수(CASCADE)됩니다.", "summary": "WITH GRANT OPTION 회수 시 CASCADE 동작", "difficulty": 1}""");
        put("옵티마이저/인덱스", """
                {"content": "다음 중 인덱스를 사용할 수 없는 경우는?\\n\\n① WHERE COL1 = '값'\\n② WHERE COL1 LIKE '값%'\\n③ WHERE SUBSTR(COL1, 1, 3) = '값'\\n④ WHERE COL1 BETWEEN 1 AND 100", "correctOption": 3, "explanation": "인덱스 컬럼에 함수를 적용하면(가공하면) 인덱스를 사용할 수 없습니다. Function-Based Index를 별도 생성해야 합니다.", "summary": "인덱스 사용 불가 조건 — 컬럼 가공(함수 적용)", "difficulty": 1}""");
        put("제약조건", """
                {"content": "FOREIGN KEY 제약조건의 참조 무결성 옵션 중 부모 삭제 시 자식도 함께 삭제되는 것은?\\n\\n① SET NULL\\n② SET DEFAULT\\n③ CASCADE\\n④ RESTRICT", "correctOption": 3, "explanation": "CASCADE: 부모 삭제 시 자식도 삭제. SET NULL: 자식 FK를 NULL로. RESTRICT/NO ACTION: 자식 있으면 삭제 불가.", "summary": "FK 참조 무결성 옵션 — CASCADE 동작", "difficulty": 1}""");
    }};

    private PromptBuilder() {
    }

    static List<String> getTopicsForSubject(String subjectName) {
        return SUBJECT_TOPICS.getOrDefault(subjectName, List.of());
    }

    static String buildGenerationPrompt(AiGenerationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("과목: ").append(request.subjectName()).append("\n");
        sb.append("토픽: ").append(request.topicName()).append("\n\n");

        // Few-shot 예시
        String example = TOPIC_EXAMPLES.get(request.topicName());
        if (example != null) {
            sb.append("예시 문제 (이 퀄리티와 형식으로 만들어주세요):\n");
            sb.append(example).append("\n\n");
        }

        if (!request.existingSummaries().isEmpty()) {
            sb.append("이미 출제된 관점 (아래와 동일한 관점은 피해주세요):\n");
            sb.append(request.existingSummaries().stream()
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        sb.append("1문제 생성하세요.");
        return sb.toString();
    }

    static String buildVerificationPrompt(AiVerificationRequest request) {
        GeneratedQuestion q = request.question();
        return "과목: " + request.subjectName() + "\n\n" +
                "문제:\n" + q.content() + "\n\n" +
                "정답: " + q.correctOption() + "번\n\n" +
                "해설:\n" + q.explanation();
    }

    static String buildFixPrompt(GeneratedQuestion question, String reason) {
        return "검증 사유: " + reason + "\n\n" +
                "원본 문제:\n" +
                "{\"content\": \"" + question.content().replace("\"", "\\\"") + "\", " +
                "\"correctOption\": " + question.correctOption() + ", " +
                "\"explanation\": \"" + question.explanation().replace("\"", "\\\"") + "\", " +
                "\"summary\": \"" + question.summary() + "\", " +
                "\"difficulty\": " + question.difficulty() + "}\n\n" +
                "위 사유를 반영하여 수정한 문제를 JSON으로 반환하세요.";
    }
}
