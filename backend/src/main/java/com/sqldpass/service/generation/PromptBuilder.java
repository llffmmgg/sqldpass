package com.sqldpass.service.generation;

import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.question.QuestionType;
import com.sqldpass.service.generation.ComputerLiteracyTopicExamples.CL1Example;
import com.sqldpass.service.generation.EngineerTopicExamples.EngineerExample;
import com.sqldpass.service.generation.dto.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
// EngineerExample은 List 형태로만 사용

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
            - difficulty는 0(기본), 1(심화), 2(고난도) 중 하나

            기본(0), 심화(1), 고난도(2) 각 1문제씩, 총 3문제를 생성하세요.

            출력 전 반드시 자체 검증하세요:
            1. 정답 번호가 실제로 맞는지 확인
            2. 오답 선택지가 명확히 틀린지 확인
            3. 해설이 정답+오답 이유를 모두 설명하는지 확인
            4. SQL 실행 결과 문제의 경우 직접 실행해보고 결과가 맞는지 확인

            반드시 아래 JSON 배열 형식으로만 응답하세요:
            {"questions": [
              {"content": "...", "correctOption": 1, "explanation": "...", "summary": "...", "difficulty": 0},
              {"content": "...", "correctOption": 2, "explanation": "...", "summary": "...", "difficulty": 1},
              {"content": "...", "correctOption": 3, "explanation": "...", "summary": "...", "difficulty": 2}
            ]}
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

    static final String ENGINEER_GENERATION_SYSTEM_PROMPT = """
            당신은 정보처리기사 실기 시험 출제위원입니다.
            아래 시드 예시들은 "주제·패턴·식별자 회피용 참고자료"입니다.
            절대 그 코드/식별자/구조를 복제해서는 안 됩니다.
            매번 새로운 개념·새로운 함정·새로운 변수명으로 출제하세요.

            **난이도 규칙 (매우 중요):**
            - 시드의 difficulty 값은 무시하세요.
            - 각 문제마다 사용자 측에서 별도로 지정한 **목표 난이도(targetDifficulty)** 를 따르세요.
            - 1 = 쉬움 (기초 개념·간단한 코드 한두 줄·표준 함수 사용 결과)
            - 2 = 보통 (실무 수준 난이도·약간의 응용·여러 단계 추론)
            - 3 = 어려움 (고난도·미묘한 함정·복잡한 흐름 추적·잘 알려지지 않은 케이스)
            - 4 = 매우 어려움 (실무 전문가도 헷갈릴 정도·여러 개념 결합·예외적 케이스·깊은 추론 필요)
            - 같은 시드 주제라도 목표 난이도에 따라 코드 길이/복잡도/요구 추론량을 조정하세요.

            출제 형식 규칙:
            - 실제 정처기 실기 시험과 동일한 형식: 단답형(SHORT_ANSWER) 또는 약술형(DESCRIPTIVE)
            - 객관식(MCQ)은 절대 만들지 말 것
            - 시드별로 매핑된 questionType을 그대로 따를 것
            - 코드형(C/Java/Python/SQL) 문제는 코드의 실행 결과를 묻는 단답형으로 작성
            - 이론형 문제는 핵심 용어 단답 또는 개념 약술
            - answer 필드: 정답(모범답안)
            - keywords 필드: 단답형이면 허용 alias 리스트, 약술형이면 채점 키워드 리스트
            - explanation: 풀이 과정/근거를 자세히
            - summary: 200자 이내, 출제 관점 요약
            - difficulty 필드: 사용자가 지정한 목표 난이도(1/2/3)를 그대로 반환

            절대 금지:
            - 시드의 함수명/클래스명/변수명을 그대로 사용 (변형해도 인식 가능하면 안 됨)
            - 시드의 코드 구조를 그대로 따라하기 (재귀를 재귀로, Shape→Circle 같은 단순 치환)
            - 시드와 동일한 주제·동일한 함정 (다른 개념·다른 함정으로 작성)
            - 회피 식별자/회피 정답 목록에 있는 항목과의 중복
            - 목표 난이도가 1(쉬움)인데 어려운 문제 작성, 또는 그 반대

            출력 전 자체 검증:
            1. 코드형이면 직접 실행 결과가 answer와 맞는지 확인
            2. answer가 명확하고 keywords가 채점에 충분한지 확인
            3. 시드 식별자 / 회피 식별자 / 회피 정답과 겹치지 않는지 확인
            4. 각 문제의 난이도가 지정된 목표 난이도와 일치하는지 확인
            5. 다양한 출제 의도(실행 결과 예측 / 빈칸 채우기 / 약술 / 잘못 짚기) 중 시드와 다른 각도 선택

            반드시 아래 JSON 형식으로만 응답하세요. questions 배열의 길이는 입력된 시드 개수와 정확히 일치해야 합니다:
            {"questions": [
              {"content": "...", "questionType": "SHORT_ANSWER", "answerText": "...", "keywords": ["..."], "explanation": "...", "summary": "...", "difficulty": 2}
            ]}
            """;

    static final String SQLD_SEED_GENERATION_SYSTEM_PROMPT = """
            당신은 SQLD(SQL 개발자) 자격증 시험 출제위원입니다.
            아래 시드 예시들은 "주제·패턴·함정 회피용 참고자료"입니다.
            절대 그 보기/SQL/식별자를 복제해서는 안 됩니다.
            매번 새로운 개념·새로운 함정·새로운 보기로 출제하세요.

            **난이도 규칙 (매우 중요):**
            - 시드의 difficulty 값은 무시하세요.
            - 각 문제마다 사용자 측에서 별도로 지정한 **목표 난이도(targetDifficulty)** 를 따르세요.
            - 1 = 쉬움 (교과서 기본 정의·표준 SQL·직관적 정답)
            - 2 = 보통 (실무 수준·약간의 응용·여러 보기 비교 필요)
            - 3 = 어려움 (미묘한 함정·예외 케이스·자주 헷갈리는 개념)
            - 4 = 매우 어려움 (실무 전문가도 헷갈릴 정도·복합 개념·예외 결합)

            출제 형식 규칙:
            - 4지선다 객관식만 (questionType=MCQ)
            - 단답형 절대 금지
            - 본문 안에 ①②③④ 마커로 보기 4개 모두 포함
            - 정답은 정확히 1개 (correctOption: 1~4)
            - "가장 적절한 것" / "가장 적절하지 않은 것" 형식 사용
            - SQL 관련 문제는 실행 결과를 묻는 형태도 적극 활용 (테이블·코드 블록 사용)
            - SQL 코드 블록은 ```sql 로 감싸기
            - explanation: 정답인 이유 + 오답 보기 이유를 모두 설명
            - summary: 200자 이내, 출제 관점 요약
            - difficulty 필드: 사용자가 지정한 목표 난이도(1~4) 그대로 반환

            절대 금지:
            - 시드의 SQL/테이블명/컬럼명을 그대로 사용
            - 시드와 동일한 함정 (다른 개념·다른 함정으로 작성)
            - 회피 정답/요약 목록과의 중복
            - 목표 난이도가 1(쉬움)인데 어려운 함정을 넣거나, 그 반대

            출력 전 자체 검증:
            1. 정답 번호가 실제로 맞는지 확인 (SQL 결과 직접 머릿속 실행)
            2. 오답 보기가 명확히 틀린지 확인
            3. 해설이 정답+오답 이유를 모두 설명하는지 확인
            4. 각 문제의 난이도가 지정된 목표 난이도와 일치하는지 확인
            5. 시드와 다른 출제 각도(개념 정의 / SQL 결과 / 테이블 설계 / 잘못된 설명 찾기)

            반드시 아래 JSON 형식으로만 응답하세요. questions 배열의 길이는 입력된 시드 개수와 정확히 일치해야 합니다:
            {"questions": [
              {"content": "다음 중 ...?\\n\\n① ...\\n② ...\\n③ ...\\n④ ...", "questionType": "MCQ", "correctOption": 2, "explanation": "...", "summary": "...", "difficulty": 2}
            ]}
            """;

    static final String COMPUTER_LITERACY_GENERATION_SYSTEM_PROMPT = """
            당신은 컴퓨터활용능력 1급 필기 시험 출제위원입니다.
            아래 시드 예시들은 "주제·패턴·함정 회피용 참고자료"입니다.
            절대 그 보기/설명/식별자를 복제해서는 안 됩니다.
            매번 새로운 개념·새로운 함정·새로운 보기로 출제하세요.

            **난이도 규칙 (매우 중요):**
            - 시드의 difficulty 값은 무시하세요.
            - 각 문제마다 사용자 측에서 별도로 지정한 **목표 난이도(targetDifficulty)** 를 따르세요.
            - 1 = 쉬움 (교과서 기본 정의·표준 함수 사용·직관적 정답)
            - 2 = 보통 (실무 수준·약간의 응용·여러 보기 비교 필요)
            - 3 = 어려움 (미묘한 함정·예외 케이스·자주 헷갈리는 개념)
            - 4 = 매우 어려움 (실무 전문가도 헷갈릴 정도·복합 개념·예외 결합)
            - 같은 시드 주제라도 목표 난이도에 따라 보기의 난이도와 함정 깊이를 조정하세요.

            출제 형식 규칙:
            - 4지선다 객관식만 (questionType=MCQ)
            - 단답형/약술형 절대 금지
            - 보기는 ①②③④ 마커를 사용 (콘텐츠 본문 안에 4개 보기가 모두 포함되어야 함)
            - 정답은 정확히 1개만 존재 (correctOption 필드: 1~4)
            - "가장 옳은 것" / "가장 옳지 않은 것" / "가장 적절한 것" 등의 표현 사용
            - 각 보기는 명확하게 구분되며 함정이 분명해야 함
            - explanation: 정답인 이유 + 오답인 이유를 모두 설명
            - summary: 200자 이내, 출제 관점 요약
            - difficulty 필드: 사용자가 지정한 목표 난이도(1~4)를 그대로 반환

            절대 금지:
            - 시드의 보기 텍스트를 그대로 사용 (변형해도 인식 가능하면 안 됨)
            - 시드와 동일한 함정 (다른 개념·다른 함정으로 작성)
            - 회피 정답/설명 목록에 있는 항목과의 중복
            - 목표 난이도가 1(쉬움)인데 어려운 함정을 넣거나, 그 반대

            출력 전 자체 검증:
            1. 정답 번호가 실제로 맞는지 확인
            2. 오답 보기가 명확히 틀린지 확인
            3. 해설이 정답 + 오답 이유를 모두 설명하는지 확인
            4. 각 문제의 난이도가 지정된 목표 난이도와 일치하는지 확인
            5. 시드와 다른 출제 각도(개념 정의 / 사례 비교 / 코드/수식 결과 / 잘못된 설명 찾기)

            반드시 아래 JSON 형식으로만 응답하세요. questions 배열의 길이는 입력된 시드 개수와 정확히 일치해야 합니다:
            {"questions": [
              {"content": "다음 중 ...?\\n\\n① ...\\n② ...\\n③ ...\\n④ ...", "questionType": "MCQ", "correctOption": 2, "explanation": "...", "summary": "...", "difficulty": 2}
            ]}
            """;

    static final String FIX_SYSTEM_PROMPT = """
            당신은 SQLD 시험 문제 수정 전문가입니다.
            검증에서 지적된 사항을 반영하여 문제를 수정하세요.
            수정이 불가능하면 {"fixable": false}를 반환하세요.

            반드시 아래 JSON 형식으로만 응답하세요:
            {"content": "...", "correctOption": 1, "explanation": "...", "summary": "...", "difficulty": 1}
            """;

    /** MCQ 자동 수정 (SQLD/컴활 공용) — 보기 4개 + correctOption 1~4 + explanation. */
    static final String MCQ_FIX_SYSTEM_PROMPT = """
            당신은 4지선다 객관식 시험 문제 수정 전문가입니다.
            검증에서 지적된 사항을 반영하여 문제 본문, 보기, 정답, 해설을 수정하세요.
            본문(content)에는 ①②③④ 4개 보기가 모두 포함되어야 하고,
            correctOption은 1~4 중 하나여야 합니다.
            해설(explanation)은 왜 그 보기가 정답이고 다른 보기가 틀렸는지 명확히 설명해야 합니다.
            수정이 불가능(예: 정답 자체가 보기에 없음, 본문이 깨져 복구 불가)하면 {"fixable": false}만 반환하세요.

            반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 금지):
            {"content": "...", "correctOption": 1, "explanation": "...", "summary": "...", "difficulty": 1}
            """;

    /** 정처기 단답형 자동 수정 — answer + keywords(alias 배열). */
    static final String SHORT_ANSWER_FIX_SYSTEM_PROMPT = """
            당신은 정보처리기사 실기 단답형 문제 수정 전문가입니다.
            검증에서 지적된 사항을 반영해 문제 본문, 모범 답안(answerText), 채점 alias(keywords)를 수정하세요.
            keywords는 답안과 동등하게 인정 가능한 표현들의 배열입니다 (예: ["OTU","O T U","otu"]).
            모범 답안이 alias 중 하나에 포함되어야 하며, 해설은 왜 그 답이 맞는지 단계별로 설명해야 합니다.
            수정이 불가능하면 {"fixable": false}만 반환하세요.

            반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 금지):
            {"content": "...", "questionType": "SHORT_ANSWER", "answerText": "...", "keywords": ["..."], "explanation": "...", "summary": "...", "difficulty": 1}
            """;

    /** 정처기 약술형 자동 수정 — answer(모범) + keywords(채점 키워드). */
    static final String DESCRIPTIVE_FIX_SYSTEM_PROMPT = """
            당신은 정보처리기사 실기 약술형 문제 수정 전문가입니다.
            검증에서 지적된 사항을 반영해 문제 본문, 모범 답안(answerText), 채점 키워드(keywords)를 수정하세요.
            keywords는 답안에 반드시 포함되어야 할 핵심 용어들의 배열입니다 — 부분 점수 채점에 사용됩니다.
            모범 답안에는 keywords가 모두 자연스럽게 포함되어 있어야 합니다.
            수정이 불가능하면 {"fixable": false}만 반환하세요.

            반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 금지):
            {"content": "...", "questionType": "DESCRIPTIVE", "answerText": "...", "keywords": ["..."], "explanation": "...", "summary": "...", "difficulty": 1}
            """;

    // 주제(subject name) → 토픽 목록
    static final Map<String, List<String>> SUBJECT_TOPICS = new LinkedHashMap<>() {{
        put("데이터 모델링의 이해", List.of("데이터 모델링 개요", "엔터티", "속성", "관계", "식별자"));
        put("데이터 모델과 SQL", List.of("정규화", "성능 데이터 모델링"));
        put("SQL 기본", List.of("DDL", "DML", "TCL", "WHERE절/조건", "함수", "NULL 처리 함수", "GROUP BY/HAVING", "ORDER BY", "제약조건"));
        put("SQL 활용", List.of("JOIN", "서브쿼리", "집합연산자", "ROLLUP/CUBE/GROUPING", "윈도우 함수", "계층형 질의", "PIVOT/UNPIVOT", "정규표현식"));
        put("관리 구문", List.of("DCL", "옵티마이저/인덱스"));
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

        // Few-shot 예시 (기본/심화/고난도 3개)
        List<String> examples = TopicExamples.EXAMPLES.get(request.topicName());
        if (examples != null && !examples.isEmpty()) {
            sb.append("예시 문제 (이 퀄리티와 형식을 참고하세요):\n\n");
            String[] labels = {"[기본]", "[심화]", "[고난도]"};
            for (int i = 0; i < examples.size() && i < 3; i++) {
                sb.append(labels[i]).append("\n").append(examples.get(i)).append("\n\n");
            }
        }

        if (!request.existingSummaries().isEmpty()) {
            sb.append("이미 출제된 관점 (아래와 동일한 관점은 피해주세요):\n");
            sb.append(request.existingSummaries().stream()
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        sb.append("위 예시와 동일한 퀄리티로 기본/심화/고난도 각 1문제, 총 3문제를 생성하세요.");
        return sb.toString();
    }

    /**
     * 정처기 카테고리 생성용 프롬프트 (시드 풀 다중화 + 사용자 지정 난이도 버전).
     * - N개의 서로 다른 시드를 "주제·식별자 회피용 참고자료"로 제공
     * - 각 시드별로 1개씩 변형 생성 요청 (N개 시드 → N개 문제)
     * - **각 문제별로 사용자 지정 targetDifficulty(1/2/3)를 강제** — 시드 난이도는 무시
     * - existingSummaries + forbiddenIdentifiers + recentAnswers로 중복 회피
     */
    static String buildEngineerPrompt(AiGenerationRequest request,
                                      List<EngineerExample> examples,
                                      List<Integer> targetDifficulties,
                                      List<String> forbiddenIdentifiers,
                                      List<String> recentAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("카테고리: ").append(request.subjectName()).append("\n");
        sb.append("필요 문항 수: ").append(examples.size()).append("개 (시드 1개당 변형 1개씩)\n\n");

        for (int i = 0; i < examples.size(); i++) {
            EngineerExample ex = examples.get(i);
            int target = targetDifficulties.get(i);
            sb.append("[문제 #").append(i + 1)
                    .append(" — **목표 난이도: ").append(targetLabel(target))
                    .append(" (").append(target).append(")**]\n");
            sb.append("(아래 시드는 주제·패턴·식별자 회피용 참고. 시드 자체 난이도는 무시하고 위 목표 난이도로 작성)\n");
            sb.append("토픽 힌트: ").append(ex.topic()).append("\n");
            sb.append("문제 유형: ").append(ex.questionType().name()).append("\n");
            sb.append("시드 본문: ").append(ex.content()).append("\n");
            sb.append("시드 정답: ").append(ex.answer()).append("\n");
            sb.append("시드 키워드: ").append(String.join(", ", ex.keywords())).append("\n");
            sb.append("시드 해설: ").append(ex.explanation()).append("\n\n");
        }

        if (forbiddenIdentifiers != null && !forbiddenIdentifiers.isEmpty()) {
            sb.append("[절대 사용 금지 식별자] (시드의 함수/클래스/변수명 + 최근 출제 식별자)\n");
            sb.append(String.join(", ", forbiddenIdentifiers)).append("\n\n");
        }

        if (recentAnswers != null && !recentAnswers.isEmpty()) {
            sb.append("[회피해야 할 정답 패턴] (이 정답들과 동일하거나 매우 유사하면 안 됨)\n");
            sb.append(recentAnswers.stream()
                    .filter(a -> a != null && !a.isBlank())
                    .limit(15)
                    .map(a -> a.length() > 80 ? a.substring(0, 80) + "…" : a)
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        if (request.existingSummaries() != null && !request.existingSummaries().isEmpty()) {
            sb.append("[이미 출제된 관점] (아래와 동일/유사한 관점은 피해주세요)\n");
            sb.append(request.existingSummaries().stream()
                    .limit(20)
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        sb.append("[지시]\n");
        sb.append("- 위 ").append(examples.size()).append("개 시드 각각에 대해 변형 1개씩, 정확히 ")
                .append(examples.size()).append("개의 문제를 생성하세요.\n");
        sb.append("- **각 문제는 위에 명시된 목표 난이도를 반드시 따르세요. 시드 자체 난이도는 무시합니다.**\n");
        sb.append("- 각 변형은 매핑된 시드의 유형/카테고리를 유지하되, 다른 개념·다른 식별자·다른 코드 구조여야 합니다.\n");
        sb.append("- 시드의 정답 형식(예: \"2 4 1 6 1 3\" 같은 숫자 나열)이 있다면 다른 형식으로 출제하세요.\n");
        sb.append("- difficulty 필드에는 각 문제의 목표 난이도(1/2/3)를 그대로 반환하세요.\n");
        sb.append("- 응답 questions 배열의 i번째 원소는 문제 #(i+1)에 대응되어야 합니다.\n");
        return sb.toString();
    }

    /**
     * SQLD 카테고리 생성용 프롬프트 — TopicExamples의 토픽별 [기본/심화/고난도] JSON 시드 사용.
     * - seedJsons: TopicExamples.randomFor()로 추출된 JSON 시드 문자열 N개
     * - targetDifficulties: 각 문제의 목표 난이도(1~4) — seedJsons와 동일 길이
     * - recentSummaries: 회피해야 할 최근 출제 관점
     */
    static String buildSqldSeedPrompt(AiGenerationRequest request,
                                      List<String> seedJsons,
                                      List<Integer> targetDifficulties,
                                      List<String> recentSummaries) {
        StringBuilder sb = new StringBuilder();
        sb.append("카테고리: ").append(request.subjectName()).append("\n");
        sb.append("필요 문항 수: ").append(seedJsons.size()).append("개 (시드 1개당 변형 1개씩)\n\n");

        for (int i = 0; i < seedJsons.size(); i++) {
            int target = targetDifficulties.get(i);
            sb.append("[문제 #").append(i + 1)
                    .append(" — **목표 난이도: ").append(targetLabel(target))
                    .append(" (").append(target).append(")**]\n");
            sb.append("(아래 시드는 주제·패턴·함정 회피용 참고. 시드 자체 난이도는 무시하고 위 목표 난이도로 작성)\n");
            sb.append("시드 (JSON): ").append(seedJsons.get(i)).append("\n\n");
        }

        if (recentSummaries != null && !recentSummaries.isEmpty()) {
            sb.append("[이미 출제된 관점] (아래와 동일/유사한 관점은 피해주세요)\n");
            sb.append(recentSummaries.stream()
                    .limit(20)
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        sb.append("[지시]\n");
        sb.append("- 위 ").append(seedJsons.size()).append("개 시드 각각에 대해 변형 1개씩, 정확히 ")
                .append(seedJsons.size()).append("개의 문제를 생성하세요.\n");
        sb.append("- **각 문제는 위에 명시된 목표 난이도를 반드시 따르세요. 시드 자체 난이도는 무시합니다.**\n");
        sb.append("- 본문 안에 ①②③④ 보기 4개를 모두 포함하세요. 시드의 SQL/테이블명/컬럼명 복제 금지.\n");
        sb.append("- correctOption 필드에 1~4 정수로 정답 번호를 반환하세요.\n");
        sb.append("- 응답 questions 배열의 i번째 원소는 문제 #(i+1)에 대응되어야 합니다.\n");
        return sb.toString();
    }

    private static String targetLabel(int difficulty) {
        return switch (difficulty) {
            case 1 -> "쉬움";
            case 2 -> "보통";
            case 3 -> "어려움";
            case 4 -> "매우 어려움";
            default -> "보통";
        };
    }

    /**
     * 컴퓨터활용능력 1급 필기 카테고리 생성용 프롬프트.
     * - N개의 시드를 주제·함정 회피용 참고로 동시 제공
     * - 각 문제별로 사용자 지정 targetDifficulty(1~4)를 강제
     * - 시드 1개당 변형 1개씩 N개 문제 생성
     */
    static String buildComputerLiteracyPrompt(AiGenerationRequest request,
                                              List<CL1Example> examples,
                                              List<Integer> targetDifficulties,
                                              List<String> recentSummaries,
                                              List<String> recentAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("카테고리: ").append(request.subjectName()).append("\n");
        sb.append("필요 문항 수: ").append(examples.size()).append("개 (시드 1개당 변형 1개씩)\n\n");

        for (int i = 0; i < examples.size(); i++) {
            CL1Example ex = examples.get(i);
            int target = targetDifficulties.get(i);
            sb.append("[문제 #").append(i + 1)
                    .append(" — **목표 난이도: ").append(targetLabel(target))
                    .append(" (").append(target).append(")**]\n");
            sb.append("(아래 시드는 주제·패턴·함정 회피용 참고. 시드 자체 난이도는 무시하고 위 목표 난이도로 작성)\n");
            sb.append("토픽 힌트: ").append(ex.topic()).append("\n");
            sb.append("시드 본문: ").append(ex.content()).append("\n");
            sb.append("시드 정답: ").append(ex.correctOption()).append("번\n");
            sb.append("시드 해설: ").append(ex.explanation()).append("\n\n");
        }

        if (recentAnswers != null && !recentAnswers.isEmpty()) {
            sb.append("[회피해야 할 최근 출제 정답/요약 패턴] (이 항목들과 동일하거나 매우 유사하면 안 됨)\n");
            sb.append(recentAnswers.stream()
                    .filter(a -> a != null && !a.isBlank())
                    .limit(15)
                    .map(a -> a.length() > 80 ? a.substring(0, 80) + "…" : a)
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        if (recentSummaries != null && !recentSummaries.isEmpty()) {
            sb.append("[이미 출제된 관점] (아래와 동일/유사한 관점은 피해주세요)\n");
            sb.append(recentSummaries.stream()
                    .limit(20)
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        sb.append("[지시]\n");
        sb.append("- 위 ").append(examples.size()).append("개 시드 각각에 대해 변형 1개씩, 정확히 ")
                .append(examples.size()).append("개의 문제를 생성하세요.\n");
        sb.append("- **각 문제는 위에 명시된 목표 난이도를 반드시 따르세요. 시드 자체 난이도는 무시합니다.**\n");
        sb.append("- 본문 안에 ①②③④ 보기 4개를 모두 포함하세요. 시드 보기 텍스트를 복제 금지.\n");
        sb.append("- correctOption 필드에 1~4 정수로 정답 번호를 반환하세요.\n");
        sb.append("- 응답 questions 배열의 i번째 원소는 문제 #(i+1)에 대응되어야 합니다.\n");
        return sb.toString();
    }

    static String buildLegacyVerificationPrompt(AiVerificationRequest request) {
        GeneratedQuestion q = request.question();
        return "과목: " + request.subjectName() + "\n\n" +
                "문제:\n" + q.content() + "\n\n" +
                "정답: " + q.correctOption() + "번\n\n" +
                "해설:\n" + q.explanation();
    }

    static String buildVerificationSystemPrompt(AiVerificationRequest request) {
        return switch (resolveExamType(request)) {
            case ENGINEER_PRACTICAL -> """
                    당신은 정보처리기사 실기 문제 검수 전문가입니다.
                    주어진 문제, 모범답안, 채점 키워드, 해설이 서로 일관적인지 검토하세요.

                    검토 기준:
                    1. 문제 유형(questionType)에 맞는 정답(answerText)인지
                    2. keywords가 채점 기준으로 충분하고 answerText와 모순되지 않는지
                    3. 해설이 정답 근거를 정확히 설명하는지
                    4. 문제 표현이 명확하고 난이도가 요청된 수준과 어긋나지 않는지

                    반드시 아래 JSON 형식으로만 응답하세요.
                    {"approved": true, "reason": "확인/거절 사유"}
                    """;
            case COMPUTER_LITERACY_1 -> """
                    당신은 컴퓨터활용능력 1급 실기 객관식 문제 검수 전문가입니다.
                    주어진 문제, 정답 번호, 해설이 서로 일관적인지 검토하세요.

                    검토 기준:
                    1. 정답 번호가 실제로 맞는지
                    2. 오답 선택지가 명확하게 틀렸는지
                    3. 해설이 정답과 오답의 이유를 모두 설명하는지
                    4. 문제 표현이 명확하고 난이도가 요청된 수준과 어긋나지 않는지

                    반드시 아래 JSON 형식으로만 응답하세요.
                    {"approved": true, "reason": "확인/거절 사유"}
                    """;
            case SQLD -> """
                    당신은 SQLD 객관식 문제 검수 전문가입니다.
                    주어진 문제, 정답 번호, 해설이 서로 일관적인지 검토하세요.

                    검토 기준:
                    1. 정답 번호가 실제로 맞는지
                    2. 오답 선택지가 명확하게 틀렸는지
                    3. 해설이 정답과 오답의 이유를 모두 설명하는지
                    4. 문제 표현이 명확하고 난이도가 요청된 수준과 어긋나지 않는지

                    반드시 아래 JSON 형식으로만 응답하세요.
                    {"approved": true, "reason": "확인/거절 사유"}
                    """;
            case ENGINEER_WRITTEN -> """
                    당신은 정보처리기사 필기 객관식 문제 검수 전문가입니다.
                    주어진 문제, 정답 번호, 해설이 서로 일관적인지 검토하세요.

                    검토 기준:
                    1. 정답 번호가 실제로 맞는지
                    2. 오답 선택지가 명확하게 틀렸는지
                    3. 해설이 정답과 오답의 이유를 모두 설명하는지
                    4. 문제 표현이 명확하고 난이도가 요청된 수준과 어긋나지 않는지

                    반드시 아래 JSON 형식으로만 응답하세요.
                    {"approved": true, "reason": "확인/거절 사유"}
                    """;
        };
    }

    static String buildVerificationPrompt(AiVerificationRequest request) {
        GeneratedQuestion q = request.question();
        QuestionType questionType = resolveQuestionType(q);

        StringBuilder sb = new StringBuilder();
        sb.append("시험 유형: ").append(resolveExamType(request)).append("\n");
        sb.append("과목: ").append(request.subjectName()).append("\n");
        if (q.topic() != null && !q.topic().isBlank()) {
            sb.append("토픽: ").append(q.topic()).append("\n");
        }
        if (q.summary() != null && !q.summary().isBlank()) {
            sb.append("요약: ").append(q.summary()).append("\n");
        }
        if (q.difficulty() != null) {
            sb.append("난이도: ").append(q.difficulty()).append("\n");
        }
        sb.append("문제 유형: ").append(questionType.name()).append("\n\n");
        sb.append("문제:\n").append(q.content()).append("\n\n");

        if (questionType == QuestionType.MCQ) {
            sb.append("정답 번호: ")
                    .append(q.correctOption() == null ? "(없음)" : q.correctOption())
                    .append("\n\n");
        } else {
            sb.append("모범 답안:\n")
                    .append(q.answerText() == null || q.answerText().isBlank() ? "(없음)" : q.answerText())
                    .append("\n\n");
            if (q.keywords() != null && !q.keywords().isEmpty()) {
                sb.append("채점 키워드: ").append(String.join(", ", q.keywords())).append("\n\n");
            }
        }

        sb.append("해설:\n")
                .append(q.explanation() == null || q.explanation().isBlank() ? "(없음)" : q.explanation());
        return sb.toString();
    }

    private static ExamType resolveExamType(AiVerificationRequest request) {
        return request.examType() != null ? request.examType() : ExamType.SQLD;
    }

    private static QuestionType resolveQuestionType(GeneratedQuestion question) {
        if (question.questionType() == null || question.questionType().isBlank()) {
            return QuestionType.MCQ;
        }
        try {
            return QuestionType.valueOf(question.questionType());
        } catch (IllegalArgumentException e) {
            return QuestionType.MCQ;
        }
    }

    static String buildFixPrompt(GeneratedQuestion question, String reason) {
        return "검증 사유: " + reason + "\n\n" +
                "원본 문제:\n" +
                "{\"content\": \"" + escape(question.content()) + "\", " +
                "\"correctOption\": " + question.correctOption() + ", " +
                "\"explanation\": \"" + escape(question.explanation()) + "\", " +
                "\"summary\": \"" + escape(question.summary()) + "\", " +
                "\"difficulty\": " + question.difficulty() + "}\n\n" +
                "위 사유를 반영하여 수정한 문제를 JSON으로 반환하세요.";
    }

    static String buildShortAnswerFixPrompt(GeneratedQuestion question, String reason) {
        return "검증 사유: " + reason + "\n\n" +
                "원본 문제 (단답형):\n" +
                "{\"content\": \"" + escape(question.content()) + "\", " +
                "\"questionType\": \"SHORT_ANSWER\", " +
                "\"answerText\": \"" + escape(nullSafe(question.answerText())) + "\", " +
                "\"keywords\": " + (question.keywords() != null ? question.keywords().toString() : "[]") + ", " +
                "\"explanation\": \"" + escape(nullSafe(question.explanation())) + "\", " +
                "\"summary\": \"" + escape(nullSafe(question.summary())) + "\", " +
                "\"difficulty\": " + question.difficulty() + "}\n\n" +
                "위 사유를 반영하여 수정한 문제를 JSON으로 반환하세요. " +
                "answerText가 keywords 중 하나와 정확히 일치(혹은 alias)하도록 보장하세요.";
    }

    static String buildDescriptiveFixPrompt(GeneratedQuestion question, String reason) {
        return "검증 사유: " + reason + "\n\n" +
                "원본 문제 (약술형):\n" +
                "{\"content\": \"" + escape(question.content()) + "\", " +
                "\"questionType\": \"DESCRIPTIVE\", " +
                "\"answerText\": \"" + escape(nullSafe(question.answerText())) + "\", " +
                "\"keywords\": " + (question.keywords() != null ? question.keywords().toString() : "[]") + ", " +
                "\"explanation\": \"" + escape(nullSafe(question.explanation())) + "\", " +
                "\"summary\": \"" + escape(nullSafe(question.summary())) + "\", " +
                "\"difficulty\": " + question.difficulty() + "}\n\n" +
                "위 사유를 반영하여 수정한 문제를 JSON으로 반환하세요. " +
                "keywords의 모든 항목이 answerText 안에 자연스럽게 포함되어야 합니다.";
    }

    /**
     * 배치 검증 시스템 프롬프트 — 단건과 같은 시험·과목 규칙을 그대로 가져오되,
     * "여러 문제를 받아 results 배열로 답하라" 지시를 끝에 덧붙임.
     */
    static String buildBatchVerificationSystemPrompt(AiVerificationRequest request) {
        return buildVerificationSystemPrompt(request) + "\n\n" +
                "여러 문제를 한 번에 받아 각 문제별 판정 결과를 results 배열로 반환합니다. " +
                "반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 금지):\n" +
                "{\"results\":[" +
                "{\"index\":0,\"approved\":true}," +
                "{\"index\":1,\"approved\":false,\"reason\":\"정답이 2가 아니라 3\",\"fixable\":true}" +
                "]}\n" +
                "- index는 입력에 표시된 순번 그대로 사용하세요.\n" +
                "- approved=false일 때만 reason과 fixable을 포함하세요.\n" +
                "- fixable=true는 본문/정답을 합리적으로 수정하면 정상화 가능한 경우, " +
                "fixable=false는 문제 자체가 복구 불가한 경우입니다.";
    }

    static String buildBatchVerificationUserPrompt(java.util.List<AiVerificationRequest> requests) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래 ").append(requests.size()).append("개 문제를 각각 판정하세요.\n\n");
        for (int i = 0; i < requests.size(); i++) {
            GeneratedQuestion q = requests.get(i).question();
            sb.append("=== #").append(i).append(" ===\n");
            sb.append("content: ").append(nullSafe(q.content())).append("\n");
            if (q.questionType() != null) {
                sb.append("type: ").append(q.questionType()).append("\n");
            }
            if (q.correctOption() != null) {
                sb.append("correctOption: ").append(q.correctOption()).append("\n");
            }
            if (q.answerText() != null) {
                sb.append("answer: ").append(q.answerText()).append("\n");
            }
            if (q.keywords() != null && !q.keywords().isEmpty()) {
                sb.append("keywords: ").append(q.keywords()).append("\n");
            }
            if (q.explanation() != null) {
                sb.append("explanation: ").append(q.explanation()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("응답: results 배열로 반환.");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
