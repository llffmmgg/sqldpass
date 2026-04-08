package com.sqldpass.service.generation;

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
     * - examples: needed개의 서로 다른 시드 (카테고리 풀에서 추출됨)
     * - targetDifficulties: 각 문제의 목표 난이도(1/2/3) — examples와 동일 길이
     * - forbiddenIdentifiers: 절대 사용 금지 식별자 (시드 + 누적 출제 식별자)
     * - recentAnswers: 회피해야 할 정답 패턴
     */
    public AiGenerationResponse generateEngineerQuestions(AiGenerationRequest request,
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
            // 정처기는 "한 카테고리 실패 = 모의고사 전체 실패"이므로 예외를 삼키지 않고 상위로 전파.
            // AI_GENERATION_FAILED(500)로 래핑 → GlobalExceptionHandler가 Discord 알림 전송.
            log.error("Failed to parse engineer generation response: {}", responseText, e);
            throw new SqldpassException(ErrorCode.AI_GENERATION_FAILED,
                    "정처기 AI 응답 파싱 실패 [" + request.subjectName() + "]: " + e.getMessage(), e);
        }
    }

    /**
     * 컴활 1급 필기 카테고리용 변형 문제 N개 생성 (시드 풀 + 사용자 지정 난이도).
     */
    public AiGenerationResponse generateComputerLiteracyQuestions(AiGenerationRequest request,
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
                .system(PromptBuilder.VERIFICATION_SYSTEM_PROMPT)
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
