# Step 4 — verify idempotency + currency 검증 + Play Billing 토큰 바인딩

## 배경

P0/P1 핵심. PaymentService 의 두 verify 메서드를 한 번에 보강한다.

1. **P0-1 verify idempotency**: `PaymentService.verify` (line 164) 가 `entity.status == PAID` 체크 없이 항상 PortOne 재호출 + Subscription 재발급. 같은 paymentId 로 두 번 호출되면 SubscriptionEntity 가 두 row 생성됨. (Step 1 의 V80 unique 제약이 안전망이지만, 코드 레벨 가드를 우선 적용.)
2. **P1-4 currency 검증 누락**: `PortOneClient.fromResponse` 가 `currency` 를 이미 파싱(`PortOneClient.java:112`) 하지만 `verify` 가 사용 안 함. amount 비교 직전에 `KRW` 검증 추가.
3. **P0-2 Play Billing 토큰 도용**: `verifyPlayBilling` (line 244) 이 `existing.isPresent()` 시 memberId 일치 검증 없이 진행 — 다른 회원의 purchaseToken 으로 본인 구독 발급 가능.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | verify, verifyPlayBilling 보강 |

수정 1개 (테스트):

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | 새 케이스 6건 추가 |

## verify 변경 (PaymentService.verify, line 164~)

기존 흐름:
```
1. findByPaymentId
2. memberId 일치 확인
3. plan null 확인
4. PortOne getPayment
5. status PAID 확인 → 아니면 markFailed + throw
6. amount 일치 → 아니면 markFailed + throw
7. paidAt 추출 → markPaid
8. SubscriptionEntity 새로 발급
```

새 흐름:
```
1. findByPaymentId
2. memberId 일치 확인
3. plan null 확인
4. ★ entity.status == PAID 면 기존 SubscriptionEntity 조회해 VerifyPaymentResult 즉시 반환 (idempotent)
5. PortOne getPayment
6. status PAID 확인 → 아니면 failureRecorder.markFailedInNewTx + throw
7. ★ currency KRW 확인 → 아니면 failureRecorder.markFailedInNewTx + throw PAYMENT_AMOUNT_MISMATCH
8. amount 일치 → 아니면 failureRecorder.markFailedInNewTx + throw
9. paidAt 추출 → markPaid
10. SubscriptionEntity 발급
```

PAID idempotent 분기 코드 (개념):

```java
if (entity.getStatus() == PaymentStatus.PAID) {
    LocalDateTime cachedExpiresAt = subscriptionRepository.findByPaymentId(entity.getId())
            .map(SubscriptionEntity::getExpiresAt)
            .orElse(null);
    return new VerifyPaymentResult(
            entity.getPaymentId(), entity.getAmount(), entity.getProductName(),
            entity.getPlan(), cachedExpiresAt);
}
```

currency 검증 코드 (개념):

```java
if (!"KRW".equalsIgnoreCase(info.currency())) {
    failureRecorder.markFailedInNewTx(entity.getId(),
            "currency=" + info.currency() + " expected=KRW");
    log.warn("결제 currency mismatch memberId={} paymentId={} currency={}",
            memberId, paymentId, info.currency());
    throw new SqldpassException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
}
```

기존 markFailed 호출 (status mismatch / amount mismatch) 두 곳도 `failureRecorder.markFailedInNewTx(entity.getId(), reason)` 으로 교체. 즉:

- line 179: `entity.markFailed("status=" + info.status())` → `failureRecorder.markFailedInNewTx(entity.getId(), "status=" + info.status())`
- line 186: `entity.markFailed("expected=...")` → 동일 패턴

## verifyPlayBilling 변경 (line 244~)

기존 흐름의 버그: `existing` 의 memberId 와 요청 memberId 일치 검증 없음.

새 흐름:

```java
Optional<PaymentEntity> existing = paymentRepository.findByPurchaseToken(purchaseToken);
if (existing.isPresent() && !existing.get().getMemberId().equals(memberId)) {
    log.warn("Play Billing 토큰 도용 시도 memberId={} expected={} token={}",
            memberId, existing.get().getMemberId(), mask(purchaseToken));
    throw new SqldpassException(ErrorCode.FORBIDDEN, "다른 회원의 결제 토큰입니다.");
}
// 이후 기존 idempotent / 신규 처리 분기 그대로
```

