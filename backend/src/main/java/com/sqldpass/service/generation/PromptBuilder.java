package com.sqldpass.service.generation;

import com.sqldpass.service.generation.EngineerTopicExamples.EngineerExample;
import com.sqldpass.service.generation.dto.*;
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
            주어진 카테고리의 예시 문제를 참고하여, 최근 실기 출제 기조에 맞춰 어려운 변형 문제를 생성하세요.

            규칙:
            - 실제 정처기 실기 시험과 동일한 형식: 단답형(SHORT_ANSWER) 또는 약술형(DESCRIPTIVE)
            - 객관식(MCQ)은 절대 만들지 말 것
            - 예시의 questionType을 그대로 따를 것 (예시가 SHORT_ANSWER면 단답형으로, DESCRIPTIVE면 약술형으로)
            - 코드형(C/Java/Python/SQL) 문제는 코드의 실행 결과를 묻는 단답형으로 작성
            - 이론형 문제는 핵심 용어 단답 또는 개념 약술
            - 최근 회차 기조 = 어렵게 (예시와 동일한 고난도 수준 유지)
            - answer 필드: 정답(모범답안)
            - keywords 필드: 단답형이면 허용 alias 리스트, 약술형이면 채점 키워드 리스트
            - explanation: 풀이 과정/근거를 자세히
            - summary: 200자 이내, 출제 관점 요약
            - difficulty는 5(고난도) 고정

            출력 전 자체 검증:
            1. 코드형이면 직접 실행 결과가 answer와 맞는지 확인
            2. answer가 명확하고 keywords가 채점에 충분한지 확인
            3. 회피 관점 목록에 있는 주제와 겹치지 않는지 확인

            반드시 아래 JSON 형식으로만 응답하세요:
            {"questions": [
              {"content": "...", "questionType": "SHORT_ANSWER", "answerText": "...", "keywords": ["..."], "explanation": "...", "summary": "...", "difficulty": 5}
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
     * 정처기 카테고리 생성용 프롬프트.
     * - 예시 1개를 few-shot으로 제시하고
     * - existingSummaries로 중복 회피 지시
     * - count 만큼 변형 생성 요청
     */
    static String buildEngineerPrompt(AiGenerationRequest request, EngineerExample example) {
        StringBuilder sb = new StringBuilder();
        sb.append("카테고리: ").append(request.subjectName()).append("\n");
        sb.append("토픽 힌트: ").append(example.topic()).append("\n");
        sb.append("문제 유형: ").append(example.questionType().name()).append("\n\n");

        sb.append("예시 문제 (이 난이도/스타일/유형을 그대로 따르세요):\n\n");
        sb.append("[고난도 예시]\n");
        sb.append("문제: ").append(example.content()).append("\n");
        sb.append("정답: ").append(example.answer()).append("\n");
        sb.append("키워드: ").append(String.join(", ", example.keywords())).append("\n");
        sb.append("해설: ").append(example.explanation()).append("\n\n");

        if (request.existingSummaries() != null && !request.existingSummaries().isEmpty()) {
            sb.append("이미 출제된 관점 (아래와 동일/유사한 관점은 피해주세요):\n");
            sb.append(request.existingSummaries().stream()
                    .collect(Collectors.joining("\n- ", "- ", "\n\n")));
        }

        sb.append("위 예시와 동일한 카테고리/유형/난이도(고난도)로 ")
                .append(request.count())
                .append("개의 서로 다른 변형 문제를 생성하세요.");
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
