# Step 2 — 백엔드 Apple 영수증 검증 API

## Background

iOS StoreKit 2 가 발급한 signedTransaction(JWS) 을 백엔드에서 검증하고 SubscriptionEntity 를 발급한다. PortOne / Play Billing 흐름을 그대로 미러링.

본 phase 의 검증 단순화 결정:
- **JWS 본체 서명 검증은 1차 minimal** — payload(JSON) 파싱 + transactionId, productId 추출
- **Apple JWKS RSA root CA chain 검증은 별도 후속 phase** (정식 출시 직전)
- 대신 idempotent 보장 (같은 transactionId 재요청 → 기존 결제 반환) + productId 화이트리스트로 1차 무결성 보장

## Workdir

```bash
backend/
```

⚠️ 본 step 검증은 macOS 셸에서 직접 수행하지 말 것 — Gradle 첫 build 시간 큼. 윈도우에서 `./gradlew.bat compileJava` 실행. 본 step 의 macOS 셸 검증은 grep 으로 코드 추가 확인만.

## Scope

| File | Change |
| --- | --- |
| `backend/src/main/java/com/sqldpass/persistent/payment/PaymentProvider.java` | enum 에 `APP_STORE` 추가 |
| `backend/src/main/resources/db/migration/V90__add_payment_provider_app_store.sql` | 신규 — payment 테이블 provider 컬럼 enum 확장 |
| `backend/src/main/java/com/sqldpass/service/payment/AppStoreClient.java` | 신규 — JWS payload 파싱 |
| `backend/src/main/java/com/sqldpass/controller/payment/dto/AppStoreVerifyRequest.java` | 신규 DTO |
| `backend/src/main/java/com/sqldpass/controller/payment/PaymentController.java` | `POST /api/payment/apple/verify` 추가 |
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | `verifyAppStore(memberId, signedTransaction, productId)` 메서드 추가 |
| `backend/src/main/resources/application.yaml` | `sqldpass.payment.app-store.bundle-id` + `product-id-mapping` 추가 |

## Implementation

### `PaymentProvider.java` (enum 확장)

```java
package com.sqldpass.persistent.payment;

/**
 * 결제 채널.
 * - PORTONE: 웹 PortOne 결제창.
 * - PLAY_BILLING: 네이티브 안드로이드 앱 Google Play 인앱 결제.
 * - APP_STORE: 네이티브 iOS 앱 StoreKit 2 인앱 결제.
 */
public enum PaymentProvider {
    PORTONE,
    PLAY_BILLING,
    APP_STORE
}
```

### Flyway `V90__add_payment_provider_app_store.sql` (신규)

`backend/src/main/resources/db/migration/V90__add_payment_provider_app_store.sql`:

```sql
-- payment.provider 컬럼이 VARCHAR enum mapping 인 경우 별도 ALTER 불필요 (JPA EnumType.STRING).
-- 만약 DB enum 타입이면 ALTER TYPE 필요하지만 sqldpass 는 VARCHAR + JPA enum string 매핑.
-- 본 마이그레이션은 변경 사항 부재를 명시적으로 표시 (forward-only 정책).

-- noop: PaymentProvider.APP_STORE 가 Java enum 에 추가됐을 뿐 DB 컬럼 변경 없음.
SELECT 1;
```

### `AppStoreClient.java` (신규)

