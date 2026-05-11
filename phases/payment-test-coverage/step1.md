# Step 1 — verify 결제 실패 status 확장 테스트

## 배경

`PaymentServiceTest` 의 기존 `verifyDetectsNonPaidStatus` 는 PortOne status="READY" 만 검증한다. 운영에서 마주칠 다른 실패 status (`FAILED`, `CANCELLED`, `VIRTUAL_ACCOUNT_ISSUED`) 와 PortOne 게이트웨이 5xx 응답이 코드 분기상 차이 없이 동일 흐름인지 회귀 방지로 못박는다.

`PortOneClient.java:98` 주석에 정의된 status 종류: `PAID / CANCELLED / FAILED / READY / VIRTUAL_ACCOUNT_ISSUED ...`. `PaymentService.verify` 는 `info.isPaid()` (status="PAID" 만 true) 분기로 비-PAID 를 모두 동일 처리 → markFailedInNewTx + `PAYMENT_VERIFICATION_FAILED` throw. 본 step 은 테스트만 추가하고 코드는 손대지 않는다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | 케이스 3-4건 추가 |

## 추가할 케이스

1. `verify_status_FAILED_시_PAYMENT_VERIFICATION_FAILED_+_markFailedInNewTx`
   - `PortOnePaymentInfo` status="FAILED"
   - 예외 errorCode 검증 + `failureRecorder.markFailedInNewTx(eq(entityId), contains("FAILED"))` 1회 호출

2. `verify_status_CANCELLED_시_PAYMENT_VERIFICATION_FAILED_+_markFailedInNewTx`
   - status="CANCELLED"
   - 동일 흐름

3. `verify_status_VIRTUAL_ACCOUNT_ISSUED_시_PAYMENT_VERIFICATION_FAILED`
   - 가상계좌 발급 단계 — 아직 미입금. isPaid()==false 라 일반 fail 흐름. (별도 PENDING 처리는 본 phase 외)

4. `verify_PortOne_5xx_시_PAYMENT_GATEWAY_ERROR_throw`
   - `portOneClient.getPayment(paymentId)` 가 `SqldpassException(PAYMENT_GATEWAY_ERROR)` throw 하도록 mock
   - PaymentService.verify 가 그 예외를 그대로 전파하는지 검증
   - 단, markFailedInNewTx 는 호출되지 않음 (게이트웨이 자체 오류라 응답 정보 부재)

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.PaymentServiceTest"
.\gradlew.bat test
```

## Acceptance Criteria

1. 위 4개 케이스가 추가되고 모두 통과.
2. 기존 `verifyDetectsNonPaidStatus` 는 그대로 유지.
3. 전체 `gradlew.bat test` 회귀 없음.
4. 케이스 4(게이트웨이 5xx) 에서는 `failureRecorder.markFailedInNewTx` 호출 0회 검증.

## 금지 사항

- PaymentService 본문을 수정하지 마라. 이유: 본 step 은 회귀 방지 테스트만 추가 — 분기 동일성 확인이 목적.
- 새 ErrorCode 를 추가하지 마라. 이유: 기존 PAYMENT_VERIFICATION_FAILED / PAYMENT_GATEWAY_ERROR 로 충분.
- VIRTUAL_ACCOUNT_ISSUED 의 별도 PENDING 처리 로직을 본 step 에서 만들지 마라. 이유: 가상계좌 결제는 별 phase 의 정책 결정 필요.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "verify 실패 status 4종 테스트 추가 (FAILED/CANCELLED/VIRTUAL_ACCOUNT_ISSUED/5xx), 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: status 종류 추가에 사용자 결정 필요 시 `blocked`.
