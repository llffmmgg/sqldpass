package com.sqldpass.service.generation;

import java.util.stream.Collectors;

public class PromptBuilder {

    static final String GENERATION_SYSTEM_PROMPT = """
            당신은 SQLD(SQL 개발자) 자격증 시험 출제위원입니다.
            주어진 과목에 대해 4지선다 객관식 문제를 생성하세요.

            규칙:
            - 실제 SQLD 시험 난이도와 형식을 따를 것
            - 정답은 반드시 1개만 존재
            - "가장 적절한 것" 또는 "가장 적절하지 않은 것" 형식 사용
            - 선택지는 ①②③④ 기호 사용
            - 해설은 정답인 이유와 오답인 이유를 포함
            - summary는 이 문제가 어떤 관점/각도에서 출제되었는지 200자 이내로 요약

            반드시 아래 JSON 형식으로만 응답하세요:
            {"questions": [{"content": "문제 본문\\n\\n① 선택지1\\n② 선택지2\\n③ 선택지3\\n④ 선택지4", "correctOption": 1, "explanation": "해설", "summary": "출제 관점 요약"}]}
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

    private PromptBuilder() {
    }

    static String buildGenerationPrompt(AiGenerationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("과목: ").append(request.subjectName()).append("\n");
        sb.append("생성할 문제 수: ").append(request.count()).append("개\n\n");

        if (!request.existingSummaries().isEmpty()) {
            sb.append("이미 출제된 관점 (아래와 동일한 관점은 피해주세요):\n");
            sb.append(request.existingSummaries().stream()
                    .collect(Collectors.joining("\n- ", "- ", "\n")));
        }

        return sb.toString();
    }

    static String buildVerificationPrompt(AiVerificationRequest request) {
        GeneratedQuestion q = request.question();
        return "과목: " + request.subjectName() + "\n\n" +
                "문제:\n" + q.content() + "\n\n" +
                "정답: " + q.correctOption() + "번\n\n" +
                "해설:\n" + q.explanation();
    }
}
