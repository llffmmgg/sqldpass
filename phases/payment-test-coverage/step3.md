# Step 3 — verify idempotency 보강 테스트

## 배경

payment-hardening Step 4 에서 추가한 `verifyIsIdempotentWhenAlreadyPaid` 는 PAID 가드 분기를 검증하지만, 다음 측면이 약하다:

1. `portOneClient.getPayment(paymentId)` 호출 0회 검증이 없거나 약하다 — 재호출 비용 증가/할당량 소진 방어.
2. `subscriptionRepository.save(...)` 호출 0회 검증이 약하다 — 중복 row 방어.
3. 반환되는 `VerifyPaymentResult.expiresAt` 가 기존 SubscriptionEntity 의 expiresAt 과 **정확히** 일치하는지 검증 없음.
4. V80 의 `subscription.payment_id UNIQUE` 제약이 코드 가드 실패 시에도 DB 단에서 막아준다는 시나리오는 코드 가드와 별개로 검증되지 않음.

본 step 은 위 4 측면을 명시적으로 못박는다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | 보강 케이스 2-3건 추가 |

## 추가할 케이스

1. `verify_PAID_재호출_시_PortOne_호출_0_save_0_expiresAt_기존_그대로`
   - 기존 PaymentEntity status=PAID, markPaid 호출된 상태
   - SubscriptionEntity 기존 row mock (expiresAt = 특정 LocalDateTime)
   - verify(memberId, paymentId) 호출
   - 검증:
     - 반환 `VerifyPaymentResult.expiresAt()` == 기존 expiresAt (Equals)
     - `verify(portOneClient, times(0)).getPayment(any())`
     - `verify(subscriptionRepository, times(0)).save(any())`
     - `verify(failureRecorder, times(0)).markFailedInNewTx(any(), any())`

2. `verify_PAID_재호출_시_subscription_없으면_expiresAt_null_반환`
   - 결제는 PAID 인데 어떤 이유로 SubscriptionEntity 가 없는 케이스 (Step 6 의 복구 대상)
   - 본 step 에서는 현재 동작 (expiresAt=null 로 반환) 회귀 방지로 명시
   - 검증: 반환 `expiresAt == null`, `save` 0회. 운영 복구는 Step 6 endpoint.

3. (옵션, `@DataJpaTest` 또는 mock 으로) `subscription_payment_id_unique_제약이_같은_paymentId_두번_save_시도시_위반`
   - V80 의 UNIQUE 제약이 코드 가드 실패 시 마지막 방어선임을 못박는다
   - 단위 테스트의 mock 환경에서는 unique 위반을 자연스럽게 재현 어려우니, 가능한 옵션:
     - `@DataJpaTest` + H2 in-memory 로 실제 SubscriptionRepository.save 두 번 호출 → DataIntegrityViolationException 발생 검증
     - 또는 Repository mock 이 두 번째 save 시 DataIntegrityViolationException throw 하도록 stub
   - H2 인프라가 본 phase 범위에 부담이면 mock stub 으로만 간단히 (`save` 두 번째 호출 시 throw 검증)

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.PaymentServiceTest"
.\gradlew.bat test
```

## Acceptance Criteria

1. 위 케이스 2건(필수) + 옵션 1건이 추가되고 모두 통과.
2. 기존 `verifyIsIdempotentWhenAlreadyPaid` 케이스와 중복되지 않도록 — 본 step 의 케이스는 호출 횟수/expiresAt 정확성/UNIQUE 동작에 초점.
3. 전체 `gradlew.bat test` 회귀 없음.

## 금지 사항

- 옵션 케이스를 위해 `@DataJpaTest` + Testcontainers 도입 같은 인프라 변경은 본 step 에서 금지. 이유: 인프라 도입은 별 phase. 본 phase 는 단위/컨트롤러 테스트 범위 유지.
- PaymentService.verify 본문을 수정하지 마라. 이유: 본 step 은 회귀 방지만.

## Status 규칙

- 성공: step 3 `completed`, summary 에 "verify idempotency 보강 케이스 2-3건 추가 (PortOne 0회/save 0회/expiresAt 일치/UNIQUE 방어선), 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: H2 도입 결정 필요 시 `blocked`.