`mask(...)` 는 `PaymentWebhookController.mask` 와 동일 패턴 — PaymentService 안에 private static helper 로 두거나 인라인.

## 새 테스트 케이스 (PaymentServiceTest)

기존 테스트 옆에 다음 6개 추가:

1. `verify_PAID_상태_재호출시_기존_subscription_결과_반환_중복발급없음`
   - entity 를 PAID 로 미리 설정 (markPaid 호출), `subscriptionRepository.findByPaymentId(entityId)` 가 기존 SubscriptionEntity 반환.
   - verify 호출 → portOneClient.getPayment 호출이 0 회, subscriptionRepository.save 0 회.
   - 반환값의 expiresAt 이 기존 SubscriptionEntity 의 expiresAt 과 일치.

2. `verify_currency_KRW_아닐때_AMOUNT_MISMATCH_throw_및_markFailedInNewTx_호출`
   - `PortOnePaymentInfo` 의 currency 를 "USD" 로.
   - 예외 type 검증 + `verify(failureRecorder).markFailedInNewTx(eq(entityId), anyString())` 1회 호출.

3. `verify_status_mismatch_시_markFailedInNewTx_호출`
   - status="READY" → throw + failureRecorder 호출 검증. 기존 테스트는 status 만 검증 — 이 케이스는 markFailedInNewTx 호출 검증 추가.

4. `verifyPlayBilling_다른_memberId_가_같은_purchaseToken_재사용시_FORBIDDEN`
   - existing PaymentEntity 의 memberId=99 인데 verifyPlayBilling(memberId=1, ..., "tok-x") 호출.
   - FORBIDDEN throw, playBillingClient.verifyProduct 호출 0회, 새 결제/구독 row 생성 0회.

5. `verifyPlayBilling_PENDING_existing_타_회원_재사용도_FORBIDDEN`
   - existing 이 PENDING 상태인 케이스 — PAID 분기 진입 전에 차단되는지.

6. `verify_status_mismatch_amount_mismatch_currency_mismatch_각각_failureRecorder_호출_확인`
   - 또는 위 2/3 케이스로 흡수해도 OK. 핵심은 markFailedInNewTx mock 호출 검증.

테스트 setUp 의 `@Mock PaymentFailureRecorder failureRecorder` 는 step 3 에서 추가됐다고 가정.

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.PaymentServiceTest"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `PaymentService.verify` 가 entity.status == PAID 시 PortOne 호출 없이 즉시 반환한다.
2. `PaymentService.verify` 가 currency != KRW 시 PAYMENT_AMOUNT_MISMATCH throw 한다.
3. `PaymentService.verify` 의 모든 markFailed 경로가 `failureRecorder.markFailedInNewTx(...)` 로 교체됐다 (직접 entity.markFailed 호출 0회).
4. `PaymentService.verifyPlayBilling` 가 existing.memberId != 요청 memberId 시 FORBIDDEN throw 한다.
5. 신규 테스트 케이스 5개(혹은 6개) 모두 통과.
6. 기존 PaymentServiceTest 케이스 모두 통과 (회귀 없음). amount mismatch 케이스의 markFailed 검증은 markFailedInNewTx 호출 검증으로 갱신.
7. `gradlew.bat test` 전체 통과.

## 금지 사항

- entity.markFailed 를 PaymentService 안에서 직접 호출하지 마라. 이유: REQUIRES_NEW 우회 함정.
- verify 의 memberId 검증을 제거하지 마라. 이유: 본인 결제만 verify 가능 정책 유지.
- PortOne 응답의 currency 가 null 인 케이스를 무시하지 마라 — null 은 KRW 와 다름으로 간주해 fail. 이유: 응답 누락이 무조건 통과되면 안 된다.
- verifyPlayBilling 의 idempotent PAID 분기에서 memberId 검증을 또 한 번 추가하지 마라. 이유: 위 단일 가드가 이미 모든 분기 진입 전에 검증하므로 중복.

## Status 규칙

- 성공: step 4 `completed`, summary 에 "verify PAID idempotent + KRW currency + Play Billing 토큰 바인딩 + markFailedInNewTx 통합. 새 테스트 N건 추가, 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: PortOne currency 응답 형식이 다를 가능성에 사용자 결정 필요 시 `blocked`.
