# Step 1 — 백엔드: UPGRADED action + 결제 시 history 기록

## 배경

`SubscriptionHistoryAction` enum 에 현재 `GRANTED / REVOKED / EXPIRED / REFUNDED / ARCHIVED` 만 있고 **`UPGRADED` 부재**. 그리고 결제 verify 3 흐름 (`verify` PortOne / `verifyPlayBilling` / `createAppStorePayment`) 셋 다 SubscriptionEntity 발급 시 `historyService.record(...)` 를 호출하지 않아 관리자가 결제 이력을 plan 만 보고 신규/업그레이드 구분할 수 없는 상태.

본 step 에서 enum 에 `UPGRADED` 추가하고 3 흐름에서 결제 직후 history 기록. 활성 plan 이 있던 상태였으면 `UPGRADED`, 없었으면 `GRANTED` 로 분기.

DB 마이그레이션 없음 — `SubscriptionHistoryAction` 은 string enum 으로 저장되므로 enum 값 추가는 schema 변경 0.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `persistent/payment/SubscriptionHistoryAction.java` | enum 에 `UPGRADED` 추가. 위치는 `GRANTED` 다음. |
| `service/payment/PaymentService.java` | `verify` / `verifyPlayBilling` / `createAppStorePayment` 3 메서드에서 `subscriptionRepository.save(subscription)` 직후 `historyService.record(...)` 호출. 분기: `active != null` 이면 `UPGRADED`, 아니면 `GRANTED`. |
| `service/payment/PaymentServiceTest.java` | 시나리오 3종 추가: (a) PortOne verify 신규 결제 → `GRANTED` 1회 (b) Play Billing 활성 위 결제 → `UPGRADED` 1회 (c) App Store idempotent (PAID 재요청) → history 0회. |

## 구현 상세

### enum 추가

```java
public enum SubscriptionHistoryAction {
    GRANTED,
    UPGRADED,
    REVOKED,
    EXPIRED,
    REFUNDED,
    ARCHIVED;
}
```

### `verify` (PortOne)

`subscriptionRepository.save(subscription)` (현재 ~line 254) 직후:

```java
SubscriptionHistoryAction action = (active != null)
    ? SubscriptionHistoryAction.UPGRADED
    : SubscriptionHistoryAction.GRANTED;
historyService.record(memberId, plan, action,
    "verify:portone", null, entity.getId());
```

**주의**: PortOne 의 `verify` 메서드는 함수 내에서 `active` 변수를 직접 들고 있지 않음 (evaluateUpgrade 는 prepare 단계에서 호출). verify 흐름에서 active 를 한 번 더 조회해야 함:

```java
// markPaid 직전에 추가
SubscriptionEntity activeBefore = subscriptionRepository
    .findActiveByMemberId(memberId, LocalDateTime.now())
    .stream().findFirst().orElse(null);
// 그리고 subscription save 후 action 분기에 activeBefore 사용
```

### `verifyPlayBilling`

`subscriptionRepository.save(subscription)` (~line 391) 직후 동일 패턴. 본 메서드는 이미 `active` 변수가 위에서 evaluateUpgrade 직전에 만들어져 있음 — 그대로 재사용. reason 은 `"verify:play_billing"`.

### `createAppStorePayment`

`subscriptionRepository.save(subscription)` (~line 528) 직후 동일. `active` 변수 재사용. reason 은 `"verify:app_store"`.

### 테스트

기존 `PaymentServiceTest` 의 `historyService` mock 은 이미 등록되어 있음 (이전 phase 에서 추가). `verify(historyService, times(1)).record(...)` 패턴.

시나리오:

1. **신규 PortOne 결제 → GRANTED**: active 없음 (`findActiveByMemberId` 가 빈 리스트) → `verify` 호출 후 `historyService.record(eq(memberId), eq(SubscriptionPlan.THREE_DAY), eq(GRANTED), ...)` 1회 검증.
2. **활성 위 Play 업그레이드 → UPGRADED**: ONE_MONTH active stub → `verifyPlayBilling("iap_unlimited", ...)` 후 `record(eq(UNLIMITED), eq(UPGRADED), ...)` 1회.
3. **App Store idempotent (PAID 재요청) → history 0회**: PAID 분기에서 즉시 return 하므로 `createAppStorePayment` 까지 안 옴 → `verify(historyService, never()).record(...)`.

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.PaymentServiceTest"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `SubscriptionHistoryAction.UPGRADED` enum 존재.
2. 3 verify 흐름 모두 SubscriptionEntity save 직후 `historyService.record(...)` 호출.
3. `active != null` 이면 `UPGRADED`, 아니면 `GRANTED` 분기 정확.
4. PaymentServiceTest 신규 시나리오 3종 통과.
5. 기존 PaymentServiceTest 전체 통과 (회귀 0).
6. `.\gradlew.bat test` 전체 통과.

## 금지 사항

- DB 마이그레이션 추가하지 마라. **이유**: enum 은 string 저장이라 Flyway 변경 불필요.
- idempotent (PAID/CANCELLED) 분기에 history 기록 호출 추가하지 마라. **이유**: 같은 결제가 history 에 중복 누적됨.
- `SubscriptionHistoryAction.GRANTED` 의 의미를 바꾸지 마라. **이유**: 어드민 수동 발급(`AdminSubscriptionService.grantManual`) 도 GRANTED 를 쓰고 있어 정의 변경 시 회귀.
- `historyService.record` 의 reason 필드를 freeform 문자열 외 다른 곳에 쓰지 마라. **이유**: audit log 본문이라 기계 파싱 대상 아님.

## Status 규칙

- 성공: `completed` + summary "SubscriptionHistoryAction.UPGRADED + 3 verify 흐름 history.record(GRANTED/UPGRADED 분기) + PaymentServiceTest 3종 추가, test/compile OK".
- 실패: 3회 재시도 후 `error`.