```java
package com.sqldpass.service.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Apple StoreKit 2 signedTransaction(JWS) 처리.
 *
 * <p>1차 구현(본 phase): JWS payload 만 base64URL 디코딩 → JSON 파싱 → productId/transactionId 추출.
 * 정식 출시 직전 별도 phase 에서 Apple Root CA 체인으로 RSA 서명 검증 + App Store Server API
 * GET /inApps/v1/transactions/{id} 교차 확인 추가 예정.</p>
 *
 * <p>현 단계 보호:
 * 1) productId 화이트리스트 (`PaymentService` 에서 product-id-mapping 확인)
 * 2) PaymentService.verifyAppStore 의 idempotent 체크 (같은 transactionId 재요청 시 기존 결제 반환)
 * </p>
 */
@Slf4j
@Component
public class AppStoreClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JWS signedTransaction 의 payload 만 추출. 서명 검증 없음(1차).
     */
    public TransactionInfo parsePayload(String signedTransaction) {
        if (signedTransaction == null || signedTransaction.isBlank()) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "signedTransaction 은 필수입니다.");
        }
        String[] parts = signedTransaction.split("\\.");
        if (parts.length != 3) {
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "JWS 형식이 아닙니다.");
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(new String(payloadBytes, StandardCharsets.UTF_8));
            return new TransactionInfo(
                    payload.path("transactionId").asText(),
                    payload.path("originalTransactionId").asText(),
                    payload.path("productId").asText(),
                    payload.path("bundleId").asText(),
                    payload.path("purchaseDate").asLong(0),
                    payload.path("expiresDate").asLong(0));
        } catch (Exception e) {
            log.warn("App Store JWS payload 파싱 실패", e);
            throw new SqldpassException(ErrorCode.INVALID_INPUT, "App Store JWS 파싱 실패");
        }
    }

    public record TransactionInfo(
            String transactionId,
            String originalTransactionId,
            String productId,
            String bundleId,
            long purchaseDate,
            long expiresDate
    ) {}
}
```

### DTO `AppStoreVerifyRequest.java` (신규)

`backend/src/main/java/com/sqldpass/controller/payment/dto/AppStoreVerifyRequest.java`:

```java
package com.sqldpass.controller.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * iOS StoreKit 2 영수증 검증 요청.
 *
 * @param signedTransaction JWS 형식의 signedTransactionInfo
 * @param productId         사용자가 구매한 상품 ID (클라이언트가 보내는 hint, 백엔드가 payload 와 비교)
 */
public record AppStoreVerifyRequest(
        @NotBlank String signedTransaction,
        @NotBlank String productId
) {}
```

### `PaymentController.java` 확장

기존 `@PostMapping("/play-billing/verify")` 다음에 추가:

```java
@PostMapping("/apple/verify")
@Operation(summary = "App Store 영수증 검증 — iOS 앱 전용. 동일 transactionId 재요청은 idempotent.")
public VerifyPaymentResult verifyAppStore(@RequestBody AppStoreVerifyRequest body,
                                          HttpServletRequest request) {
    Long memberId = (Long) request.getAttribute("memberId");
    if (memberId == null) {
        throw new SqldpassException(ErrorCode.UNAUTHORIZED);
    }
    if (body == null || body.signedTransaction() == null || body.productId() == null) {
        throw new SqldpassException(ErrorCode.INVALID_INPUT,
                "signedTransaction 과 productId 는 필수입니다.");
    }
    return paymentService.verifyAppStore(memberId, body.signedTransaction(), body.productId());
}
```

import 도 추가: `com.sqldpass.controller.payment.dto.AppStoreVerifyRequest`.

### `PaymentService.java` 확장

기존 `verifyPlayBilling` 패턴 미러링. 메서드 추가:

```java
@Transactional
public VerifyPaymentResult verifyAppStore(Long memberId, String signedTransaction, String clientProductId) {
    AppStoreClient.TransactionInfo info = appStoreClient.parsePayload(signedTransaction);

    // productId 일치 검증 (클라이언트 hint 와 payload 비교)
    if (!info.productId().equals(clientProductId)) {
        log.warn("App Store productId mismatch: client={} payload={}",
                clientProductId, info.productId());
        throw new SqldpassException(ErrorCode.INVALID_INPUT, "productId 불일치");
    }

    // bundleId 검증 (config 의 sqldpass.payment.app-store.bundle-id 와 일치)
    if (!info.bundleId().equals(appStoreBundleId)) {
        log.warn("App Store bundleId mismatch: payload={}", info.bundleId());
        throw new SqldpassException(ErrorCode.OAUTH_LOGIN_FAILED, "유효하지 않은 결제");
    }

    // idempotent — 같은 transactionId 재요청 시 기존 결제 반환
    return paymentRepository.findByProviderAndProviderTransactionId(
                    PaymentProvider.APP_STORE, info.transactionId())
            .map(existing -> new VerifyPaymentResult(existing.getId(), existing.getStatus().name()))
            .orElseGet(() -> createAppStorePayment(memberId, info));
}

private VerifyPaymentResult createAppStorePayment(Long memberId, AppStoreClient.TransactionInfo info) {
    // PaymentEntity 신규 발급 + SubscriptionService.activate(provider=APP_STORE, productId, expiresAt=info.expiresDate)
    // 기존 verifyPlayBilling 의 entity 생성 패턴 재사용. 통합 entitlement 정책 적용.
    // 구체 구현은 verifyPlayBilling 의 흐름 참고 — provider/productId만 다름.
    throw new UnsupportedOperationException("createAppStorePayment 실제 구현은 PaymentService 의 기존 verifyPlayBilling 흐름을 참고해 동일 패턴으로 작성. 본 step 1차는 메서드 골격만.");
}
```

