# Step 1 — 백엔드: supersede 가드 + Projection 확장

## 배경

업그레이드 회원의 payment 행이 모두 PAID + PORTONE 으로 admin/refunds 에 표시 → 옛 결제 환불 사고 위험. 백엔드에서 차단 + Projection 에 supersededByNewerPayment 필드 추가로 UI 도 disable.

판별: 같은 회원의 더 최신(`paid_at` 비교) PAID 결제 중 subscription 이 활성(`expires_at IS NULL OR > now`) 인 것이 존재하면 superseded.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `service/common/ErrorCode.java` | `PAYMENT_SUPERSEDED_BY_NEWER` 추가 (HttpStatus.BAD_REQUEST + "이 결제는 이후 업그레이드 결제로 대체됐어요. 현재 활성 결제부터 환불해주세요.") |
| `controller/admin/AdminPaymentRow.java` | record 마지막에 `Boolean supersededByNewerPayment` 추가 |
| `persistent/payment/PaymentRepository.java` | `existsNewerActivePaidPaymentForMember(memberId, paymentId, paidAt)` 메서드 신규. `findAdminPage` JPQL 의 SELECT new 절에 `CASE WHEN EXISTS(...) THEN TRUE ELSE FALSE END` 추가 |
| `service/payment/PaymentService.java` | `revokePortOnePayment` 의 CANCELLED idempotent 가드 다음에 superseded 가드 추가 |
| 테스트 | `PaymentServiceTest` 2건 (superseded 거절 / 최신 정상 환불), `AdminPaymentControllerTest` 의 list 케이스에 supersededByNewerPayment=false 추가 |

## ErrorCode 추가

PAYMENT_GATEWAY_ERROR 옆 결제 그룹에 추가:

```java
PAYMENT_SUPERSEDED_BY_NEWER(HttpStatus.BAD_REQUEST, "PAYMENT_SUPERSEDED_BY_NEWER",
        "이 결제는 이후 업그레이드 결제로 대체됐어요. 현재 활성 결제부터 환불해주세요."),
```

## AdminPaymentRow 확장

```java
public record AdminPaymentRow(
        Long id, String paymentId, Long memberId, String nickname,
        SubscriptionPlan plan, Integer amount, Integer baseAmount, Integer prorateDiscount,
        PaymentStatus status, PaymentProvider provider,
        String buyerName, String buyerEmail, String buyerPhoneNumber,
        LocalDateTime paidAt, LocalDateTime createdAt,
        Boolean supersededByNewerPayment
) {}
```

## PaymentRepository 변경

신규 메서드 (가드 전용):

```java
@Query("""
        SELECT CASE WHEN COUNT(p2) > 0 THEN true ELSE false END
        FROM PaymentEntity p2
        WHERE p2.memberId = :memberId
          AND p2.id <> :paymentId
          AND p2.status = com.sqldpass.persistent.payment.PaymentStatus.PAID
          AND p2.paidAt > :paidAt
          AND EXISTS (
              SELECT 1 FROM SubscriptionEntity s
              WHERE s.paymentId = p2.id
                AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
          )
        """)
boolean existsNewerActivePaidPaymentForMember(@Param("memberId") Long memberId,
                                              @Param("paymentId") Long paymentId,
                                              @Param("paidAt") LocalDateTime paidAt);
```

`findAdminPage` SELECT 절에 추가:

```java
@Query("""
        SELECT new com.sqldpass.controller.admin.AdminPaymentRow(
                p.id, p.paymentId, p.memberId, m.nickname,
                p.plan, p.amount, p.baseAmount, p.prorateDiscount,
                p.status, p.provider,
                p.buyerName, p.buyerEmail, p.buyerPhoneNumber,
                p.paidAt, p.createdAt,
                CASE WHEN EXISTS (
                    SELECT 1 FROM PaymentEntity p2
                    WHERE p2.memberId = p.memberId
                      AND p2.id <> p.id
                      AND p2.status = com.sqldpass.persistent.payment.PaymentStatus.PAID
                      AND p2.paidAt > p.paidAt
                      AND EXISTS (
                          SELECT 1 FROM SubscriptionEntity s
                          WHERE s.paymentId = p2.id
                            AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
                      )
                ) THEN TRUE ELSE FALSE END
        )
        FROM PaymentEntity p LEFT JOIN MemberEntity m ON m.id = p.memberId
        WHERE (:status IS NULL OR p.status = :status)
          ...
```

