# Step 6 — RTDN OIDC 검증

## 배경

P1-5: `PaymentWebhookController` (`backend/src/main/java/com/sqldpass/controller/payment/PaymentWebhookController.java:43-56`) 가 RTDN 인증을 URL `?token=...` shared-secret 으로 한다. URL 파라미터는 access log/proxy log/Referer 로 누출 가능 — 코드 주석도 "운영용 인증은 OIDC token verification 이 정석" 이라 인지 중.

해결: Google Pub/Sub push subscription 의 OIDC token (Authorization 헤더의 Bearer JWT) 을 검증 — `iss` 가 `https://accounts.google.com`, `aud` 가 운영자가 등록한 endpoint URL 또는 별도 audience 와 일치하는지 확인.

기존 `service/auth/GoogleOAuthClient.java:78-119` 에 `verifyIdToken` 가 RestClient 로 Google tokeninfo (`https://oauth2.googleapis.com/tokeninfo?id_token=...`) 호출 → JsonNode 반환 패턴이 있다. 동일 패턴 재사용해 새 `GoogleIdTokenVerifier` 컴포넌트 신설.

shared-secret 는 백워드호환 fallback 으로 유지 (Pub/Sub 등록 변경 전 무중단 전환).

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 1개:

| 파일 | 역할 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/auth/GoogleIdTokenVerifier.java` | tokeninfo 호출 + iss/aud 검증 |

수정 2개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/controller/payment/PaymentWebhookController.java` | OIDC 분기 + sharedSecret fallback |
| `backend/src/main/resources/application.yaml` | `sqldpass.play-billing.rtdn-oidc-audience` 키 추가 |

신규 1개 (테스트):

| 파일 | 역할 |
|------|------|
| `backend/src/test/java/com/sqldpass/controller/payment/PaymentWebhookControllerTest.java` | OIDC 분기 4건 + sharedSecret fallback 검증 |

## GoogleIdTokenVerifier 구조

```java
@Slf4j
@Component
public class GoogleIdTokenVerifier {
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Google OIDC ID token 검증 — Pub/Sub push subscription 이 보낸 Authorization Bearer 헤더용.
     *
     * @param authorizationHeader "Bearer <jwt>" 또는 null
     * @param expectedAudience    운영자가 Pub/Sub subscription 의 audience 로 등록한 값 (보통 endpoint URL)
     * @return 검증 통과 시 token claims; 실패 시 SqldpassException(UNAUTHORIZED)
     */
    public JsonNode verify(String authorizationHeader, String expectedAudience) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        String idToken = authorizationHeader.substring(7).trim();
        if (idToken.isEmpty()) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }

        String body;
        try {
            body = restClient.get()
                    .uri(TOKENINFO_URL + idToken)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("tokeninfo 호출 실패", e);
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }

        JsonNode claims;
        try {
            claims = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }

        String iss = claims.path("iss").asText("");
        String aud = claims.path("aud").asText("");
        if (!ALLOWED_ISSUERS.contains(iss)) {
            log.warn("RTDN OIDC iss 불일치 iss={}", iss);
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        if (expectedAudience == null || expectedAudience.isBlank() || !expectedAudience.equals(aud)) {
            log.warn("RTDN OIDC aud 불일치 expected={} actual={}", expectedAudience, aud);
            throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        }
        return claims;
    }
}
```

`ErrorCode.UNAUTHORIZED` 는 backend 에 이미 존재 (확인 후, 없으면 본 step 시작 직전에 사용자에게 `blocked` 보고).

## PaymentWebhookController 수정

```java
@Value("${sqldpass.play-billing.rtdn-shared-secret:}") private String rtdnSharedSecret;
@Value("${sqldpass.play-billing.rtdn-oidc-audience:}") private String rtdnOidcAudience;

private final GoogleIdTokenVerifier idTokenVerifier;

@PostMapping("/play-billing/rtdn")
public ResponseEntity<Void> handleRtdn(
        @RequestBody RtdnEnvelope envelope,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestParam(value = "token", required = false) String token) {

    if (!authenticateRtdn(authHeader, token)) {
        return ResponseEntity.status(401).build();
    }
    // 이하 기존 envelope 처리 그대로
}

private boolean authenticateRtdn(String authHeader, String token) {
    boolean oidcConfigured = rtdnOidcAudience != null && !rtdnOidcAudience.isBlank();
    boolean secretConfigured = rtdnSharedSecret != null && !rtdnSharedSecret.isBlank();

    // OIDC 헤더 우선
    if (authHeader != null && oidcConfigured) {
        try {
            idTokenVerifier.verify(authHeader, rtdnOidcAudience);
            return true;
        } catch (SqldpassException e) {
            log.warn("RTDN OIDC 검증 실패");
            return false;
        }
    }

    // OIDC 미사용 또는 헤더 없음 → shared-secret fallback
    if (secretConfigured) {
        return token != null && rtdnSharedSecret.equals(token);
    }

    // dev 환경: 둘 다 미설정 — 기존 동작 유지 (검증 스킵)
    return true;
}
```

