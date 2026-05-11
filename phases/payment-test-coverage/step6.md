# Step 6 — 결제 후 권한 미부여 복구 endpoint

## 배경

`PaymentEntity.status == PAID` 인데 `SubscriptionEntity` 가 없는 케이스 — verify 도중 PortOne 응답을 받았으나 SubscriptionEntity.save 직전 예외(예: DB 일시 장애, V80 unique 충돌 등 비정상) 로 트랜잭션이 깨졌거나, 결제 후 운영자가 SubscriptionEntity 만 잘못 지운 경우.

payment-hardening Step 3 의 PaymentService.verify PAID 가드 분기는 이미 `subscription.findByPaymentId(...).map(expiresAt)::orElse(null)` 로 반환 — 즉 다시 verify 를 호출해도 SubscriptionEntity 가 자동 발급되지 않는다 (사용자 결정 사항: 자동 보완 X, 어드민 endpoint O).

해결: 어드민 endpoint `POST /api/admin/payments/{paymentId}/reissue-subscription` 신설.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개 + (Step 4 의) 컨트롤러에 endpoint 추가:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | `reissueSubscription(paymentEntityId, actorAdminId)` 메서드 추가 |
| `backend/src/main/java/com/sqldpass/controller/admin/AdminPaymentController.java` | (Step 4 에서 신설) endpoint 추가 |

수정 1개 (테스트):

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | reissueSubscription 케이스 추가 |
| `backend/src/test/java/com/sqldpass/controller/admin/AdminPaymentControllerTest.java` | endpoint 테스트 추가 |

## PaymentService.reissueSubscription 시그니처

```java
@Transactional
public ReissueResult reissueSubscription(Long paymentEntityId, Long actorAdminId) {
    PaymentEntity entity = paymentRepository.findById(paymentEntityId)
            .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
    if (entity.getStatus() != PaymentStatus.PAID) {
        throw new SqldpassException(ErrorCode.INVALID_INPUT, "PAID 상태인 결제만 재발급 대상입니다.");
    }
    Optional<SubscriptionEntity> existing = subscriptionRepository.findByPaymentId(entity.getId());
    if (existing.isPresent() && existing.get().isActive(LocalDateTime.now())) {
        // 이미 활성 구독 존재 — idempotent
        return new ReissueResult(false, existing.get().getExpiresAt());
    }
    SubscriptionPlan plan = entity.getPlan();
    if (plan == null) {
        throw new SqldpassException(ErrorCode.INVALID_INPUT, "plan 정보 없는 결제는 재발급 불가합니다.");
    }
    LocalDateTime paidAt = entity.getPaidAt() != null ? entity.getPaidAt() : LocalDateTime.now();
    LocalDateTime expiresAt = plan.isLifetime() ? null : paidAt.plusDays(plan.getDays());
    SubscriptionEntity subscription = new SubscriptionEntity(
            entity.getMemberId(), plan, entity.getId(), paidAt, expiresAt);
    subscriptionRepository.save(subscription);

    historyService.record(entity.getMemberId(), plan,
            SubscriptionHistoryAction.GRANTED,
            "admin-reissue:paymentId=" + entity.getPaymentId(),
            actorAdminId, entity.getId());
    return new ReissueResult(true, expiresAt);
}

public record ReissueResult(boolean issued, LocalDateTime expiresAt) {}
```

V80 UNIQUE 제약이 두 번째 시도를 막아주지만, 위 idempotent 가드(`existing.isActive`) 가 1차 방어. existing 이 expired 면 새 row save 가 unique 위반 — 그 경우 운영자가 새 결제를 요구하거나 `expireManual` 후 재발급. 본 step 의 정책: 이미 row 가 있으면 expiresAt 만 반환, 새 row 안 만듦 (단순).

## AdminPaymentController endpoint 추가 (Step 4 의 컨트롤러에 추가)

```java
@PostMapping("/{paymentId}/reissue-subscription")
@Operation(summary = "결제 후 구독 미발급 복구 — PAID 결제로 SubscriptionEntity 강제 재발급")
public ReissueResponse reissue(@PathVariable Long paymentId,
                                @RequestBody ReissueRequest body,
                                HttpServletRequest request) {
    Long actorAdminId = (Long) request.getAttribute("memberId");
    if (actorAdminId == null) throw new SqldpassException(ErrorCode.UNAUTHORIZED);
    PaymentService.ReissueResult result =
            paymentService.reissueSubscription(paymentId, actorAdminId);
    return new ReissueResponse(paymentId, result.issued(), result.expiresAt());
}

public record ReissueRequest(String reason) {}
public record ReissueResponse(Long paymentId, boolean issued, LocalDateTime expiresAt) {}
```

`reason` body 는 옵션 — actorAdminId 만으로 audit 보존이 충분하지만 운영 편의상 보존. service 메서드에는 전달 안 함 (history reason 은 자동 생성 문구 사용).

## 단위 테스트 (PaymentServiceTest)

1. `reissueSubscription_PAID_결제에_subscription_없으면_새로_발급_+_history_GRANTED`
   - PaymentEntity PAID, paidAt 기록됨, subscriptionRepository.findByPaymentId empty
   - 호출 후 save 1회 + history GRANTED 1회 + result.issued()==true + expiresAt 계산 정확

2. `reissueSubscription_이미_활성_subscription_있으면_idempotent_save_0`
   - existing.isActive(now)==true
   - 호출 후 save/history 0회 + result.issued()==false

3. `reissueSubscription_PAID_아닌_결제는_INVALID_INPUT`
   - PaymentEntity PENDING/FAILED → throw

4. `reissueSubscription_plan_null_결제는_INVALID_INPUT`
   - 옛 mock-exam 결제 등 plan 없는 row → throw

5. `reissueSubscription_paidAt_null_이면_now_사용`
   - PaymentEntity paidAt null → expiresAt = now + plan.days (분 단위 근사)

## 컨트롤러 테스트 (AdminPaymentControllerTest)

- `reissue_정상_200_및_service_호출` — service.reissueSubscription 1회 호출 + 결과 매핑
- `reissue_미인증_시_401`
- (옵션) `reissue_service_가_INVALID_INPUT_throw_시_400_매핑`

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.*" --tests "com.sqldpass.controller.admin.*"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `PaymentService.reissueSubscription(Long, Long)` 시그니처 + `ReissueResult` record 추가.
2. `AdminPaymentController` 에 `POST /api/admin/payments/{paymentId}/reissue-subscription` endpoint 추가.
3. service 단위 테스트 5건 + 컨트롤러 테스트 2-3건 추가, 모두 통과.
4. history GRANTED 가 1회 기록되고 reason 에 "admin-reissue:paymentId=..." 가 포함.
5. `gradlew.bat test` 전체 통과.

## 금지 사항

- verify 의 PAID 가드 분기 안에서 자동 재발급 로직을 넣지 마라. 이유: 사용자 결정 — 운영자 명시 액션만 허용.
- existing 이 expired 인 경우 새 row 를 save 하지 마라 (V80 UNIQUE 위반). 이유: 만료된 구독을 재발급하려면 별 정책 결정 필요. 본 step 은 단순 idempotent 정책.
- PaymentEntity 의 plan 필드 nullable 정책을 변경하지 마라 (옛 단건 결제 호환). 이유: 마이그레이션 영향.

## Status 규칙

- 성공: step 6 `completed`, summary 에 "reissueSubscription 서비스 + 어드민 endpoint + 단위/컨트롤러 테스트 7-8건 추가, 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: existing expired 정책에 사용자 결정 필요 시 `blocked`.
