package com.sqldpass.controller.usage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.controller.mockexam.MockExamController;
import com.sqldpass.controller.question.QuestionController;
import com.sqldpass.domain.mockexam.MockExam;
import com.sqldpass.domain.question.Question;
import com.sqldpass.persistent.member.MemberRepository;
import com.sqldpass.persistent.mockexam.EngineerExamTemplate;
import com.sqldpass.persistent.mockexam.ExamType;
import com.sqldpass.persistent.mockexam.MockExamKind;
import com.sqldpass.persistent.mockexam.MockExamVisibility;
import com.sqldpass.persistent.payment.MockExamPurchaseRepository;
import com.sqldpass.persistent.solve.SolveRepository;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.mockexam.MockExamService;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.payment.PaymentProperties;
import com.sqldpass.service.payment.SubscriptionService;
import com.sqldpass.service.pdf.MockExamPdfService;
import com.sqldpass.service.question.QuestionService;
import com.sqldpass.service.usage.DailyUsageService;
import com.sqldpass.service.usage.QuotaExceededException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step 5 통합 테스트 — 컨트롤러 가드 + quota envelope 검증.
 * WebMvc 슬라이스로 Question/MockExam/Quota 세 컨트롤러를 함께 로드.
 *
 * 시나리오:
 * 1. /api/questions: 한도 초과 시 402 + DAILY_QUESTION_LIMIT
 * 2. /api/mock-exams/{정규모의ID}: 2번째 호출에 402 + DAILY_MOCK_LIMIT
 * 3. /api/mock-exams/{기출ID}: PAST_EXAM 은 가드 면제 — 여러 번 호출해도 200
 * 5. 활성 구독자: 가드는 service 안에서 면제(no-op) — 컨트롤러는 모두 200
 */
