package com.sqldpass.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sqldpass.service.generation.AiProvider;

@Configuration
public class AiClientConfig {

    @Bean
    @Qualifier("generator")
    public AiProvider generator(
            @Value("${sqldpass.ai.generator.provider}") String provider,
            AnthropicChatModel anthropicModel,
            GoogleGenAiChatModel geminiModel,
            OpenAiChatModel openAiModel) {
        ChatClient client = ChatClient.builder(selectModel(provider, anthropicModel, geminiModel, openAiModel)).build();
        return new AiProvider(client);
    }

    @Bean
    @Qualifier("verifier")
    public AiProvider verifier(
            @Value("${sqldpass.ai.verifier.provider}") String provider,
            AnthropicChatModel anthropicModel,
            GoogleGenAiChatModel geminiModel,
            OpenAiChatModel openAiModel) {
        ChatClient client = ChatClient.builder(selectModel(provider, anthropicModel, geminiModel, openAiModel)).build();
        return new AiProvider(client);
    }

    private ChatModel selectModel(String provider, AnthropicChatModel anthropicModel,
                                  GoogleGenAiChatModel geminiModel, OpenAiChatModel openAiModel) {
        return switch (provider) {
            case "claude" -> anthropicModel;
            case "gemini" -> geminiModel;
            case "openai" -> openAiModel;
            default -> throw new IllegalArgumentException("Unknown AI provider: " + provider);
        };
    }
}
