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

    public AiVerificationResponse verifyQuestion(AiVerificationRequest request) {
        String prompt = PromptBuilder.buildVerificationPrompt(request);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.buildVerificationSystemPrompt(request))
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            boolean approved = root.get("approved").asBoolean();
            String reason = root.has("reason") ? root.get("reason").asText() : "";
            return new AiVerificationResponse(approved, reason);
        } catch (Exception e) {
            log.error("Failed to parse verification response: {}", responseText, e);
            return new AiVerificationResponse(false, "파싱 실패");
        }
    }

    public GeneratedQuestion fixQuestion(GeneratedQuestion question, String reason) {
        String prompt = PromptBuilder.buildFixPrompt(question, reason);
        String responseText = chatClient.prompt()
                .system(PromptBuilder.FIX_SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        try {
            String json = extractJson(responseText);
            JsonNode root = objectMapper.readTree(json);
            if (root.has("fixable") && !root.get("fixable").asBoolean()) {
                return null;
            }
            return objectMapper.readValue(json, GeneratedQuestion.class);
        } catch (Exception e) {
            log.error("Failed to parse fix response: {}", responseText, e);
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