JPQL 매핑 실패 시 fallback: findAdminPage 응답 받은 후 controller layer 에서 후처리로 `existsNewerActivePaidPaymentForMember` 호출 (N+1 허용, page size 20 라 무시 가능). 본 step acceptance 는 "응답에 필드 포함" 만 요구 — 구현 자유.

## PaymentService.revokePortOnePayment 가드 추가

```diff
  public void revokePortOnePayment(Long paymentEntityId, String reason, Long actorAdminId) {
      PaymentEntity entity = paymentRepository.findById(paymentEntityId)
              .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
      if (entity.getProvider() != PaymentProvider.PORTONE) {
          throw new SqldpassException(ErrorCode.INVALID_INPUT,
                  "PortOne 결제만 본 메서드로 환불 가능합니다.");
      }
      if (entity.getStatus() == PaymentStatus.CANCELLED) {
          return; // idempotent
      }
+     // 업그레이드로 superseded 된 결제 환불 차단 — 옛 결제 환불 시 활성 구독은 최신 결제에
+     // 묶여있어 사용자가 환불 + 서비스 이용 동시 발생. paidAt 이 null 이면 아직 PAID 가
+     // 아니라 가드 위쪽에서 다른 경로로 처리됨.
+     if (entity.getPaidAt() != null && paymentRepository.existsNewerActivePaidPaymentForMember(
+             entity.getMemberId(), entity.getId(), entity.getPaidAt())) {
+         log.warn("superseded payment 환불 거절 paymentId={} memberId={} actorAdminId={}",
+                 entity.getPaymentId(), entity.getMemberId(), actorAdminId);
+         throw new SqldpassException(ErrorCode.PAYMENT_SUPERSEDED_BY_NEWER);
+     }
      portOneClient.cancel(entity.getPaymentId(), reason);
      ...
```

## 테스트

`PaymentServiceTest`:
- `revokePortOnePayment_superseded_시_PAYMENT_SUPERSEDED_BY_NEWER_throw_cancel_미호출`
- `revokePortOnePayment_최신_결제는_정상_환불`

`AdminPaymentControllerTest`:
- `list_기본_정상` 의 AdminPaymentRow 생성 시 supersededByNewerPayment=false 추가 + jsonPath 어설션 1줄

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `ErrorCode.PAYMENT_SUPERSEDED_BY_NEWER` 추가.
2. `AdminPaymentRow` 에 `supersededByNewerPayment` 필드 추가.
3. `PaymentRepository.existsNewerActivePaidPaymentForMember` 메서드 신규.
4. `findAdminPage` 응답에 supersededByNewerPayment 필드 포함.
5. `revokePortOnePayment` 가 superseded 결제 시 throw + PG cancel 호출 안 함 + history/save 0회.
6. 테스트 신규 2건 통과 + 기존 list 케이스 어설션 보정.
7. `gradlew test` 전체 통과 + `compileJava` OK.

## 금지 사항

- paidAt 비교를 `createdAt` 으로 바꾸지 마라. **이유**: PAID 시점이 진짜 결제 유효 시작. createdAt 은 prepare 시점이라 PENDING/FAILED 가 PAID 보다 먼저 생성된 케이스에서 잘못 매칭.
- 가드를 PG cancel 호출 이후로 옮기지 마라. **이유**: 사고 차단 핵심. PG cancel 전에 거절해야 환불 비용/추적 사고 0.
- 강제 환불 옵션(`force=true`) 추가하지 마라. **이유**: 본 plan 은 차단 우선. 강제 흐름은 별도 phase 의 audit/권한 강화와 함께.
- subscription 정책 변경 (업그레이드 시 기존 expire) 하지 마라. **이유**: 정합성 근본 해결은 별도 phase. 본 step 은 환불 가드만.

## Status 규칙

- 성공: step 1 `completed`, summary "PAYMENT_SUPERSEDED_BY_NEWER + existsNewerActivePaidPaymentForMember + findAdminPage projection 확장 + revokePortOnePayment 가드 + 테스트 보강, test+compile OK".
- 실패: 3회 재시도 후 `error`. JPQL EXISTS 매핑 실패 시 controller 후처리 fallback.
