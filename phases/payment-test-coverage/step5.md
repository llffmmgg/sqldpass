# Step 5 — RTDN 중복 수신 통합 테스트

## 배경

Google Pub/Sub 은 at-least-once delivery — 같은 RTDN payload 가 두 번 이상 도착할 수 있다. `PaymentService.revokePlayBillingByToken` 은 이미 `if (payment.getStatus() != PaymentStatus.CANCELLED)` 가드로 markCancelled 를 한 번만 수행한다. 다만 컨트롤러 통합 레벨에서 같은 envelope 두 번 POST 시 history REFUNDED 도 1개만 기록되는지, 200 ack 가 두 번 모두 반환되는지를 명시적으로 못박은 케이스가 없다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/controller/payment/PaymentWebhookControllerTest.java` | 중복 수신 케이스 1-2건 추가 |

## 추가할 케이스

1. `rtdn_같은_purchaseToken_중복_수신_시_revoke_1회만_200_2회`
   - 첫 번째 RTDN envelope POST → 200 + `paymentService.revokePlayBillingByToken("tok-x")` 1회 호출
   - 같은 envelope 두 번째 POST → 200 + service 호출은 또 1회 (총 2회) — controller 는 항상 호출
   - 단, service 내부에서 두 번째는 no-op (status 이미 CANCELLED). 본 케이스는 controller 동작 검증이라 service 호출 자체는 2회 (mock 측면). 실제 idempotent 는 service 단위 테스트 `revokePlayBillingByTokenExpiresSubscription` 가 따로 검증.

2. `rtdn_oneTimeProductNotification_type_1_PURCHASED_는_revoke_호출_0`
   - notificationType=1 (PURCHASED, 정보성) envelope POST
   - service 호출 0회 + 200 응답

3. (옵션) `rtdn_payload_파싱_실패해도_200_반환` — 잘못된 base64/JSON envelope POST → 200 + service 호출 0회 (Pub/Sub retry 회피)

## mockMvc 설정 참고

기존 `PaymentWebhookControllerTest` 가 `@WebMvcTest(PaymentWebhookController.class)` + `@MockBean PaymentService` + `@MockBean GoogleIdTokenVerifier` 패턴이면 그대로 재사용. base64 envelope 생성 헬퍼 메서드도 기존 케이스에서 차용 가능.

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.controller.payment.PaymentWebhookControllerTest"
.\gradlew.bat test
```

## Acceptance Criteria

1. 위 케이스 2건(필수) + 옵션 1건이 추가되고 모두 통과.
2. 컨트롤러 본문 변경 0건.
3. 기존 OIDC/sharedSecret 케이스 회귀 없음.

## 금지 사항

- service `revokePlayBillingByToken` 의 idempotency 를 본 케이스에서 다시 검증하지 마라. 이유: 서비스 단위 테스트 (`revokePlayBillingByTokenExpiresSubscription`) 와 중복. 본 step 은 컨트롤러 레벨 통합 동작만.
- payload 파싱 실패 케이스에서 500 반환을 기대하지 마라. 이유: Pub/Sub 가 500 받으면 retry — 본 시스템 정책은 200 ack.

## Status 규칙

- 성공: step 5 `completed`, summary 에 "RTDN 중복 수신 + 정보성 type=1 + payload 오류 케이스 추가, 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: WebMvcTest 의 인증 인터셉터 통과 설정에 사용자 결정 필요 시 `blocked`.
