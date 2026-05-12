package com.sqldpass.controller.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sqldpass.persistent.payment.PaymentProvider;
import com.sqldpass.persistent.payment.PaymentRepository;
import com.sqldpass.persistent.payment.PaymentStatus;
import com.sqldpass.persistent.payment.SubscriptionPlan;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.payment.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private PaymentRepository paymentRepository;

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
    @DisplayName("POST /api/admin/payments/{id}/refund — reason 빈 문자열이면 @NotBlank 위반으로 400")
    void refund_빈_reason_400() throws Exception {
        mockMvc.perform(post("/api/admin/payments/42/refund")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(paymentService, times(0)).revokePortOnePayment(any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/admin/payments/{id}/refund — reason 201자 초과면 @Size 위반으로 400")
    void refund_긴_reason_400() throws Exception {
        String longReason = "가".repeat(201);
        mockMvc.perform(post("/api/admin/payments/42/refund")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + longReason + "\"}"))
                .andExpect(status().isBadRequest());

        verify(paymentService, times(0)).revokePortOnePayment(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/admin/payments — 필터 없이 정상 200 + repository.findAdminPage 호출")
    void list_기본_페이지_정상() throws Exception {
        AdminPaymentRow row = new AdminPaymentRow(
                42L, "sqldpass-1700000000-7", 7L, "닉네임",
                SubscriptionPlan.THREE_DAY, 3900, 3900, 0,
                PaymentStatus.PAID, PaymentProvider.PORTONE,
                "홍길동", "buyer@example.com", "01012345678",
                LocalDateTime.of(2026, 5, 12, 10, 0), LocalDateTime.of(2026, 5, 12, 9, 59));
        Page<AdminPaymentRow> page = new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);
        given(paymentRepository.findAdminPage(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].nickname").value("닉네임"))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/payments — status=PAID 필터가 repository 에 그대로 전달")
    void list_status_필터_전달() throws Exception {
        given(paymentRepository.findAdminPage(eq(PaymentStatus.PAID), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/admin/payments")
                        .param("status", "PAID")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L))
                .andExpect(status().isOk());

        verify(paymentRepository, times(1))
                .findAdminPage(eq(PaymentStatus.PAID), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/admin/payments — nickname/paymentId 파라미터 trim 후 repository 전달, 빈문자열은 null 로")
    void list_nickname_paymentId_trim_및_빈문자열_null() throws Exception {
        given(paymentRepository.findAdminPage(any(), any(), eq("홍길동"), eq("sqldpass-1"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        given(paymentRepository.findAdminPage(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // trim
        mockMvc.perform(get("/api/admin/payments")
                        .param("nickname", "  홍길동  ")
                        .param("paymentId", "  sqldpass-1  ")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L))
                .andExpect(status().isOk());

        verify(paymentRepository, times(1))
                .findAdminPage(isNull(), isNull(), eq("홍길동"), eq("sqldpass-1"), any(Pageable.class));

        // 빈 문자열은 null 로 정규화
        mockMvc.perform(get("/api/admin/payments")
                        .param("nickname", "")
                        .param("paymentId", "   ")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L))
                .andExpect(status().isOk());

        verify(paymentRepository, times(1))
                .findAdminPage(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/admin/payments — size 100 초과 요청은 100 으로 강제, 음수 page 는 0 으로")
    void list_size_상한_및_음수_page_보정() throws Exception {
        given(paymentRepository.findAdminPage(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/api/admin/payments")
                        .param("size", "999")
                        .param("page", "-5")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("memberId", 7L))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository, times(1))
                .findAdminPage(any(), any(), any(), any(), captor.capture());
        Pageable pageable = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(pageable.getPageSize()).isEqualTo(100);
        org.assertj.core.api.Assertions.assertThat(pageable.getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/admin/payments — memberId attribute 없으면 401")
    void list_미인증_시_401() throws Exception {
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isUnauthorized());

        verify(paymentRepository, times(0)).findAdminPage(any(), any(), any(), any(), any(Pageable.class));
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
