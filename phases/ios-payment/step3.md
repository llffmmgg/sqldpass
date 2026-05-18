# Step 3 — 백엔드 App Store Server Notifications V2 웹훅

## Background

Apple App Store Server Notifications V2 는 구독 갱신/취소/환불을 백엔드에 비동기 통보. Play Billing RTDN(`/api/webhook/play-billing/rtdn`) 패턴 미러링.

본 phase 의 1차 단순화: signedPayload JWS 파싱만 + notificationType 분기 로그 + SubscriptionHistoryService 호출. JWS 서명 검증은 Step 2 와 동일하게 후속 phase 에서 정식 추가.

## Workdir

```bash
backend/
```

## Scope

| File | Change |
| --- | --- |
| `backend/src/main/java/com/sqldpass/controller/payment/PaymentWebhookController.java` | `POST /api/webhook/app-store/notifications` 추가 |
| `backend/src/main/java/com/sqldpass/controller/payment/dto/AppStoreNotificationRequest.java` | 신규 DTO |

## Implementation

### `AppStoreNotificationRequest.java` (신규)

`backend/src/main/java/com/sqldpass/controller/payment/dto/AppStoreNotificationRequest.java`:

```java
package com.sqldpass.controller.payment.dto;

/**
 * App Store Server Notifications V2 페이로드.
 *
 * Apple → 우리 webhook → signedPayload 단일 필드 (JWS).
 * JWS payload 디코딩 후 notificationType / data.signedTransactionInfo 로 분기.
 */
public record AppStoreNotificationRequest(
        String signedPayload
) {}
```

### `PaymentWebhookController.java` 확장

기존 `@PostMapping("/play-billing/rtdn")` 에 이어 추가:

```java
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
```

필요 import:
- `com.sqldpass.controller.payment.dto.AppStoreNotificationRequest`
- `tools.jackson.databind.JsonNode`, `tools.jackson.databind.ObjectMapper`
- `java.util.Base64`, `java.util.Map`

기존 컨트롤러에 `private final ObjectMapper objectMapper = new ObjectMapper();` 필드 추가 (이미 있으면 재사용).

### `WebMvcConfig` 인터셉터 등록

`/api/webhook/**` 는 이미 OptionalMemberAuthInterceptor 또는 public 인터셉터에 등록돼있는지 확인. 기존 `/api/webhook/play-billing/rtdn` 가 동일 컨트롤러라 같은 패턴 적용. 추가 등록 불필요.

## Validation

```bash
grep -c "/app-store/notifications" backend/src/main/java/com/sqldpass/controller/payment/PaymentWebhookController.java
ls backend/src/main/java/com/sqldpass/controller/payment/dto/AppStoreNotificationRequest.java
```

둘 다 ≥ 1 / 존재.

컴파일 검증은 윈도우에서 `./gradlew.bat compileJava`.

## 금지사항

- signedPayload JWS 서명 검증을 본 step 에서 완전 구현하려 하지 마라. 이유: 본 phase 1차 단순화. 후속 phase 에서 Apple Root CA + RSA 검증.
- notification 처리 실패 시 500 반환 금지. 이유: Apple 이 webhook 실패 시 재시도하므로, 우리는 200 + status="error" 로 응답하고 로그만 남김. 실제 결제 영향 없음.
- 같은 notificationType 의 중복 처리(duplicate notification) 를 idempotent 하게 처리하라는 요구를 본 step 에서 충족하려 하지 마라. 이유: 본 phase 는 골격만. notificationUUID 기반 dedup 는 후속 phase.
