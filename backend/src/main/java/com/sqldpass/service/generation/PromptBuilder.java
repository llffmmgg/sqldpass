package com.sqldpass.service.generation;

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
            아래 시드 예시들은 "이 정도 깊이/완성도/품질"의 기준이며, 절대 그 코드/식별자/구조를 복제해서는 안 됩니다.
            매번 새로운 개념·새로운 함정·새로운 변수명으로 출제하세요.

            규칙:
            - 실제 정처기 실기 시험과 동일한 형식: 단답형(SHORT_ANSWER) 또는 약술형(DESCRIPTIVE)
            - 객관식(MCQ)은 절대 만들지 말 것
            - 시드별로 매핑된 questionType을 그대로 따를 것
            - 코드형(C/Java/Python/SQL) 문제는 코드의 실행 결과를 묻는 단답형으로 작성
            - 이론형 문제는 핵심 용어 단답 또는 개념 약술
            - 시드의 difficulty(1=기본, 3=중급, 5=고난도)와 동일한 난이도로 작성할 것
            - answer 필드: 정답(모범답안)
            - keywords 필드: 단답형이면 허용 alias 리스트, 약술형이면 채점 키워드 리스트
            - explanation: 풀이 과정/근거를 자세히
            - summary: 200자 이내, 출제 관점 요약
            - difficulty 필드는 시드의 difficulty 값을 그대로 사용

            절대 금지:
            - 시드의 함수명/클래스명/변수명을 그대로 사용 (변형해도 인식 가능하면 안 됨)
            - 시드의 코드 구조를 그대로 따라하기 (재귀를 재귀로, Shape→Circle 같은 단순 치환)
            - 시드와 동일한 주제·동일한 함정 (다른 개념·다른 함정으로 작성)
            - 회피 식별자/회피 정답 목록에 있는 항목과의 중복

            출력 전 자체 검증:
            1. 코드형이면 직접 실행 결과가 answer와 맞는지 확인
            2. answer가 명확하고 keywords가 채점에 충분한지 확인
            3. 시드 식별자 / 회피 식별자 / 회피 정답과 겹치지 않는지 확인
            4. 다양한 출제 의도(실행 결과 예측 / 빈칸 채우기 / 약술 / 잘못 짚기) 중 시드와 다른 각도 선택

            반드시 아래 JSON 형식으로만 응답하세요. questions 배열의 길이는 입력된 시드 개수와 정확히 일치해야 합니다:
            {"questions": [
              {"content": "...", "questionType": "SHORT_ANSWER", "answerText": "...", "keywords": ["..."], "explanation": "...", "summary": "...", "difficulty": 3}
            ]}
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
     * 정처기 카테고리 생성용 프롬프트 (시드 풀 다중화 버전).
     * - N개의 서로 다른 시드를 "퀄리티 기준 + 회피 대상"으로 동시에 제공
     * - 각 시드별로 1개씩 변형 생성 요청 (N개 시드 → N개 문제)
     * - 시드의 난이도를 그대로 계승 (자연스러운 난이도 분산)
     * - existingSummaries + forbiddenIdentifiers + recentAnswers로 중복 회피
     */
    static String buildEngineerPrompt(AiGenerationRequest request,
                                      List<EngineerExample> examples,
                                      List<String> forbiddenIdentifiers,
                                      List<String> recentAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("카테고리: ").append(request.subjectName()).append("\n");
        sb.append("필요 문항 수: ").append(examples.size()).append("개 (시드 1개당 변형 1개씩)\n\n");

        for (int i = 0; i < examples.size(); i++) {
            EngineerExample ex = examples.get(i);
            sb.append("[시드 #").append(i + 1).append(" - 난이도 ").append(ex.difficulty()).append("]\n");
            sb.append("토픽 힌트: ").append(ex.topic()).append("\n");
            sb.append("문제 유형: ").append(ex.questionType().name()).append("\n");
            sb.append("문제: ").append(ex.content()).append("\n");
            sb.append("정답: ").append(ex.answer()).append("\n");
            sb.append("키워드: ").append(String.join(", ", ex.keywords())).append("\n");
            sb.append("해설: ").append(ex.explanation()).append("\n\n");
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
        sb.append("- 각 변형은 매핑된 시드의 난이도/유형/카테고리를 유지하되, 다른 개념·다른 식별자·다른 코드 구조여야 합니다.\n");
        sb.append("- 시드의 정답 형식(예: \"2 4 1 6 1 3\" 같은 숫자 나열)이 있다면 다른 형식으로 출제하세요.\n");
        sb.append("- 응답 questions 배열의 i번째 원소는 시드 #").append("(i+1)에 대응되어야 합니다.\n");
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
