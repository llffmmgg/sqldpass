# Step 2 — 백엔드: PAYMENT_CANCELLED ErrorCode + verify 분기

## 배경

현재 `PaymentService.verify` 는 PortOne status FAILED 와 CANCELLED 를 둘 다 `PAYMENT_VERIFICATION_FAILED` ("결제 검증에 실패했습니다.") 로 throw 한다. 프론트의 `/취소|cancel/i.test(message)` 분기는 메시지에 "취소" 키워드가 있어야 info 톤 토스트 ("결제를 취소하셨습니다.") 를 띄우는데, 현재 메시지로는 매칭 실패 → 사용자 취소도 error 톤 "결제 검증에 실패했습니다." 로 잘못 표시.

Step 1 의 verifiedRef 가드로 토스트 중복은 해결되나, **사용자 취소 시 토스트 톤/문구 정확성** 은 본 step 으로 보강.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/common/ErrorCode.java` | `PAYMENT_CANCELLED` enum 신규 (메시지 "결제가 취소되었습니다.") |
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | `verify` 의 PortOne status 분기 — CANCELLED → PAYMENT_CANCELLED throw, FAILED → 기존 PAYMENT_VERIFICATION_FAILED 유지 |
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | CANCELLED 케이스 어설션을 새 ErrorCode 로 보정 |

## ErrorCode 변경

기존 `PAYMENT_VERIFICATION_FAILED` 옆에 신규 추가:

```java
PAYMENT_CANCELLED(HttpStatus.BAD_REQUEST, "PAYMENT_CANCELLED",
        "결제가 취소되었습니다."),
```

## PaymentService.verify 분기

기존 코드 (대략):
```java
if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)
        || "VIRTUAL_ACCOUNT_ISSUED".equalsIgnoreCase(status)) {
    failureRecorder.markFailed(payment.getId(), "portone:" + status);
    throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
}
```

변경 (실제 구조는 구현 시 재확인):
```java
if ("CANCELLED".equalsIgnoreCase(status)) {
    failureRecorder.markFailed(payment.getId(), "portone:CANCELLED");
    throw new SqldpassException(ErrorCode.PAYMENT_CANCELLED);
}
if ("FAILED".equalsIgnoreCase(status) || "VIRTUAL_ACCOUNT_ISSUED".equalsIgnoreCase(status)) {
    failureRecorder.markFailed(payment.getId(), "portone:" + status);
    throw new SqldpassException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
}
```

`failureRecorder.markFailed` 호출은 양쪽 모두 유지 — payment_failure_log 에 raw status 기록 보존.

## 테스트 보정

`PaymentServiceTest` 의 `verifyFailureStatusCases` (payment-test-coverage step1 에서 추가된 케이스들) 중 CANCELLED 케이스 어설션을 보정:

```diff
- assertThatThrownBy(() -> service.verify(memberId, paymentId))
-     .isInstanceOf(SqldpassException.class)
-     .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_VERIFICATION_FAILED);
+ assertThatThrownBy(() -> service.verify(memberId, paymentId))
+     .isInstanceOf(SqldpassException.class)
+     .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_CANCELLED);
```

(CANCELLED 케이스 테스트 메서드명에 따라 1-2건만 보정. FAILED / VIRTUAL_ACCOUNT_ISSUED / 5xx 케이스는 기존 PAYMENT_VERIFICATION_FAILED 유지.)

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `ErrorCode.PAYMENT_CANCELLED` 가 추가되고 메시지에 "취소" 포함 ("결제가 취소되었습니다.").
2. `PaymentService.verify` 가 PortOne status CANCELLED 시 `PAYMENT_CANCELLED` throw.
3. FAILED / VIRTUAL_ACCOUNT_ISSUED / 5xx 분기는 기존 `PAYMENT_VERIFICATION_FAILED` 유지.
4. `failureRecorder.markFailed` 가 CANCELLED 케이스에서도 호출됨 (감사 로그 유지).
5. PaymentServiceTest 의 CANCELLED 케이스 어설션이 PAYMENT_CANCELLED 로 보정됨.
6. `gradlew test` 전체 통과, `gradlew compileJava` 성공.

## 금지 사항

- 응답에 raw PortOne status (예: "CANCELLED") 텍스트를 그대로 노출하지 마라. **이유**: 사용자 친화 메시지 우선. raw status 는 log + payment_failure_log 에만.
- VIRTUAL_ACCOUNT_ISSUED 분기를 변경하지 마라. **이유**: 가상계좌는 입금 대기 상태로 정상 흐름 일부. 본 step 은 단순 취소/실패만 다룸.
- `failureRecorder.markFailed` 호출을 빼지 마라. **이유**: 감사 로그 누락. CANCELLED 도 결제 실패 통계에 포함되어야 추후 PG 협의/통계 활용 가능.
- 다른 ErrorCode 의 메시지 텍스트를 함께 바꾸지 마라. **이유**: 본 step 은 PAYMENT_CANCELLED 신규 + verify 분기 1줄만.

## Status 규칙

- 성공: step 2 `completed`, summary "PAYMENT_CANCELLED ErrorCode 신규 + verify CANCELLED 분기 + 테스트 어설션 보정, test+compile OK".
- 실패: 3회 재시도 후 `error`.
