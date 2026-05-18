package com.sqldpass.controller.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.NotificationTypeV2;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.sqldpass.controller.payment.dto.AppStoreNotificationRequest;
import com.sqldpass.service.auth.GoogleIdTokenVerifier;
import com.sqldpass.service.common.SqldpassException;
import com.sqldpass.service.payment.AppStorePayloadVerificationException;
import com.sqldpass.service.payment.AppStorePayloadVerifier;
import com.sqldpass.service.payment.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 결제 webhook 엔드포인트 — 인증 인터셉터 없이 외부에서 호출. (WebMvcConfig 에 등록 안 함)
 *
 * <p>Play Billing RTDN(Real-time Developer Notifications) 는 Google Pub/Sub push subscription 으로
 * 들어온다. payload 는 {@code message.data} 를 base64 디코드하면 packageName + oneTimeProductNotification
 * 또는 subscriptionNotification 이 들어 있다. 우리는 일회성 상품만 쓰니 oneTime 만 처리.
 *
 * <p>보안: Pub/Sub push subscription 의 OIDC ID token (Authorization Bearer) 을 우선 검증하고,
 * 미설정·미발급 환경에서는 URL {@code ?token=...} shared secret 로 fallback. 둘 다 미설정 시
 * dev 모드로 검증 스킵.
 *
 * <p>App Store Server Notifications V2 는 {@link AppStorePayloadVerifier} 가 JWS 서명·체인·bundleId 를
 * 검증한 뒤에만 dispatch — 검증 실패 시 401.
 */