원본 `if (rtdnSharedSecret ...) { ... }` 분기를 위 헬퍼로 대체.

## application.yaml 변경

`sqldpass.play-billing` 블록에 audience 키 추가:

```yaml
sqldpass:
  play-billing:
    package-name: ${PLAY_BILLING_PACKAGE_NAME}
    service-account-json-path: ${PLAY_BILLING_SA_PATH}
    rtdn-shared-secret: ${PLAY_BILLING_RTDN_SECRET:}
    rtdn-oidc-audience: ${PLAY_BILLING_RTDN_AUDIENCE:}
    product-id-mapping:
      ...
```

기존 키 변경 없음 — `rtdn-oidc-audience` 만 추가, `${...:}` 로 미설정 허용.

## 새 테스트 (PaymentWebhookControllerTest)

`@WebMvcTest(PaymentWebhookController.class)` + `MockMvc` 또는 SpringBootTest. 단순화 위해 controller 단위 테스트:

- `RTDN_OIDC_정상_aud_iss_시_payload_처리_200` — verifier mock 이 정상 claims 반환, body 처리 후 200.
- `RTDN_OIDC_aud_불일치시_401` — verifier mock 이 SqldpassException(UNAUTHORIZED) throw, 401 반환.
- `RTDN_OIDC_iss_불일치시_401` — 동일 throw 패턴, 401.
- `RTDN_authHeader_없고_secret_불일치시_401` — secret 만 설정, token 잘못, 401.
- `RTDN_authHeader_없고_secret_일치시_200` — fallback 통과.
- `RTDN_OIDC_audience_미설정_dev_모드_pass` — 둘 다 미설정 시 200.

`GoogleIdTokenVerifier` 는 mock. 실제 tokeninfo 호출은 별도 통합테스트 (본 step 범위 외).

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.controller.payment.PaymentWebhookControllerTest"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `GoogleIdTokenVerifier` 가 신설되고 RestClient + ObjectMapper 로 tokeninfo 호출 → iss/aud 검증.
2. `PaymentWebhookController` 가 `Authorization` 헤더가 있으면 OIDC 우선, 없으면 shared-secret fallback, 둘 다 미설정 시 dev pass.
3. `application.yaml` 에 `sqldpass.play-billing.rtdn-oidc-audience` 키 추가 (env 기본 빈값).
4. 새 테스트 6건 모두 통과.
5. 기존 RTDN 처리 (Pub/Sub envelope 디코드, oneTimeProductNotification type=2 처리) 회귀 없음.
6. `gradlew.bat test` 전체 통과.

## 금지 사항

- `GoogleOAuthClient.verifyIdToken` 을 직접 호출해 RTDN 검증을 하지 마라. 이유: 그 메서드는 사용자 OAuth aud (web client id) 를 기대 — RTDN 의 aud 와 다르다.
- 401 외의 status 로 인증 실패를 반환하지 마라 (예: 200 ack 후 무시). 이유: 보안 감사상 명확한 401 가 필요.
- 인증 통과 후 Pub/Sub ack 정책 (200 반환) 을 변경하지 마라. 이유: 처리 실패와 retry loop 회피 정책 유지.
- shared-secret fallback 를 제거하지 마라. 이유: 운영 Pub/Sub 등록 변경 전 무중단 전환 필요.

## Status 규칙

- 성공: step 6 `completed`, summary 에 "GoogleIdTokenVerifier 신설 + Webhook OIDC/sharedSecret 분기 + 테스트 6건 추가 OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: `ErrorCode.UNAUTHORIZED` 부재 또는 audience 정책에 사용자 결정 필요 시 `blocked`.
