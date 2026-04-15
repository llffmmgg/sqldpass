package com.sqldpass.service.generation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.generation.dto.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AiProvider {

    /**
     * 한 번의 AI 호출에서 생성할 최대 문제 수.
     * Claude Sonnet 4 출력 한도 8192 토큰을 초과하지 않도록 안전 마진 확보.
     * 객관식(보기4 + 해설)은 1문항당 ~500 토큰. 단답형은 ~300 토큰.
     * needed가 이 값을 초과하면 자동으로 분할 호출.
     */
    private static final int MAX_QUESTIONS_PER_CALL = 8;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AiProvider(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    public AiGenerationResponse generateQuestions(AiGenerationRequest request) {
        String prompt = PromptBuilder.buildGenerationPrompt(request);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse generation response: {}", responseText, e);
            return new AiGenerationResponse(List.of());
        }
    }

    /**
     * 정처기 카테고리용 변형 문제 N개 생성 (시드 풀 다중화 + 사용자 지정 난이도 버전).
     * needed가 MAX_QUESTIONS_PER_CALL을 초과하면 자동 chunk 분할 호출 후 결과 합치기.
     */
    public AiGenerationResponse generateEngineerQuestions(AiGenerationRequest request,
                                                          List<EngineerTopicExamples.EngineerExample> examples,
                                                          List<Integer> targetDifficulties,
                                                          List<String> forbiddenIdentifiers,
                                                          List<String> recentAnswers) {
        if (examples.size() <= MAX_QUESTIONS_PER_CALL) {
            return callEngineerOnce(request, examples, targetDifficulties, forbiddenIdentifiers, recentAnswers);
        }
        List<GeneratedQuestion> all = new ArrayList<>(examples.size());
        for (int start = 0; start < examples.size(); start += MAX_QUESTIONS_PER_CALL) {
            int end = Math.min(start + MAX_QUESTIONS_PER_CALL, examples.size());
            List<EngineerTopicExamples.EngineerExample> seedChunk = examples.subList(start, end);
            List<Integer> diffChunk = targetDifficulties.subList(start, end);
            log.info("정처기 chunk 호출 [{}] {}~{}/{} (size={})",
                    request.subjectName(), start, end, examples.size(), seedChunk.size());
            AiGenerationResponse partial = callEngineerOnce(request, seedChunk, diffChunk, forbiddenIdentifiers, recentAnswers);
            if (partial.questions() == null || partial.questions().size() < seedChunk.size()) {
                throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                        "정처기 chunk 실패 [" + request.subjectName() + "] " + start + "~" + end);
            }
            all.addAll(partial.questions().subList(0, seedChunk.size()));
        }
        return new AiGenerationResponse(all);
    }

    private AiGenerationResponse callEngineerOnce(AiGenerationRequest request,
                                                  List<EngineerTopicExamples.EngineerExample> examples,
                                                  List<Integer> targetDifficulties,
                                                  List<String> forbiddenIdentifiers,
                                                  List<String> recentAnswers) {
        String prompt = PromptBuilder.buildEngineerPrompt(request, examples, targetDifficulties, forbiddenIdentifiers, recentAnswers);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.ENGINEER_GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse engineer generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "정처기 AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * SQLD 카테고리용 변형 문제 N개 생성 — TopicExamples의 토픽별 [기본/심화/고난도] JSON 시드 사용.
     * needed가 MAX_QUESTIONS_PER_CALL을 초과하면 자동 chunk 분할.
     */
    public AiGenerationResponse generateSqldFromSeeds(AiGenerationRequest request,
                                                      List<String> seedJsons,
                                                      List<Integer> targetDifficulties,
                                                      List<String> recentSummaries) {
        if (seedJsons.size() <= MAX_QUESTIONS_PER_CALL) {
            return callSqldOnce(request, seedJsons, targetDifficulties, recentSummaries);
        }
        List<GeneratedQuestion> all = new ArrayList<>(seedJsons.size());
        for (int start = 0; start < seedJsons.size(); start += MAX_QUESTIONS_PER_CALL) {
            int end = Math.min(start + MAX_QUESTIONS_PER_CALL, seedJsons.size());
            List<String> seedChunk = seedJsons.subList(start, end);
            List<Integer> diffChunk = targetDifficulties.subList(start, end);
            log.info("SQLD chunk 호출 [{}] {}~{}/{} (size={})",
                    request.subjectName(), start, end, seedJsons.size(), seedChunk.size());
            AiGenerationResponse partial = callSqldOnce(request, seedChunk, diffChunk, recentSummaries);
            if (partial.questions() == null || partial.questions().size() < seedChunk.size()) {
                throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                        "SQLD chunk 실패 [" + request.subjectName() + "] " + start + "~" + end);
            }
            all.addAll(partial.questions().subList(0, seedChunk.size()));
        }
        return new AiGenerationResponse(all);
    }

    private AiGenerationResponse callSqldOnce(AiGenerationRequest request,
                                              List<String> seedJsons,
                                              List<Integer> targetDifficulties,
                                              List<String> recentSummaries) {
        String prompt = PromptBuilder.buildSqldSeedPrompt(request, seedJsons, targetDifficulties, recentSummaries);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.SQLD_SEED_GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse SQLD seed generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "SQLD AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * 컴활 1급 필기 카테고리용 변형 문제 N개 생성 (시드 풀 + 사용자 지정 난이도).
     * needed가 MAX_QUESTIONS_PER_CALL을 초과하면 자동 chunk 분할.
     */
    public AiGenerationResponse generateComputerLiteracyQuestions(AiGenerationRequest request,
                                                                  List<ComputerLiteracyTopicExamples.CL1Example> examples,
                                                                  List<Integer> targetDifficulties,
                                                                  List<String> recentSummaries,
                                                                  List<String> recentAnswers) {
        if (examples.size() <= MAX_QUESTIONS_PER_CALL) {
            return callCl1Once(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        }
        List<GeneratedQuestion> all = new ArrayList<>(examples.size());
        for (int start = 0; start < examples.size(); start += MAX_QUESTIONS_PER_CALL) {
            int end = Math.min(start + MAX_QUESTIONS_PER_CALL, examples.size());
            List<ComputerLiteracyTopicExamples.CL1Example> seedChunk = examples.subList(start, end);
            List<Integer> diffChunk = targetDifficulties.subList(start, end);
            log.info("컴활 chunk 호출 [{}] {}~{}/{} (size={})",
                    request.subjectName(), start, end, examples.size(), seedChunk.size());
            AiGenerationResponse partial = callCl1Once(request, seedChunk, diffChunk, recentSummaries, recentAnswers);
            if (partial.questions() == null || partial.questions().size() < seedChunk.size()) {
                throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                        "컴활 chunk 실패 [" + request.subjectName() + "] " + start + "~" + end);
            }
            all.addAll(partial.questions().subList(0, seedChunk.size()));
        }
        return new AiGenerationResponse(all);
    }

    private AiGenerationResponse callCl1Once(AiGenerationRequest request,
                                             List<ComputerLiteracyTopicExamples.CL1Example> examples,
                                             List<Integer> targetDifficulties,
                                             List<String> recentSummaries,
                                             List<String> recentAnswers) {
        String prompt = PromptBuilder.buildComputerLiteracyPrompt(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.COMPUTER_LITERACY_GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse computer literacy generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "컴활 AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * 컴활 2급 필기 카테고리용 변형 문제 N개 생성 (시드 풀 + 사용자 지정 난이도).
     * needed가 MAX_QUESTIONS_PER_CALL을 초과하면 자동 chunk 분할.
     */
    public AiGenerationResponse generateComputerLiteracy2Questions(AiGenerationRequest request,
                                                                    List<ComputerLiteracy2TopicExamples.CL2Example> examples,
                                                                    List<Integer> targetDifficulties,
                                                                    List<String> recentSummaries,
                                                                    List<String> recentAnswers) {
        if (examples.size() <= MAX_QUESTIONS_PER_CALL) {
            return callCl2Once(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        }
        List<GeneratedQuestion> all = new ArrayList<>(examples.size());
        for (int start = 0; start < examples.size(); start += MAX_QUESTIONS_PER_CALL) {
            int end = Math.min(start + MAX_QUESTIONS_PER_CALL, examples.size());
            List<ComputerLiteracy2TopicExamples.CL2Example> seedChunk = examples.subList(start, end);
            List<Integer> diffChunk = targetDifficulties.subList(start, end);
            log.info("컴활2 chunk 호출 [{}] {}~{}/{} (size={})",
                    request.subjectName(), start, end, examples.size(), seedChunk.size());
            AiGenerationResponse partial = callCl2Once(request, seedChunk, diffChunk, recentSummaries, recentAnswers);
            if (partial.questions() == null || partial.questions().size() < seedChunk.size()) {
                throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                        "컴활2 chunk 실패 [" + request.subjectName() + "] " + start + "~" + end);
            }
            all.addAll(partial.questions().subList(0, seedChunk.size()));
        }
        return new AiGenerationResponse(all);
    }

    private AiGenerationResponse callCl2Once(AiGenerationRequest request,
                                             List<ComputerLiteracy2TopicExamples.CL2Example> examples,
                                             List<Integer> targetDifficulties,
                                             List<String> recentSummaries,
                                             List<String> recentAnswers) {
        String prompt = PromptBuilder.buildComputerLiteracy2Prompt(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.COMPUTER_LITERACY_2_GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse computer literacy 2 generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "컴활2 AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * ADsP 카테고리용 변형 문제 N개 생성 (시드 풀 + 사용자 지정 난이도).
     * needed가 MAX_QUESTIONS_PER_CALL을 초과하면 자동 chunk 분할.
     */
    public AiGenerationResponse generateAdspQuestions(AiGenerationRequest request,
                                                       List<AdspTopicExamples.AdspExample> examples,
                                                       List<Integer> targetDifficulties,
                                                       List<String> recentSummaries,
                                                       List<String> recentAnswers) {
        if (examples.size() <= MAX_QUESTIONS_PER_CALL) {
            return callAdspOnce(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        }
        List<GeneratedQuestion> all = new ArrayList<>(examples.size());
        for (int start = 0; start < examples.size(); start += MAX_QUESTIONS_PER_CALL) {
            int end = Math.min(start + MAX_QUESTIONS_PER_CALL, examples.size());
            List<AdspTopicExamples.AdspExample> seedChunk = examples.subList(start, end);
            List<Integer> diffChunk = targetDifficulties.subList(start, end);
            log.info("ADsP chunk 호출 [{}] {}~{}/{} (size={})",
                    request.subjectName(), start, end, examples.size(), seedChunk.size());
            AiGenerationResponse partial = callAdspOnce(request, seedChunk, diffChunk, recentSummaries, recentAnswers);
            if (partial.questions() == null || partial.questions().size() < seedChunk.size()) {
                throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                        "ADsP chunk 실패 [" + request.subjectName() + "] " + start + "~" + end);
            }
            all.addAll(partial.questions().subList(0, seedChunk.size()));
        }
        return new AiGenerationResponse(all);
    }

    private AiGenerationResponse callAdspOnce(AiGenerationRequest request,
                                               List<AdspTopicExamples.AdspExample> examples,
                                               List<Integer> targetDifficulties,
                                               List<String> recentSummaries,
                                               List<String> recentAnswers) {
        String prompt = PromptBuilder.buildAdspPrompt(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.ADSP_GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse ADsP generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "ADsP AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * 정처기 필기 과목용 변형 문제 N개 생성 (시드 풀 + 사용자 지정 난이도).
     * needed가 MAX_QUESTIONS_PER_CALL을 초과하면 자동 chunk 분할.
     */
    public AiGenerationResponse generateEngineerWrittenQuestions(AiGenerationRequest request,
                                                                 List<EngineerWrittenTopicExamples.EWExample> examples,
                                                                 List<Integer> targetDifficulties,
                                                                 List<String> recentSummaries,
                                                                 List<String> recentAnswers) {
        if (examples.size() <= MAX_QUESTIONS_PER_CALL) {
            return callEwOnce(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        }
        List<GeneratedQuestion> all = new ArrayList<>(examples.size());
        for (int start = 0; start < examples.size(); start += MAX_QUESTIONS_PER_CALL) {
            int end = Math.min(start + MAX_QUESTIONS_PER_CALL, examples.size());
            List<EngineerWrittenTopicExamples.EWExample> seedChunk = examples.subList(start, end);
            List<Integer> diffChunk = targetDifficulties.subList(start, end);
            log.info("정처기 필기 chunk 호출 [{}] {}~{}/{} (size={})",
                    request.subjectName(), start, end, examples.size(), seedChunk.size());
            AiGenerationResponse partial = callEwOnce(request, seedChunk, diffChunk, recentSummaries, recentAnswers);
            if (partial.questions() == null || partial.questions().size() < seedChunk.size()) {
                throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                        "정처기 필기 chunk 실패 [" + request.subjectName() + "] " + start + "~" + end);
            }
            all.addAll(partial.questions().subList(0, seedChunk.size()));
        }
        return new AiGenerationResponse(all);
    }

    private AiGenerationResponse callEwOnce(AiGenerationRequest request,
                                            List<EngineerWrittenTopicExamples.EWExample> examples,
                                            List<Integer> targetDifficulties,
                                            List<String> recentSummaries,
                                            List<String> recentAnswers) {
        String prompt = PromptBuilder.buildEngineerWrittenPrompt(request, examples, targetDifficulties, recentSummaries, recentAnswers);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.ENGINEER_WRITTEN_GENERATION_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.has("questions") ? root.get("questions") : root;
            List<GeneratedQuestion> questions = objectMapper.readValue(
                    questionsNode.toString(), new TypeReference<>() {});
            return new AiGenerationResponse(questions);
        } catch (Exception e) {
            log.error("Failed to parse engineer written generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "정처기 필기 AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /** 단일 검증 — UNKNOWN(빈 응답/파싱 실패)일 땐 throw 대신 outcome으로 표현. */
    public AiVerificationResponse verifyQuestion(AiVerificationRequest request) {
        String prompt = PromptBuilder.buildVerificationPrompt(request);
        String responseText = safeCall(
                PromptBuilder.buildVerificationSystemPrompt(request), prompt);

        if (responseText == null || responseText.isBlank()) {
            log.warn("Verification empty response for [{}]", request.subjectName());
            return AiVerificationResponse.ofUnknown("LLM 빈 응답");
        }

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            boolean approved = root.has("approved") && root.get("approved").asBoolean();
            String reason = root.has("reason") ? root.get("reason").asText() : "";
            Boolean fixable = root.has("fixable") ? root.get("fixable").asBoolean() : null;
            return approved
                    ? AiVerificationResponse.ofApproved()
                    : AiVerificationResponse.ofRejected(reason, fixable);
        } catch (Exception e) {
            log.warn("Failed to parse verification response: {}", responseText, e);
            return AiVerificationResponse.ofUnknown("파싱 실패");
        }
    }

    /**
     * 배치 검증 — N문제를 한 번의 LLM 호출로 판정.
     * 응답은 {"results":[{"index":0,"approved":true},{"index":1,"approved":false,"reason":"...","fixable":true},...]} 형식.
     * 파싱 실패/길이 불일치 시 모두 UNKNOWN으로 채움 (다음 회차 재시도).
     */
    public List<AiVerificationResponse> verifyQuestionsBatch(List<AiVerificationRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();

        // 같은 시험·과목끼리만 들어온다고 가정 — 호출자가 묶어서 보낸다.
        AiVerificationRequest first = requests.get(0);
        String systemPrompt = PromptBuilder.buildBatchVerificationSystemPrompt(first);
        String userPrompt = PromptBuilder.buildBatchVerificationUserPrompt(requests);

        String responseText = safeCall(systemPrompt, userPrompt);
        if (responseText == null || responseText.isBlank()) {
            log.warn("Batch verification empty response — retrying once");
            responseText = safeCall(systemPrompt, userPrompt);
        }

        if (responseText == null || responseText.isBlank()) {
            log.warn("Batch verification still empty after retry — marking all UNKNOWN");
            return fillUnknown(requests.size(), "LLM 빈 응답");
        }

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            JsonNode resultsNode = root.has("results") ? root.get("results") : root;
            if (!resultsNode.isArray()) {
                log.warn("Batch verification response not an array: {}", responseText);
                return fillUnknown(requests.size(), "응답 포맷 오류");
            }

            List<AiVerificationResponse> out = new ArrayList<>(requests.size());
            for (int i = 0; i < requests.size(); i++) out.add(null);

            for (JsonNode item : resultsNode) {
                int idx = item.has("index") ? item.get("index").asInt(-1) : -1;
                if (idx < 0 || idx >= requests.size()) continue;
                boolean approved = item.has("approved") && item.get("approved").asBoolean();
                String reason = item.has("reason") ? item.get("reason").asText() : "";
                Boolean fixable = item.has("fixable") ? item.get("fixable").asBoolean() : null;
                out.set(idx, approved
                        ? AiVerificationResponse.ofApproved()
                        : AiVerificationResponse.ofRejected(reason, fixable));
            }
            // 비어 있는 슬롯은 UNKNOWN으로
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i) == null) {
                    out.set(i, AiVerificationResponse.ofUnknown("응답에 누락"));
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("Failed to parse batch verification response: {}", responseText, e);
            return fillUnknown(requests.size(), "파싱 실패");
        }
    }

    private List<AiVerificationResponse> fillUnknown(int n, String reason) {
        List<AiVerificationResponse> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(AiVerificationResponse.ofUnknown(reason));
        return out;
    }

    /**
     * 거절된 문제를 LLM에게 수정 요청.
     * questionType별로 다른 system prompt + user prompt를 사용 (MCQ / SHORT_ANSWER / DESCRIPTIVE).
     */
    public GeneratedQuestion fixQuestion(GeneratedQuestion question, String reason,
                                         com.sqldpass.persistent.mockexam.ExamType examType,
                                         com.sqldpass.persistent.question.QuestionType questionType) {
        String systemPrompt;
        String userPrompt;
        switch (questionType) {
            case SHORT_ANSWER -> {
                systemPrompt = PromptBuilder.SHORT_ANSWER_FIX_SYSTEM_PROMPT;
                userPrompt = PromptBuilder.buildShortAnswerFixPrompt(question, reason);
            }
            case DESCRIPTIVE -> {
                systemPrompt = PromptBuilder.DESCRIPTIVE_FIX_SYSTEM_PROMPT;
                userPrompt = PromptBuilder.buildDescriptiveFixPrompt(question, reason);
            }
            default -> {
                systemPrompt = PromptBuilder.MCQ_FIX_SYSTEM_PROMPT;
                userPrompt = PromptBuilder.buildFixPrompt(question, reason);
            }
        }

        String responseText = safeCall(systemPrompt, userPrompt);
        if (responseText == null || responseText.isBlank()) {
            log.warn("Fix empty response (type={})", questionType);
            return null;
        }

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            if (root.has("fixable") && !root.get("fixable").asBoolean()) {
                return null;
            }
            return objectMapper.readValue(json, GeneratedQuestion.class);
        } catch (Exception e) {
            log.warn("Failed to parse fix response: {}", responseText, e);
            return null;
        }
    }

    /** 호환용 — 기존 호출자를 위해 유지. SQLD MCQ로 가정. */
    public GeneratedQuestion fixQuestion(GeneratedQuestion question, String reason) {
        return fixQuestion(question, reason,
                com.sqldpass.persistent.mockexam.ExamType.SQLD,
                com.sqldpass.persistent.question.QuestionType.MCQ);
    }

    /** Spring AI ChatClient의 빈 응답/예외를 한 곳에서 처리. */
    private String safeCall(String systemPrompt, String userPrompt) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("ChatClient call failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('[');
        int startObj = text.indexOf('{');
        if (start == -1 || (startObj != -1 && startObj < start)) {
            start = startObj;
        }
        int end = Math.max(text.lastIndexOf(']'), text.lastIndexOf('}'));
        if (start == -1 || end == -1) return text;
        return text.substring(start, end + 1);
    }
}
