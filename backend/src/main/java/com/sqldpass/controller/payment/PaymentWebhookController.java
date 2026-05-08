package com.sqldpass.controller.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
 * <p>보안: 운영용 인증은 OIDC token verification 이 정석이지만 MVP 에선 URL shared secret 로 시작.
 * Pub/Sub subscription 의 push endpoint 에 {@code ?token=...} 를 붙여 등록하고 백엔드에서 일치 확인.
 */
@Slf4j
@Tag(name = "결제 webhook", description = "Play Billing RTDN 등 외부 결제 통지 처리")
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sqldpass.play-billing.rtdn-shared-secret:}")
    private String rtdnSharedSecret;

    @PostMapping("/play-billing/rtdn")
    @Operation(summary = "Play Billing RTDN — 환불/재구매 통지를 받아 구독 상태 동기화")
    public ResponseEntity<Void> handleRtdn(@RequestBody RtdnEnvelope envelope,
                                           @RequestParam(value = "token", required = false) String token) {
        // shared-secret 검증 — 미설정 시(dev 환경) 검증 스킵.
        if (rtdnSharedSecret != null && !rtdnSharedSecret.isBlank()) {
            if (token == null || !rtdnSharedSecret.equals(token)) {
                log.warn("RTDN shared-secret 불일치 — 무시");
                return ResponseEntity.status(401).build();
            }
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

    private static String mask(String token) {
        if (token == null) return "null";
        return token.length() <= 8 ? "***" : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /** Google Pub/Sub push subscription 표준 envelope. */
    public record RtdnEnvelope(RtdnMessage message, String subscription) {}

    public record RtdnMessage(String data, String messageId, String publishTime) {}
}