@Slf4j
@Tag(name = "결제 webhook", description = "Play Billing RTDN 등 외부 결제 통지 처리")
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final GoogleIdTokenVerifier idTokenVerifier;
    private final AppStorePayloadVerifier appStorePayloadVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sqldpass.play-billing.rtdn-shared-secret:}")
    private String rtdnSharedSecret;

    @Value("${sqldpass.play-billing.rtdn-oidc-audience:}")
    private String rtdnOidcAudience;

    @PostMapping("/play-billing/rtdn")
    @Operation(summary = "Play Billing RTDN — 환불/재구매 통지를 받아 구독 상태 동기화")
    public ResponseEntity<Void> handleRtdn(@RequestBody RtdnEnvelope envelope,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @RequestParam(value = "token", required = false) String token) {
        if (!authenticateRtdn(authHeader, token)) {
            return ResponseEntity.status(401).build();
        }
        if (envelope == null || envelope.message() == null || envelope.message().data() == null) {
            // Pub/Sub 가 ack 받기 위해 200 을 기대 — 형식 오류여도 200 으로 끝낸다.
            return ResponseEntity.ok().build();
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(envelope.message().data()),
                    StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(decoded);

            // oneTimeProductNotification.notificationType
            // 1 = PURCHASED (정보성, verify 단계에서 이미 처리), 2 = CANCELED (환불) → revoke.
            JsonNode oneTime = root.get("oneTimeProductNotification");
            if (oneTime != null && !oneTime.isNull()) {
                int notificationType = oneTime.path("notificationType").asInt(0);
                String purchaseToken = oneTime.path("purchaseToken").asText(null);
                if (notificationType == 2 && purchaseToken != null && !purchaseToken.isBlank()) {
                    boolean revoked = paymentService.revokePlayBillingByToken(purchaseToken);
                    log.info("Play Billing RTDN one-time refund 처리 token={} revoked={}",
                            mask(purchaseToken), revoked);
                }
            }

            // subscriptionNotification.notificationType — Non-Renewing 모델이라 갱신/취소/만료는
            // 발생하지 않거나 무시 가능. 12 = SUBSCRIPTION_REVOKED (환불·차지백·정책 위반으로
            // entitlement 회수) 만 처리.
            JsonNode subNoti = root.get("subscriptionNotification");
            if (subNoti != null && !subNoti.isNull()) {
                int notificationType = subNoti.path("notificationType").asInt(0);
                String purchaseToken = subNoti.path("purchaseToken").asText(null);
                if (notificationType == 12 && purchaseToken != null && !purchaseToken.isBlank()) {
                    boolean revoked = paymentService.revokePlayBillingByToken(purchaseToken);
                    log.info("Play Billing RTDN subscription revoke 처리 token={} revoked={}",
                            mask(purchaseToken), revoked);
                } else {
                    log.info("Play Billing RTDN subscriptionNotification 무시 type={}", notificationType);
                }
            }
        } catch (Exception e) {
            log.error("RTDN payload 처리 실패", e);
        }
        // Pub/Sub 는 200 외 응답을 받으면 retry 하므로 우리 처리 실패와 무관하게 200 으로 ack.
        return ResponseEntity.ok().build();
    }

    private boolean authenticateRtdn(String authHeader, String token) {
        boolean oidcConfigured = rtdnOidcAudience != null && !rtdnOidcAudience.isBlank();
        boolean secretConfigured = rtdnSharedSecret != null && !rtdnSharedSecret.isBlank();

        // Authorization 헤더가 들어오면 OIDC 검증 우선.
        if (authHeader != null && oidcConfigured) {
            try {
                idTokenVerifier.verify(authHeader, rtdnOidcAudience);
                return true;
            } catch (SqldpassException e) {
                log.warn("RTDN OIDC 검증 실패");
                return false;
            }
        }

        // OIDC 미사용·헤더 없음 → shared-secret fallback.
        if (secretConfigured) {
            if (token != null && rtdnSharedSecret.equals(token)) {
                return true;
            }
            log.warn("RTDN shared-secret 불일치 — 무시");
            return false;
        }

        // dev 환경 — 둘 다 미설정 시 검증 스킵 (기존 동작 유지).
        return true;
    }

    private static String mask(String token) {
        if (token == null) return "null";
        return token.length() <= 8 ? "***" : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    @PostMapping("/app-store/notifications")
    @Operation(summary = "App Store Server Notifications V2 — 구독 갱신/만료/환불 비동기 통보")
    public ResponseEntity<Map<String, String>> handleAppStoreNotification(@RequestBody AppStoreNotificationRequest body) {
        if (body == null || body.signedPayload() == null || body.signedPayload().isBlank()) {
            log.warn("App Store notification 빈 signedPayload");
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        ResponseBodyV2DecodedPayload decoded;
        try {
            decoded = appStorePayloadVerifier.verifyAndDecodeNotification(body.signedPayload());
        } catch (AppStorePayloadVerificationException e) {
            log.warn("ASSN v2 검증 실패 — webhook reject: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        NotificationTypeV2 notificationTypeEnum = decoded.getNotificationType();
        String notificationType = notificationTypeEnum != null
                ? notificationTypeEnum.name()
                : decoded.getRawNotificationType();
        // Subtype 은 enum (UPGRADE/VOLUNTARY/...) — name() 으로 String 화. raw 값 fallback.
        String subtype = decoded.getSubtype() != null
                ? decoded.getSubtype().name()
                : decoded.getRawSubtype();

        log.info("App Store notification: type={} subtype={}", notificationType, subtype);

        // Non-Renewing 모델: SUBSCRIBED / DID_RENEW / EXPIRED / DID_FAIL_TO_RENEW /
        // GRACE_PERIOD_EXPIRED / OFFER_REDEEMED 는 우리 정책상 발생하지 않거나 무시 가능.
        // 환불·가족공유 회수만 entitlement 회수.
        if ("REFUND".equals(notificationType) || "REVOKE".equals(notificationType)) {
            String transactionId = extractAppStoreTransactionId(decoded);
            if (transactionId == null || transactionId.isBlank()) {
                log.warn("App Store {} — transactionId 추출 실패", notificationType);
            } else {
                boolean revoked = paymentService.revokeAppStoreByTransactionId(transactionId);
                log.info("App Store {} 처리 txId={} revoked={}",
                        notificationType, mask(transactionId), revoked);
            }
        } else {
            log.info("App Store notification 무시 type={}", notificationType);
        }

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "type", notificationType == null ? "" : notificationType));
    }

    /**
     * ASSN v2 검증된 payload 의 {@code data.signedTransactionInfo} 를 다시 검증·디코딩하여 transactionId 추출.
     * 검증 실패 또는 transactionId 누락 시 null.
     */
    private String extractAppStoreTransactionId(ResponseBodyV2DecodedPayload decoded) {
        if (decoded.getData() == null || decoded.getData().getSignedTransactionInfo() == null) {
            return null;
        }
        try {
            JWSTransactionDecodedPayload tx = appStorePayloadVerifier
                    .verifyAndDecodeTransaction(decoded.getData().getSignedTransactionInfo());
            String txId = tx.getTransactionId();
            if (txId != null && !txId.isBlank()) return txId;
            String origTxId = tx.getOriginalTransactionId();
            if (origTxId != null && !origTxId.isBlank()) return origTxId;
            return null;
        } catch (AppStorePayloadVerificationException e) {
            log.warn("App Store signedTransactionInfo 검증 실패: {}", e.getMessage());
            return null;
        }
    }

    /** Google Pub/Sub push subscription 표준 envelope. */
    public record RtdnEnvelope(RtdnMessage message, String subscription) {}

    public record RtdnMessage(String data, String messageId, String publishTime) {}
}
