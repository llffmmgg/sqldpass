package com.sqldpass.controller.admin;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.payment.PaymentService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPaymentController.class)
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private static final String AUTH_HEADER = "Bearer admin-token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("admin-token")).willReturn(true);
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/refund — 정상 200 + paymentService.revokePortOnePayment(paymentId, reason, actorAdminId) 호출")
    void refund_정상_200_및_service_호출_검증() throws Exception {
        mockMvc.perform(post("/api/admin/payments/42/refund")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"고객 요청\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(42))
                .andExpect(jsonPath("$.status").value("refunded"));

        verify(paymentService, times(1))
                .revokePortOnePayment(eq(42L), eq("고객 요청"), eq(7L));
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/refund — request attribute memberId 없으면 401")
    void refund_미인증_시_401() throws Exception {
        mockMvc.perform(post("/api/admin/payments/42/refund")
                        .header("Authorization", AUTH_HEADER)
                        // memberId attribute 의도적으로 누락.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isUnauthorized());

        verify(paymentService, times(0))
                .revokePortOnePayment(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/refund — body 없어도 reason=null 로 service 호출")
    void refund_body_없어도_reason_null_로_service_호출() throws Exception {
        mockMvc.perform(post("/api/admin/payments/42/refund")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(42))
                .andExpect(jsonPath("$.status").value("refunded"));

        verify(paymentService, times(1))
                .revokePortOnePayment(eq(42L), eq(null), eq(7L));
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/reissue-subscription — 정상 200 + service.reissueSubscription 1회 + 응답 매핑")
    void reissue_정상_200_및_service_호출() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        given(paymentService.reissueSubscription(eq(42L), eq(7L)))
                .willReturn(new PaymentService.ReissueResult(true, expiresAt));

        mockMvc.perform(post("/api/admin/payments/42/reissue-subscription")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"운영자 수동 복구\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(42))
                .andExpect(jsonPath("$.issued").value(true))
                .andExpect(jsonPath("$.expiresAt").exists());

        verify(paymentService, times(1)).reissueSubscription(eq(42L), eq(7L));
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/reissue-subscription — 미인증 (memberId attribute 없음) 시 401")
    void reissue_미인증_시_401() throws Exception {
        mockMvc.perform(post("/api/admin/payments/42/reissue-subscription")
                        .header("Authorization", AUTH_HEADER)
                        // memberId attribute 의도적으로 누락.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verify(paymentService, times(0)).reissueSubscription(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/reissue-subscription — service 가 INVALID_INPUT throw 시 400 매핑")
    void reissue_service_가_INVALID_INPUT_throw_시_400() throws Exception {
        given(paymentService.reissueSubscription(eq(42L), eq(7L)))
                .willThrow(new SqldpassException(ErrorCode.INVALID_INPUT,
                        "PAID 상태인 결제만 재발급 대상입니다."));

        mockMvc.perform(post("/api/admin/payments/42/reissue-subscription")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(paymentService, times(1)).reissueSubscription(eq(42L), eq(7L));
    }
}
