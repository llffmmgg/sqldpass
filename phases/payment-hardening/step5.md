# Step 5 — verifyPlayBilling 정책 일관화

## 배경

P2-6: PortOne `prepare/verify` 흐름은 `evaluateUpgrade` 로 UNLIMITED 활성/같은 plan/다운그레이드를 차단하는데, `verifyPlayBilling` 은 이 검증을 거치지 않는다. 결과: 같은 회원이 웹에서 ONE_MONTH 활성 상태에서 앱으로 ONE_MONTH 추가 결제 → 중복 plan 발급. 또는 UNLIMITED 활성 상태에서 THREE_DAY 결제 → 다운그레이드 row 가 dormant 누적.

**제약**: Play Billing 은 백엔드가 결제 차단을 결정하기 전에 device-Google 사이에서 결제가 끝난다. 따라서 차단 시점은 verify 단계 — 사용자는 이미 Google 에 결제했으므로 환불/사용자 안내가 필요. 본 step 은 차단만 구현하고 환불 자동화는 별 issue (운영자가 수동 환불).

prorate 차감은 PortOne 흐름에서만 적용 — Play Billing 은 부분 환불 메커니즘이 복잡하므로 evaluateUpgrade 의 allowed 체크만 사용 (discount=0 이라 가정한 채 처리).

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | verifyPlayBilling 에 evaluateUpgrade 호출 추가 |

수정 1개 (테스트):

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | UNLIMITED 활성/다운그레이드/같은 plan 케이스 3건 추가 |

## verifyPlayBilling 변경

위치: `playBillingClient.verifyProduct(...)` 직후, idempotent PAID 분기 통과 이후, 새 결제/구독 발급 직전.

```java
PlayBillingClient.PlayPurchaseInfo info = playBillingClient.verifyProduct(productId, purchaseToken);
if (!info.isPurchased()) {
    log.warn("Play Billing 검증 실패 ...");
    throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
}

// ★ 추가 — 활성 구독 정책 검증
SubscriptionEntity active = subscriptionRepository
        .findActiveByMemberId(memberId, LocalDateTime.now())
        .stream().findFirst().orElse(null);
PaymentProperties.PlanConfig planConfig = properties.configFor(plan);
int baseAmount = planConfig.getAmount();
UpgradeEvaluation eval = evaluateUpgrade(active, plan, baseAmount);
if (!eval.allowed()) {
    log.warn("Play Billing 정책 차단 memberId={} productId={} reason={}",
            memberId, productId, eval.reason());
    throw new SqldpassException(ErrorCode.INVALID_INPUT, eval.reason());
}

// 기존 흐름: PaymentEntity 생성/저장, markPaid, SubscriptionEntity 발급 ...
```

기존 코드의 `PaymentProperties.PlanConfig planConfig = properties.configFor(plan);` 와 `int baseAmount = planConfig.getAmount();` 는 evaluateUpgrade 호출 직전으로 이동 (중복 lookup 회피).

`eval.discount()` 무시 — Play Billing 은 prorate 적용 안 함. PaymentEntity 의 amount/baseAmount 는 모두 baseAmount 동일 값 (기존 그대로).

## 새 테스트 케이스

다음 3건을 PaymentServiceTest 에 추가:

1. `verifyPlayBilling_UNLIMITED_활성에서_재구매_INVALID_INPUT`
   - active = UNLIMITED 구독, verifyPlayBilling(memberId, "iap_one_month", token) 호출.
   - INVALID_INPUT throw, `paymentRepository.save`/`subscriptionRepository.save` 0회, `playBillingClient.acknowledge` 0회.

2. `verifyPlayBilling_같은_plan_재구매_INVALID_INPUT`
   - active = ONE_MONTH (만료 25일 남음), verifyPlayBilling(memberId, "iap_one_month", token).
   - INVALID_INPUT.

3. `verifyPlayBilling_다운그레이드_INVALID_INPUT`
   - active = ONE_MONTH, verifyPlayBilling(memberId, "iap_three_day", token).
   - INVALID_INPUT.

기존 통과 케이스(`verifyPlayBillingCreatesSubscriptionAndAcknowledges`) 가 active=null 케이스라 영향 없음. 다만 `subscriptionRepository.findActiveByMemberId` mock 이 기본 empty 반환되도록 설정 (`@MockitoExtension` 의 lenient 또는 BDDMockito.given(...).willReturn(List.of())).

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.PaymentServiceTest"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `verifyPlayBilling` 가 `playBillingClient.verifyProduct` 통과 후 새 결제/구독 발급 직전에 `evaluateUpgrade` 호출.
2. `!eval.allowed()` 시 `INVALID_INPUT(eval.reason())` throw.
3. acknowledge 와 결제/구독 row 저장이 0회 (차단 분기에서).
4. 새 테스트 3개 모두 통과.
5. 기존 verifyPlayBilling 테스트 (active=null, idempotent) 회귀 없음.
6. `gradlew.bat test` 전체 통과.

## 금지 사항

- prorate discount 를 Play Billing 에 적용하지 마라. 이유: Play 환불 메커니즘 부재 → 회계 불일치 위험.
- 차단된 결제를 자동 환불하지 마라. 이유: 본 step 범위 외 + AndroidPublisher.refund API 는 별도 권한/정책 필요.
- evaluateUpgrade 의 reason 메시지를 가공하지 마라. 이유: PortOne 흐름과 동일 메시지 ("이미 무제한 이용권을 이용 중입니다." 등) 로 UI 일관성.
- 활성 구독 조회 결과를 캐시하지 마라. 이유: 동시성 시 stale.

## Status 규칙

- 성공: step 5 `completed`, summary 에 "verifyPlayBilling 에 evaluateUpgrade 적용 (UNLIMITED/같은plan/다운그레이드 차단), 테스트 3건 추가, 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: prorate 정책에 사용자 결정 필요 시 `blocked`.