@WebMvcTest(controllers = {QuestionController.class, MockExamController.class, QuotaController.class})
class QuotaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private MockExamService mockExamService;

    @MockitoBean
    private SolveRepository solveRepository;

    @MockitoBean
    private MockExamPurchaseRepository mockExamPurchaseRepository;

    @MockitoBean
    private MockExamPdfService mockExamPdfService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private PaymentProperties paymentProperties;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private DailyUsageService dailyUsageService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private static final String AUTH_HEADER = "Bearer test-token";
    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.extractMemberId("test-token")).willReturn(MEMBER_ID);
    }

    @Test
    @DisplayName("[1] GET /api/questions — 한도 초과 시 402 DAILY_QUESTION_LIMIT")
    void questions_over_limit_returns_402() throws Exception {
        doThrow(new QuotaExceededException(
                "DAILY_QUESTION_LIMIT", 30, 30,
                LocalDateTime.of(2026, 5, 22, 0, 0, 0)))
                .when(dailyUsageService).consumeQuestion(eq(MEMBER_ID), anyInt());
        given(questionService.getRandomQuestions(eq(5L), eq(MEMBER_ID), eq(20)))
                .willReturn(List.of(new Question(1L, 5L, "Q", 1, "E")));

        mockMvc.perform(get("/api/questions")
                        .param("subjectId", "5")
                        .param("size", "20")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("DAILY_QUESTION_LIMIT"))
                .andExpect(jsonPath("$.used").value(30))
                .andExpect(jsonPath("$.limit").value(30))
                .andExpect(jsonPath("$.resetAt").value("2026-05-22T00:00:00"));
    }

    @Test
    @DisplayName("[1-ok] GET /api/questions — 한도 내면 200, consumeQuestion 호출 검증")
    void questions_under_limit_calls_consume() throws Exception {
        doNothing().when(dailyUsageService).consumeQuestion(eq(MEMBER_ID), anyInt());
        Question q = new Question(1L, 5L, "문제", 2, "해설");
        given(questionService.getRandomQuestions(eq(5L), eq(MEMBER_ID), eq(20)))
                .willReturn(List.of(q));

        mockMvc.perform(get("/api/questions")
                        .param("subjectId", "5")
                        .param("size", "20")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());

        verify(dailyUsageService).consumeQuestion(MEMBER_ID, 1);
    }

    @Test
    @DisplayName("[2-detail] GET /api/mock-exams/{id} — 상세 조회는 consumeMockSession 미호출")
    void mockexam_detail_does_not_consume() throws Exception {
        MockExam mock = aiMockExam(100L);
        given(mockExamService.getForUser(eq(100L), eq(MEMBER_ID))).willReturn(mock);

        mockMvc.perform(get("/api/mock-exams/100").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());

        verify(dailyUsageService, never()).consumeMockSession(anyLong());
    }

    @Test
    @DisplayName("[2-start] POST /api/mock-exams/{id}/start — consumeMockSession 호출(정규 회차)")
    void mockexam_start_regular_calls_consume() throws Exception {
        MockExam mock = aiMockExam(100L);
        given(mockExamService.getForUser(eq(100L), eq(MEMBER_ID))).willReturn(mock);
        doNothing().when(dailyUsageService).consumeMockSession(MEMBER_ID);

        mockMvc.perform(post("/api/mock-exams/100/start").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());

        verify(dailyUsageService).consumeMockSession(MEMBER_ID);
    }

    @Test
    @DisplayName("[2-over] POST /api/mock-exams/{id}/start — 한도 초과 시 402 DAILY_MOCK_LIMIT")
    void mockexam_over_limit_returns_402() throws Exception {
        MockExam mock = aiMockExam(101L);
        given(mockExamService.getForUser(eq(101L), eq(MEMBER_ID))).willReturn(mock);
        doThrow(new QuotaExceededException(
                "DAILY_MOCK_LIMIT", 1, 1,
                LocalDateTime.of(2026, 5, 22, 0, 0, 0)))
                .when(dailyUsageService).consumeMockSession(MEMBER_ID);

        mockMvc.perform(post("/api/mock-exams/101/start").header("Authorization", AUTH_HEADER))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("DAILY_MOCK_LIMIT"))
                .andExpect(jsonPath("$.used").value(1))
                .andExpect(jsonPath("$.limit").value(1));
    }

    @Test
    @DisplayName("[3] GET /api/mock-exams/{id} — 기출(PAST_EXAM)은 가드 면제 (consumeMockSession 미호출)")
    void mockexam_past_exam_is_exempt() throws Exception {
        MockExam past = pastExamMockExam(200L);
        given(mockExamService.getForUser(eq(200L), eq(MEMBER_ID))).willReturn(past);

        mockMvc.perform(get("/api/mock-exams/200").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());

        verify(dailyUsageService, never()).consumeMockSession(anyLong());
    }

    @Test
    @DisplayName("[5] 활성 구독자 시뮬레이션 — service 안에서 no-op (가드 호출은 되지만 예외 없이 통과)")
    void subscriber_passes_all_guards() throws Exception {
        // 구독자: consumeQuestion / consumeMockSession 모두 no-op (실제 service 가 면제 처리하는 동작을 mock 으로 흉내)
        doNothing().when(dailyUsageService).consumeQuestion(eq(MEMBER_ID), anyInt());
        doNothing().when(dailyUsageService).consumeMockSession(MEMBER_ID);

        Question q = new Question(1L, 5L, "Q", 1, "E");
        given(questionService.getRandomQuestions(any(), any(), anyInt())).willReturn(List.of(q));

        MockExam mock = aiMockExam(300L);
        given(mockExamService.getForUser(eq(300L), eq(MEMBER_ID))).willReturn(mock);

        // 호출 100번 시뮬: 모두 200
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/questions").param("subjectId", "5")
                            .header("Authorization", AUTH_HEADER))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/mock-exams/300").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[6] GET /api/quota — 무료 회원 사용량 응답")
    void quota_free_member() throws Exception {
        given(dailyUsageService.getQuota(MEMBER_ID)).willReturn(new DailyUsageService.Quota(
                18, DailyUsageService.DAILY_QUESTION_LIMIT,
                0, DailyUsageService.DAILY_MOCK_SESSION_LIMIT,
                LocalDateTime.of(2026, 5, 22, 0, 0, 0)));

        mockMvc.perform(get("/api/quota").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionUsed").value(18))
                .andExpect(jsonPath("$.questionLimit").value(30))
                .andExpect(jsonPath("$.resetAt").value("2026-05-22T00:00:00"));
    }

    @Test
    @DisplayName("[7] GET /api/quota — 구독자 limit=null")
    void quota_subscriber() throws Exception {
        given(dailyUsageService.getQuota(MEMBER_ID)).willReturn(
                DailyUsageService.Quota.unlimited(LocalDateTime.of(2026, 5, 22, 0, 0, 0)));

        mockMvc.perform(get("/api/quota").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionLimit").doesNotExist())
                .andExpect(jsonPath("$.mockLimit").doesNotExist());
    }

    // ---- helpers ----

    private MockExam aiMockExam(Long id) {
        return new MockExam(
                id, "AI 모의 #" + id, ExamType.SQLD, 1,
                LocalDateTime.now(),
                /* questions */ List.of(),
                (EngineerExamTemplate) null,
                MockExamVisibility.PUBLISHED, /* expertVerified */ true,
                MockExamKind.AI, null, null, null, null, null);
    }

    private MockExam pastExamMockExam(Long id) {
        return new MockExam(
                id, "기출 #" + id, ExamType.SQLD, 1,
                LocalDateTime.now(),
                /* questions */ List.of(),
                (EngineerExamTemplate) null,
                MockExamVisibility.PUBLISHED, /* expertVerified */ true,
                MockExamKind.PAST_EXAM, 2024, 56, null, null, null);
    }

}
