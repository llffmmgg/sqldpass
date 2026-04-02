package com.sqldpass.service.generation;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiProvider {

    private static final Logger log = LoggerFactory.getLogger(AiProvider.class);

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
