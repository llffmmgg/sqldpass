package com.sqldpass.controller.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.apple.itunes.storekit.model.Data;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.NotificationTypeV2;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.auth.GoogleIdTokenVerifier;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.notification.DiscordNotifier;
import com.sqldpass.service.payment.AppStorePayloadVerificationException;
import com.sqldpass.service.payment.AppStorePayloadVerifier;
import com.sqldpass.service.payment.PaymentService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentWebhookController controller;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private GoogleIdTokenVerifier idTokenVerifier;

    @MockitoBean
    private AppStorePayloadVerifier appStorePayloadVerifier;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private DiscordNotifier discordNotifier;

    private static final String EXPECTED_AUDIENCE = "https://api.sqldpass.com/api/webhook/play-billing/rtdn";

    private String envelopeJson(int notificationType, String purchaseToken) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode oneTime = mapper.createObjectNode();
        oneTime.put("version", "1.0");
        oneTime.put("notificationType", notificationType);
        oneTime.put("purchaseToken", purchaseToken);
        oneTime.put("sku", "iap_one_month");
        ObjectNode root = mapper.createObjectNode();
        root.put("packageName", "com.sqldpass.app");
        root.set("oneTimeProductNotification", oneTime);
        String dataB64 = Base64.getEncoder().encodeToString(
                mapper.writeValueAsString(root).getBytes(StandardCharsets.UTF_8));
        ObjectNode message = mapper.createObjectNode();
        message.put("data", dataB64);
        message.put("messageId", "msg-1");
        message.put("publishTime", "2026-05-11T00:00:00Z");
        ObjectNode envelope = mapper.createObjectNode();
        envelope.set("message", message);
        envelope.put("subscription", "projects/sqldpass/subscriptions/play-billing-rtdn");
        return mapper.writeValueAsString(envelope);
    }

    /** subscriptionNotification 변형 — 일회성 결제 알림 대신 구독 알림 (notificationType 12 = REVOKED). */
    private String subscriptionEnvelopeJson(int notificationType, String purchaseToken) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode sub = mapper.createObjectNode();
        sub.put("version", "1.0");
        sub.put("notificationType", notificationType);
        sub.put("purchaseToken", purchaseToken);
        sub.put("subscriptionId", "iap_one_month");
        ObjectNode root = mapper.createObjectNode();
        root.put("packageName", "com.sqldpass.app");
        root.set("subscriptionNotification", sub);
        String dataB64 = Base64.getEncoder().encodeToString(
                mapper.writeValueAsString(root).getBytes(StandardCharsets.UTF_8));
        ObjectNode message = mapper.createObjectNode();
        message.put("data", dataB64);
        message.put("messageId", "msg-sub");
        message.put("publishTime", "2026-05-11T00:00:00Z");
        ObjectNode envelope = mapper.createObjectNode();
        envelope.set("message", message);
        envelope.put("subscription", "projects/sqldpass/subscriptions/play-billing-rtdn");
        return mapper.writeValueAsString(envelope);
    }

    /**
     * 검증 mock 으로 사용할 ASSN v2 페이로드 stub.
     * 실제 JWS 가 아닌 더미 token 을 컨트롤러에 보내고, verifier mock 이 이 객체를 반환하도록 stub.
     */
    private ResponseBodyV2DecodedPayload buildDecodedPayload(String notificationType, String signedTxInfoStub) {
        ResponseBodyV2DecodedPayload payload = new ResponseBodyV2DecodedPayload();
        // NotificationTypeV2.fromValue 를 raw 값으로 set — 알 수 없는 값(SUBSCRIBED 등 enum 매핑 안 되는 케이스 포함).
        payload.setRawNotificationType(notificationType);
        Data data = new Data();
        data.setSignedTransactionInfo(signedTxInfoStub);
        payload.setData(data);
        return payload;
    }

    private JWSTransactionDecodedPayload buildDecodedTransaction(String transactionId) {
        JWSTransactionDecodedPayload tx = new JWSTransactionDecodedPayload();
        tx.setTransactionId(transactionId);
        tx.setOriginalTransactionId(transactionId);
        return tx;
    }

    private String appStoreBodyJson(String signedPayload) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("signedPayload", signedPayload);
        return mapper.writeValueAsString(body);
    }

    @BeforeEach
    void resetWebhookConfig() {
        ReflectionTestUtils.setField(controller, "rtdnSharedSecret", "");
        ReflectionTestUtils.setField(controller, "rtdnOidcAudience", "");
    }

    @Test
    @DisplayName("RTDN OIDC 정상 aud/iss 시 payload 처리 200")
    void rtdn_oidc_success() throws Exception {
        ReflectionTestUtils.setField(controller, "rtdnOidcAudience", EXPECTED_AUDIENCE);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode claims = mapper.createObjectNode();
        claims.put("iss", "https://accounts.google.com");
        claims.put("aud", EXPECTED_AUDIENCE);
        given(idTokenVerifier.verify(eq("Bearer real-jwt"), eq(EXPECTED_AUDIENCE))).willReturn(claims);
        given(paymentService.revokePlayBillingByToken("token-abcdef1234")).willReturn(true);

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .header("Authorization", "Bearer real-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(2, "token-abcdef1234")))
                .andExpect(status().isOk());

        verify(paymentService).revokePlayBillingByToken("token-abcdef1234");
    }

    @Test
    @DisplayName("RTDN OIDC aud 불일치시 401")
    void rtdn_oidc_aud_mismatch() throws Exception {
        ReflectionTestUtils.setField(controller, "rtdnOidcAudience", EXPECTED_AUDIENCE);
        given(idTokenVerifier.verify(eq("Bearer wrong-aud-jwt"), eq(EXPECTED_AUDIENCE)))
                .willThrow(new SqldpassException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .header("Authorization", "Bearer wrong-aud-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(2, "token-abcdef1234")))
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }

    @Test
    @DisplayName("RTDN OIDC iss 불일치시 401")
    void rtdn_oidc_iss_mismatch() throws Exception {
        ReflectionTestUtils.setField(controller, "rtdnOidcAudience", EXPECTED_AUDIENCE);
        given(idTokenVerifier.verify(eq("Bearer bad-iss-jwt"), eq(EXPECTED_AUDIENCE)))
                .willThrow(new SqldpassException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .header("Authorization", "Bearer bad-iss-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(2, "token-abcdef1234")))
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }

    @Test
    @DisplayName("authHeader 없고 secret 불일치시 401")
    void rtdn_secret_mismatch() throws Exception {
        ReflectionTestUtils.setField(controller, "rtdnSharedSecret", "expected-secret");

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .param("token", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(2, "token-abcdef1234")))
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }

    @Test
    @DisplayName("authHeader 없고 secret 일치시 200 (fallback)")
    void rtdn_secret_fallback() throws Exception {
        ReflectionTestUtils.setField(controller, "rtdnSharedSecret", "expected-secret");
        given(paymentService.revokePlayBillingByToken("token-abcdef1234")).willReturn(true);

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .param("token", "expected-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(2, "token-abcdef1234")))
                .andExpect(status().isOk());

        verify(paymentService).revokePlayBillingByToken("token-abcdef1234");
    }

    @Test
    @DisplayName("OIDC audience·secret 둘 다 미설정 시 dev 모드 pass 200")
    void rtdn_dev_mode_pass() throws Exception {
        // 두 설정 다 빈 문자열 — @BeforeEach 에서 reset 됨.
        given(paymentService.revokePlayBillingByToken("token-abcdef1234")).willReturn(true);

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(2, "token-abcdef1234")))
                .andExpect(status().isOk());

        verify(paymentService).revokePlayBillingByToken("token-abcdef1234");
    }

    @Test
    @DisplayName("RTDN 같은 purchaseToken 중복 수신 시 두 번 모두 200 + controller 는 매번 service 호출 (idempotency 는 service 레벨)")
    void rtdn_duplicate_envelope_both_200_controller_calls_each_time() throws Exception {
        given(paymentService.revokePlayBillingByToken("tok-x")).willReturn(true);
        String body = envelopeJson(2, "tok-x");

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(paymentService, times(2)).revokePlayBillingByToken("tok-x");
    }

    @Test
    @DisplayName("RTDN oneTimeProductNotification type=1 (PURCHASED) 정보성 알림은 revoke 호출 0회 + 200")
    void rtdn_oneTime_type_1_purchased_no_revoke() throws Exception {
        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelopeJson(1, "token-abcdef1234")))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }

    @Test
    @DisplayName("RTDN subscriptionNotification type=12 (REVOKED) → revokePlayBillingByToken 호출")
    void rtdn_subscription_revoked_type_12_calls_revoke() throws Exception {
        given(paymentService.revokePlayBillingByToken("sub-tok-12")).willReturn(true);

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionEnvelopeJson(12, "sub-tok-12")))
                .andExpect(status().isOk());

        verify(paymentService).revokePlayBillingByToken("sub-tok-12");
    }

    @Test
    @DisplayName("RTDN subscriptionNotification type=2 (RENEWED) Non-Renewing 모델 무시 → revoke 0회")
    void rtdn_subscription_renewed_type_2_ignored() throws Exception {
        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionEnvelopeJson(2, "sub-tok-2")))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }

    @Test
    @DisplayName("RTDN subscriptionNotification type=3 (CANCELED) 무시 → revoke 0회")
    void rtdn_subscription_canceled_type_3_ignored() throws Exception {
        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionEnvelopeJson(3, "sub-tok-3")))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }

    @Test
    @DisplayName("App Store REFUND → verifier 가 검증된 페이로드 반환 시 revokeAppStoreByTransactionId 호출 + 200")
    void appstore_refund_calls_revoke() throws Exception {
        given(appStorePayloadVerifier.verifyAndDecodeNotification(anyString()))
                .willReturn(buildDecodedPayload(NotificationTypeV2.REFUND.name(), "stub-signed-tx"));
        given(appStorePayloadVerifier.verifyAndDecodeTransaction(eq("stub-signed-tx")))
                .willReturn(buildDecodedTransaction("tx-refund-1"));
        given(paymentService.revokeAppStoreByTransactionId("tx-refund-1")).willReturn(true);

        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appStoreBodyJson("dummy.jws.payload")))
                .andExpect(status().isOk());

        verify(paymentService).revokeAppStoreByTransactionId("tx-refund-1");
    }

    @Test
    @DisplayName("App Store REVOKE → verifier 가 검증된 페이로드 반환 시 revokeAppStoreByTransactionId 호출 + 200")
    void appstore_revoke_calls_revoke() throws Exception {
        given(appStorePayloadVerifier.verifyAndDecodeNotification(anyString()))
                .willReturn(buildDecodedPayload(NotificationTypeV2.REVOKE.name(), "stub-signed-tx"));
        given(appStorePayloadVerifier.verifyAndDecodeTransaction(eq("stub-signed-tx")))
                .willReturn(buildDecodedTransaction("tx-revoke-2"));
        given(paymentService.revokeAppStoreByTransactionId("tx-revoke-2")).willReturn(true);

        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appStoreBodyJson("dummy.jws.payload")))
                .andExpect(status().isOk());

        verify(paymentService).revokeAppStoreByTransactionId("tx-revoke-2");
    }

    @Test
    @DisplayName("App Store DID_RENEW 무시 → revoke 0회")
    void appstore_did_renew_ignored() throws Exception {
        given(appStorePayloadVerifier.verifyAndDecodeNotification(anyString()))
                .willReturn(buildDecodedPayload(NotificationTypeV2.DID_RENEW.name(), "stub-signed-tx"));

        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appStoreBodyJson("dummy.jws.payload")))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokeAppStoreByTransactionId(any());
    }

    @Test
    @DisplayName("App Store SUBSCRIBED 무시 → revoke 0회")
    void appstore_subscribed_ignored() throws Exception {
        given(appStorePayloadVerifier.verifyAndDecodeNotification(anyString()))
                .willReturn(buildDecodedPayload(NotificationTypeV2.SUBSCRIBED.name(), "stub-signed-tx"));

        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appStoreBodyJson("dummy.jws.payload")))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokeAppStoreByTransactionId(any());
    }

    @Test
    @DisplayName("App Store EXPIRED 무시 (Non-Renewing 정책상 만료 자동 처리) → revoke 0회")
    void appstore_expired_ignored() throws Exception {
        given(appStorePayloadVerifier.verifyAndDecodeNotification(anyString()))
                .willReturn(buildDecodedPayload(NotificationTypeV2.EXPIRED.name(), "stub-signed-tx"));

        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appStoreBodyJson("dummy.jws.payload")))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokeAppStoreByTransactionId(any());
    }

    @Test
    @DisplayName("App Store signedPayload 빈 문자열 → ignored + revoke 0회")
    void appstore_empty_payload_ignored() throws Exception {
        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signedPayload\":\"\"}"))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokeAppStoreByTransactionId(any());
    }

    @Test
    @DisplayName("App Store JWS 검증 실패 시 401 + revoke 0회 (위조 webhook 차단)")
    void appstore_verification_failure_returns_401() throws Exception {
        given(appStorePayloadVerifier.verifyAndDecodeNotification(anyString()))
                .willThrow(new AppStorePayloadVerificationException("invalid x5c chain"));

        mockMvc.perform(post("/api/webhook/app-store/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appStoreBodyJson("forged.jws.payload")))
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).revokeAppStoreByTransactionId(any());
    }

    @Test
    @DisplayName("RTDN payload base64/JSON 파싱 실패해도 Pub/Sub retry 회피를 위해 200 반환 + revoke 0회")
    void rtdn_invalid_payload_still_200_no_revoke() throws Exception {
        // 유효한 base64 지만 디코드 결과가 JSON 이 아님 → objectMapper.readTree 가 throw.
        String invalidDataB64 = Base64.getEncoder().encodeToString(
                "not-a-json {{".getBytes(StandardCharsets.UTF_8));
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode message = mapper.createObjectNode();
        message.put("data", invalidDataB64);
        message.put("messageId", "msg-broken");
        message.put("publishTime", "2026-05-11T00:00:00Z");
        ObjectNode envelope = mapper.createObjectNode();
        envelope.set("message", message);
        envelope.put("subscription", "projects/sqldpass/subscriptions/play-billing-rtdn");

        mockMvc.perform(post("/api/webhook/play-billing/rtdn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(envelope)))
                .andExpect(status().isOk());

        verify(paymentService, never()).revokePlayBillingByToken(any());
    }
}
