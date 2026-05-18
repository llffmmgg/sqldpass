# Step 1 — Apple ASSN v2 JWS 검증 도입

## 작업 디렉터리
`backend/`

## 배경 / Why
- `backend-refund-webhook` phase 가 ASSN v2 REFUND/REVOKE 처리를 구현했지만 페이로드 신뢰 상태. 누구든 Apple 을 사칭한 위조 JWS 를 보내 `subscription.revoke` 를 트리거할 수 있음 (보안 P0).
- Apple 이 공식 검증 라이브러리(`app-store-server-library`) 를 Maven Central 에 배포 — Java 17+ 지원. 이걸 사용하는 게 표준.

## 변경 대상

### 1. `backend/build.gradle` (또는 `backend/build.gradle.kts`)
- 의존성 추가:
  ```gradle
  implementation 'com.apple.itunes:app-store-server-library:3.5.0'
  ```
  (정확한 최신 버전은 Maven Central 에서 확인. 3.x 계열이면 OK.)
- 라이브러리는 `SignedDataVerifier`, `ResponseBodyV2DecodedPayload`, `Environment` 타입 제공.

### 2. `application.yaml` — 설정 추가
- Apple 검증에 필요한 값:
  - `app-store.bundle-id` — 예: `com.sqldpass.app` (Info.plist 의 bundle id).
  - `app-store.app-apple-id` — App Store Connect 의 numeric app id (예: `1234567890`).
  - `app-store.environment` — `SANDBOX` | `PRODUCTION` (또는 둘 다 시도하는 헬퍼 가능).
- 누락 시 검증 비활성화 가능하도록 fallback (개발 환경) — 단 production 에서는 반드시 설정.

### 3. 새 서비스 `backend/src/main/java/com/sqldpass/service/payment/AppStorePayloadVerifier.java`
- `@Service` Spring component.
- 생성자에서 `SignedDataVerifier` 를 빌드 — Apple 의 root certificates 는 `app-store-server-library` 의 helper(`AppleRootCertificates.fromBundle()` 같은) 또는 자체 PEM 로딩.
- 메서드: `verifyAndDecode(String signedPayload): ResponseBodyV2DecodedPayload`
  - 검증 실패 (서명/체인/만료/bundleId 불일치) → 명확한 예외 throw — `IllegalArgumentException` 또는 커스텀 `AppStorePayloadVerificationException`.
  - 성공 시 decoded payload 반환.
- 또한 `verifyTransaction(String signedTransactionInfo): JWSTransactionDecodedPayload` 헬퍼도 제공 — `data.signedTransactionInfo` 검증용.

### 4. `PaymentWebhookController.java` 수정
- 현재 ASSN v2 경로는 raw JWS payload 를 base64 디코드해서 raw JSON 파싱. 이걸 다음으로 교체:
  ```java
  try {
      ResponseBodyV2DecodedPayload decoded = verifier.verifyAndDecode(signedPayload);
      String notificationType = decoded.getNotificationType().name();
      JWSTransactionDecodedPayload tx = verifier.verifyTransaction(decoded.getData().getSignedTransactionInfo());
      String transactionId = tx.getTransactionId();
      // existing dispatch: REFUND / REVOKE -> revokeAppStoreByTransactionId
  } catch (AppStorePayloadVerificationException e) {
      log.warn("ASSN v2 검증 실패 — webhook reject: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
  ```
- 기존 raw JWS 파싱·base64 decode 코드는 모두 제거. **TODO 코멘트도 제거.**

### 5. 테스트 `backend/src/test/java/com/sqldpass/controller/payment/PaymentWebhookControllerTest.java` 보강
- 기존 ASSN 시나리오들에 검증 mock 추가:
  - `verifier.verifyAndDecode` mock 으로 verified payload 반환 → 기존 dispatch 검증 그대로 통과.
  - 검증 throw 케이스 추가 → controller 401 반환 + revoke 호출 안 됨 검증.
- 별도 `AppStorePayloadVerifierTest` — 실제 Apple 서명 검증은 키 없이 테스트 불가. 인스턴스화 가능성, 빈 payload reject, 잘못된 base64 reject 같은 라이브러리 호출 형식만 sanity check (혹은 라이브러리 동작은 테스트하지 않음으로 결정 — `@Disabled` 명시).

## 작업 절차
1. Maven Central 에서 `com.apple.itunes:app-store-server-library` 최신 버전 확인 (3.x 계열).
2. build.gradle 의존성 추가, `./gradlew compileJava` 통과 확인.
3. application.yaml 에 설정 추가 (실제 값은 placeholder — `${APP_STORE_BUNDLE_ID:com.sqldpass.app}` 등 환경변수 fallback).
4. AppStorePayloadVerifier 구현. Apple root CA 로딩은 라이브러리 헬퍼 사용.
5. PaymentWebhookController 수정 — 검증 호출 + 실패 reject + 기존 dispatch 보존.
6. 테스트 보강.
7. `gradlew test` 통과.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\backend
.\\gradlew.bat test
```

## 금지사항
- 자체 JWS/JWT 라이브러리(`nimbus-jose-jwt` 등) 직접 사용 금지. 이유: Apple 공식 라이브러리가 인증서 체인 + 만료 + bundle id 검증을 통합 처리. 자체 구현은 누락·취약점 위험.
- 검증 우회 옵션을 production profile 에 두지 말 것. 이유: 위장 webhook 으로 다른 사용자의 entitlement 회수 가능.
- 검증 실패 시 silent log 하지 말 것. 반드시 HTTP 401/403 으로 reject — Apple 이 재전송 시 재검증 가능.

## 산출물
- 의존성/설정/신규 클래스/수정 컨트롤러/테스트 목록.
- `gradlew test` 결과 마지막 10줄.
