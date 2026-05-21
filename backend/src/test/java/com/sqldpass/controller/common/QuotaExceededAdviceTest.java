package com.sqldpass.controller.common;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.usage.QuotaExceededException;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc 로 GlobalExceptionHandler 의 QuotaExceededException → 402 변환 검증.
 * 가짜 컨트롤러가 예외를 throw 하면 advice 가 받아서 응답 body 4필드 + 상태 코드 402 로 직렬화.
 */
class QuotaExceededAdviceTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        objectMapper.disable(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(mock(DiscordNotifier.class)))
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("DAILY_QUESTION_LIMIT 예외 → 402 + body { error, used, limit, resetAt }")
    void question_limit_returns_402() throws Exception {
        mockMvc.perform(get("/test/quota-question").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("DAILY_QUESTION_LIMIT"))
                .andExpect(jsonPath("$.used").value(30))
                .andExpect(jsonPath("$.limit").value(30))
                // KST naive 직렬화 — Z 없음, +09:00 없음, "2026-05-22T00:00:00" 형식
                .andExpect(jsonPath("$.resetAt").value("2026-05-22T00:00:00"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("DAILY_MOCK_LIMIT 예외 → 402 + DAILY_MOCK_LIMIT 코드")
    void mock_limit_returns_402() throws Exception {
        mockMvc.perform(get("/test/quota-mock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("DAILY_MOCK_LIMIT"))
                .andExpect(jsonPath("$.used").value(2))
                .andExpect(jsonPath("$.limit").value(1));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/quota-question")
        public void throwQuestion() {
            throw new QuotaExceededException(
                    "DAILY_QUESTION_LIMIT", 30, 30,
                    LocalDateTime.of(2026, 5, 22, 0, 0, 0));
        }

        @GetMapping("/test/quota-mock")
        public void throwMock() {
            throw new QuotaExceededException(
                    "DAILY_MOCK_LIMIT", 2, 1,
                    LocalDateTime.of(2026, 5, 22, 0, 0, 0));
        }
    }
}
