# Step 1 — AppStorePayloadVerifier fail-fast 제거 + verify 시점 검증

## 작업 디렉터리
`backend/`

## 배경 / Why
- 2026-05-19 운영 다운타임 사고의 root cause:
  - commit `c60d85a` 의 `AppStorePayloadVerifier` 생성자가 `PRODUCTION + appAppleId=0` 조건에서 즉시 `IllegalStateException` throw
  - Bean 생성 실패 → Spring `ApplicationContext.refresh()` 실패 → Tomcat startup 실패
  - cd.yml 의 envs/env/docker-run-e 매핑 chain 끊김 (별도 commit 작업 트리만) 으로 컨테이너 env 가 0 으로 들어가서 fail-fast 트리거
- 결제 webhook 1개 모듈의 misconfig 가 backend 전체를 죽이는 설계 결함.
- 같은 chain 끊김 (다른 새 env 도입 + cd.yml 누락 등) 이 또 발생해도 backend 죽지 않도록 격리.

## 변경 대상

### `backend/src/main/java/com/sqldpass/service/payment/AppStorePayloadVerifier.java`

**생성자:**
- `PRODUCTION + appAppleId=0` 분기에서 `throw new IllegalStateException(...)` 제거
- 대신 `private final boolean misconfigured` 필드 셋팅
- WARN 로그만 출력
- `SignedDataVerifier` 생성도 try-catch 로 감싸 실패 시 swallow (`verifier=null`)

**verifyAndDecodeNotification / verifyAndDecodeTransaction 메서드:**
- 시작 시 `if (misconfigured) throw new AppStorePayloadVerificationException(...)` 가드
- `if (verifier == null) throw new AppStorePayloadVerificationException(...)` 가드
- 나머지 흐름 (signedPayload 비었음 / verifier 호출) 그대로

**`@PostConstruct logConfig()`:**
- 그대로 유지. misconfigured 상태도 WARN 으로 알리도록 보강.

### `backend/src/test/java/com/sqldpass/service/payment/AppStorePayloadVerifierTest.java`

**`@Disabled` 해제 후 시나리오 정정:**

1. SANDBOX + appAppleId=0 → 생성자 성공 + blank input reject (기존 시나리오)
2. **신규**: PRODUCTION + appAppleId=0 → 생성자 성공 (이전엔 throw 였음) + verifyAndDecodeNotification 호출 시 `AppStorePayloadVerificationException` throw

## 작업 절차
1. AppStorePayloadVerifier.java 위 패턴으로 수정
2. AppStorePayloadVerifierTest.java `@Disabled` 해제 + 신규 misconfigured 테스트 추가
3. `gradlew test --tests "*AppStorePayloadVerifier*" --tests "*PaymentWebhookController*"` 통과 확인
4. commit (push 안 함, 사용자 명시 지시 후만)

## 검증

```powershell
cd C:\Users\admin\desktop\sqldpass\sqldpass\backend
.\gradlew.bat test --tests "*AppStorePayloadVerifier*" --tests "*PaymentWebhookController*" --console=plain --no-daemon
```

기대:
- BUILD SUCCESSFUL
- `AppStorePayloadVerifierTest`: 2 tests (sandbox + production-misconfigured) 통과
- `PaymentWebhookControllerTest`: 기존 19 tests 그대로 통과

## 금지사항
- `PaymentWebhookController` 의 401 처리 흐름 변경 금지. 이유: 본 step 은 verifier 만 격리. 컨트롤러는 그대로 `AppStorePayloadVerificationException` catch → 401 반환.
- `Bean` 자체를 Optional 또는 nullable Bean 으로 변경 금지. 이유: 호출 측 (PaymentWebhookController) 의 null 처리 부담 증가. verifier 인스턴스는 항상 존재하되 내부 상태로 misconfigured 표현.
- Apple Root CA 로딩 (`loadRootCertificates`) 실패 시 throw 제거 금지. 이유: 그건 jar 패키징 자체의 결함 — 빌드 시점 잡혀야. PRODUCTION + appAppleId=0 같은 운영 env 관련만 graceful.

## 산출물
- 2 파일 diff
- gradlew test 결과 마지막 10줄
