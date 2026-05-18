package com.sqldpass.controller.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqldpass.controller.payment.dto.AppStoreNotificationRequest;
import com.sqldpass.service.auth.GoogleIdTokenVerifier;
import com.sqldpass.service.common.SqldpassException;
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
 */
@Slf4j
@Tag(name = "결제 webhook", description = "Play Billing RTDN 등 외부 결제 통지 처리")
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final GoogleIdTokenVerifier idTokenVerifier;
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
            JsonNode oneTime = root.get("oneTimeProductNotification");
            if (oneTime != null && !oneTime.isNull()) {
                int notificationType = oneTime.has("notificationType")
                        ? oneTime.get("notificationType").asInt()
                        : 0;
                String purchaseToken = oneTime.has("purchaseToken")
                        ? oneTime.get("purchaseToken").asText()
                        : null;
                // 1 = PURCHASED (정보성, 우리는 verify 단계에서 이미 처리), 2 = CANCELED (환불)
                if (notificationType == 2 && purchaseToken != null && !purchaseToken.isBlank()) {
                    boolean revoked = paymentService.revokePlayBillingByToken(purchaseToken);
                    log.info("Play Billing RTDN refund 처리 token={} revoked={}",
                            mask(purchaseToken), revoked);
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
    public Map<String, String> handleAppStoreNotification(@RequestBody AppStoreNotificationRequest body) {
        if (body == null || body.signedPayload() == null || body.signedPayload().isBlank()) {
            log.warn("App Store notification 빈 signedPayload");
            return Map.of("status", "ignored");
        }

        try {
            String[] parts = body.signedPayload().split("\\.");
            if (parts.length != 3) {
                log.warn("App Store notification JWS 형식 아님");
                return Map.of("status", "ignored");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            String notificationType = payload.path("notificationType").asText();
            String subtype = payload.path("subtype").asText();

            log.info("App Store notification: type={} subtype={}", notificationType, subtype);

            // notificationType 분기 — 1차는 로그 + history 기록만, 실제 entitlement 동기화는 후속
            switch (notificationType) {
                case "SUBSCRIBED", "DID_RENEW" -> {
                    // TODO: SubscriptionService.activateFromAppStoreNotification(payload)
                    log.info("App Store SUBSCRIBED/DID_RENEW — 후속 phase 에서 entitlement 갱신 구현");
                }
                case "EXPIRED", "DID_FAIL_TO_RENEW", "GRACE_PERIOD_EXPIRED", "REVOKE" -> {
                    log.info("App Store {} — 후속 phase 에서 entitlement 만료 구현", notificationType);
                }
                case "REFUND" -> {
                    log.info("App Store REFUND — 후속 phase 에서 환불 동기화 구현");
                }
                default -> log.info("App Store notification 미처리 type: {}", notificationType);
            }

            return Map.of("status", "ok", "type", notificationType);
        } catch (Exception e) {
            log.warn("App Store notification 처리 실패", e);
            return Map.of("status", "error");
        }
    }

    /** Google Pub/Sub push subscription 표준 envelope. */
    public record RtdnEnvelope(RtdnMessage message, String subscription) {}

    public record RtdnMessage(String data, String messageId, String publishTime) {}
}
