# Step 4 — PortOne 환불 service + 어드민 endpoint

## 배경

`PortOneClient.cancel(paymentId, reason)` 는 존재하지만 (`PortOneClient.java:67`) 호출자가 없다. 운영 시 PortOne 결제 환불은 수동 DB 조작이 아니라 PG 까지 도달해야 정상.

해결: `PaymentService.revokePortOnePayment(...)` 와 어드민 endpoint 신설. payment-hardening Step 7 에서 만든 `SubscriptionHistoryService` + `SubscriptionService.revokeByPaymentId` 흐름을 재사용해 history REVOKED/REFUNDED 자동 기록.

Play Billing 측 환불(`revokePlayBillingByToken`) 은 이미 RTDN 자동 처리되니 본 step 은 **PortOne 채널 전용**.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개 + 신규 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | `revokePortOnePayment` 메서드 추가 |
| `backend/src/main/java/com/sqldpass/controller/admin/AdminPaymentController.java` | 신규 — 환불 endpoint |

신규 테스트 1개 + 수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/controller/admin/AdminPaymentControllerTest.java` | 신규 mockMvc 테스트 |
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | revokePortOnePayment 단위 케이스 추가 |

## PaymentService.revokePortOnePayment 시그니처

```java
@Transactional
public void revokePortOnePayment(Long paymentEntityId, String reason, Long actorAdminId) {
    PaymentEntity entity = paymentRepository.findById(paymentEntityId)
            .orElseThrow(() -> new SqldpassException(ErrorCode.PAYMENT_NOT_FOUND));
    if (entity.getProvider() != PaymentProvider.PORTONE) {
        throw new SqldpassException(ErrorCode.INVALID_INPUT, "PortOne 결제만 본 메서드로 환불 가능합니다.");
    }
    if (entity.getStatus() == PaymentStatus.CANCELLED) {
        return; // idempotent
    }
    // PG 측 환불 — 실패 시 SqldpassException(PAYMENT_GATEWAY_ERROR) throw → 트랜잭션 롤백
    portOneClient.cancel(entity.getPaymentId(), reason);
    entity.markCancelled("admin-refund:" + (reason == null ? "" : reason));
    // 구독 회수 + history REVOKED (SubscriptionService.revokeByPaymentId 가 이미 history 기록)
    subscriptionService.revokeByPaymentId(entity.getId());
    // 추가로 REFUNDED action 으로 별도 history 한 줄 (REVOKED 가 자동 기록되지만 환불 행위의 명시 기록)
    historyService.record(entity.getMemberId(), entity.getPlan(),
            SubscriptionHistoryAction.REFUNDED, reason, actorAdminId, entity.getId());
}
```

생성자 주입에 `SubscriptionService subscriptionService` 가 이미 있는지 확인 — 없으면 추가. (현재 PaymentService 는 SubscriptionRepository 만 주입받고 SubscriptionService 미주입. 순환 의존 가능성 확인 — SubscriptionService 는 SubscriptionRepository 만 주입받고 PaymentService 미참조 → 순환 없음.)

대안: SubscriptionService 주입 대신 본 메서드 안에서 `subscriptionRepository.findByPaymentId(...).ifPresent(s -> s.revoke(now))` 인라인. 더 가볍지만 history REVOKED 도 직접 호출해야. 가독성 위해 SubscriptionService 위임 권장.

## AdminPaymentController 신설

```java
@Tag(name = "관리자 결제", description = "관리자 환불·재발급 등")
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "PortOne 결제 환불 (관리자) — PG 취소 + 구독 회수 + history REFUNDED")
    public RefundResponse refund(@PathVariable Long paymentId,
                                 @RequestBody RefundRequest body,
                                 HttpServletRequest request) {
        Long actorAdminId = (Long) request.getAttribute("memberId");
        if (actorAdminId == null) throw new SqldpassException(ErrorCode.UNAUTHORIZED);
        String reason = body == null ? null : body.reason();
        paymentService.revokePortOnePayment(paymentId, reason, actorAdminId);
        return new RefundResponse(paymentId, "refunded");
    }

    public record RefundRequest(String reason) {}
    public record RefundResponse(Long paymentId, String status) {}
}
```

`/api/admin/payments/**` 는 `WebMvcConfig` 의 `adminAuthInterceptor` (`/api/admin/**`) 에 자동 포함되므로 추가 인터셉터 등록 불필요.

## 단위 테스트 (PaymentServiceTest)

1. `revokePortOnePayment_정상_PG_cancel_호출_+_PaymentEntity_CANCELLED_+_revokeByPaymentId_+_history_REFUNDED`
   - PaymentEntity (provider=PORTONE, status=PAID) mock
   - `portOneClient.cancel` mock (void)
   - 호출 검증: cancel 1회 + entity.markCancelled + subscriptionService.revokeByPaymentId 1회 + historyService.record(REFUNDED) 1회

2. `revokePortOnePayment_PG_5xx_시_PAYMENT_GATEWAY_ERROR_그대로_throw_+_상태_미변경`
   - `portOneClient.cancel` 가 `SqldpassException(PAYMENT_GATEWAY_ERROR)` throw
   - 검증: 예외 propagation + `entity.markCancelled` 미호출 (트랜잭션 롤백이라 사실상 PaymentEntity 변경 무효)
   - `subscriptionService.revokeByPaymentId` 호출 0회

3. `revokePortOnePayment_이미_CANCELLED_상태면_idempotent_no_op`
   - status=CANCELLED → 메서드 즉시 return
   - cancel/revoke/history 호출 0회

4. `revokePortOnePayment_PLAY_BILLING_provider_면_INVALID_INPUT_throw`
   - entity.provider=PLAY_BILLING → throw, cancel 호출 0회

## 컨트롤러 테스트 (AdminPaymentControllerTest)

- `@WebMvcTest(AdminPaymentController.class)` + `@MockBean PaymentService`
- `refund_정상_200_및_service_호출_검증` — actorAdminId 추출 + 인자 전달
- `refund_미인증_시_401` — request attribute memberId 없을 때
- `refund_body_없어도_reason_null_로_service_호출`

mock 인증 인터셉터는 직접 mock 또는 `@AutoConfigureMockMvc` + 가짜 인터셉터 헬퍼 사용. 본 프로젝트의 다른 admin 컨트롤러 테스트 패턴 그대로 따른다 (기존 admin 컨트롤러 테스트가 있다면 참고).

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.*" --tests "com.sqldpass.controller.admin.*"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `PaymentService.revokePortOnePayment(Long, String, Long)` 가 추가되고 PG cancel + 구독 회수 + history REFUNDED 까지 한 트랜잭션에서 처리.
2. `AdminPaymentController.refund` endpoint 신설 (POST /api/admin/payments/{paymentId}/refund).
3. `WebMvcConfig` 의 adminAuthInterceptor 매핑이 `/api/admin/**` 라 자동 포함됨을 확인 (추가 변경 0).
4. 단위 테스트 4건 + 컨트롤러 테스트 3건 추가 + 모두 통과.
5. `gradlew.bat test` 전체 통과.

## 금지 사항

- Play Billing 환불은 본 step 에서 손대지 마라. 이유: RTDN 자동 처리 흐름 (`revokePlayBillingByToken`) 이 이미 존재 + Google API 환불은 별 권한 필요.
- 어드민 컨트롤러를 `/api/admin/subscriptions/...` 같은 기존 경로에 합치지 마라. 이유: 결제 환불은 결제 도메인 액션 — `AdminSubscriptionController` 와 책임 분리.
- `revokePortOnePayment` 안에서 PortOne 응답을 파싱해 추가 검증 하지 마라. 이유: `PortOneClient.cancel` 가 이미 `onStatus(isError)` 처리 — 실패 시 SqldpassException throw 로 충분.

## Status 규칙

- 성공: step 4 `completed`, summary 에 "revokePortOnePayment 서비스 + 어드민 환불 endpoint + 단위/컨트롤러 테스트 7건 추가, 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: SubscriptionService 주입 순환 의존 발생 시 `blocked`.