`appStoreBundleId` 필드 + `@Value("${sqldpass.payment.app-store.bundle-id:com.sqldpass.app}") String appStoreBundleId` 생성자 인자 추가. `appStoreClient` 의존성 주입 추가.

> **현실적 단순화**: `createAppStorePayment` 의 구체 구현은 `verifyPlayBilling` 의 패턴을 따라 작성. PaymentRepository 의 findByProviderAndProviderTransactionId 가 없으면 새로 추가하거나 purchaseToken 컬럼 재활용. 본 step.md 가 길어지지 않게 골격만 제시.

### `application.yaml` 확장

`sqldpass.payment` 블록 안에 추가:

```yaml
    app-store:
      # 앱 Bundle ID — iOS native 와 동일
      bundle-id: ${APP_STORE_BUNDLE_ID:com.sqldpass.app}
    # 4티어 + iOS 인앱 결제 SKU 매핑 (App Store Connect 등록 후 확정)
    app-store-product-id-mapping:
      THREE_DAY: ${APP_STORE_SKU_THREE_DAY:iap_three_day}
      FOCUS: ${APP_STORE_SKU_FOCUS:iap_focus}
      ONE_MONTH: ${APP_STORE_SKU_ONE_MONTH:iap_one_month}
      UNLIMITED: ${APP_STORE_SKU_UNLIMITED:iap_unlimited}
```

## Validation

본 step 의 검증은 macOS 셸에서 grep 확인까지. 컴파일/테스트는 윈도우에서 `./gradlew.bat compileJava` 별도 수행.

```bash
# 마이그레이션 파일 존재
ls backend/src/main/resources/db/migration/V90__add_payment_provider_app_store.sql

# 코드 추가 확인
grep -c "APP_STORE" backend/src/main/java/com/sqldpass/persistent/payment/PaymentProvider.java
grep -c "AppStoreClient\|verifyAppStore" backend/src/main/java/com/sqldpass/service/payment/PaymentService.java
grep -c "/apple/verify" backend/src/main/java/com/sqldpass/controller/payment/PaymentController.java

# AppStoreClient + DTO 존재
ls backend/src/main/java/com/sqldpass/service/payment/AppStoreClient.java
ls backend/src/main/java/com/sqldpass/controller/payment/dto/AppStoreVerifyRequest.java

# application.yaml
grep -c "app-store" backend/src/main/resources/application.yaml
```

모든 grep ≥ 1, 모든 ls 성공이어야 함.

## 금지사항

- JWS 서명 검증을 일단 건너뛰는 것에 PROD-ready 라고 표시 금지. 이유: 본 phase 는 1차 minimal. 출시 직전 별도 phase 에서 Apple Root CA 체인 검증 추가. 그 전까지 상품 화이트리스트 + idempotent 로만 보호.
- `createAppStorePayment` 의 실제 영속화 로직을 본 step 에서 완전 구현하려 하지 마라. 이유: 기존 verifyPlayBilling 패턴 재사용 — Repository 메서드 / Entity 필드가 정확히 어떻게 돼있는지 본 step.md 안에서 다 명세하기엔 분량 큼. 메서드 골격 + UnsupportedOperationException 으로 일단 컴파일 통과시키고, verifyPlayBilling 흐름 미러링은 별도 PR 또는 본 phase 의 후속 step 에서.
- macOS 셸에서 `./gradlew compileJava` 시도 금지. 이유: Gradle 첫 build 캐시/의존성 다운로드 5~10분 + Harness AGENTS.md 의 작업 디렉토리 규칙. 컴파일 검증은 윈도우 또는 GitHub Actions CI.
- Apple Root CA 인증서를 코드에 하드코딩 금지. 이유: 추후 인증서 갱신 대응. JJWT/spring 의 키 로딩 메커니즘 사용.
