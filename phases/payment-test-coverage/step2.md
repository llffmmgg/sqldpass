# Step 2 — prepare 의 amount 변조 방지 명시 테스트

## 배경

`PaymentService.prepare(memberId, plan)` 는 `PrepareRequest` 로 `plan` 만 받고 amount 는 `PaymentProperties.configFor(plan).getAmount()` 에서 가져온다. 즉 client 가 어떤 값을 보내든 백엔드가 자체 계산한 금액으로 PaymentEntity 가 생성된다. 이는 **결제 금액 변조 방어의 1차 라인**이지만 현재 테스트는 prepare 의 amount 계산 정확성만 검증하고 "client 가 의도적으로 다른 금액을 시도하는 시나리오" 를 명시적으로 못박은 케이스가 없다.

본 step 은 회귀 방지 테스트만 추가한다. (코드 변경 0건.)

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | 케이스 2건 추가 |

## 추가할 케이스

1. `prepare_는_properties_의_정가만_사용_PaymentEntity_amount_검증`
   - `properties.setThreeDay(new PlanConfig(3900, "..."))` 로 정가 설정
   - prepare 호출 후 `paymentRepository.save` ArgumentCaptor 로 PaymentEntity 캡쳐
   - `entity.getAmount() == 3900`, `entity.getBaseAmount() == 3900`, `entity.getProrateDiscount() == 0` 검증
   - 활성 구독 없음 (default mock empty)

2. `prepare_업그레이드_시_prorate_차감만_허용_finalAmount_가_baseAmount_미만`
   - ONE_MONTH 활성 + UNLIMITED 결제
   - prepare 결과의 `finalAmount = baseAmount - prorateDiscount` 등식 검증
   - PaymentEntity 의 `amount == finalAmount`, `baseAmount == 정가`, `prorateDiscount > 0` 동시 검증
   - 즉 client 가 amount 를 임의로 못 줄여도 정상 업그레이드 차감은 발생

3. (옵션) `PrepareRequest_record_는_plan_필드만_보유` — 컴파일 시점 회귀 방지. record 의 필드 개수/이름을 reflection 으로 1줄 assert. plan 외 필드(amount, productName 등) 가 추가되면 즉시 실패.

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.PaymentServiceTest"
.\gradlew.bat test
```

## Acceptance Criteria

1. 위 2건(또는 옵션 포함 3건) 케이스가 추가되고 모두 통과.
2. 기존 prepare 테스트와 중복 없도록 — 기존 `prepareUnlimitedUpgradeApplyProrate` 와 검증 방향이 다른 부분(반환값 vs 저장된 Entity 캡쳐)에 초점.
3. 전체 `gradlew.bat test` 회귀 없음.

## 금지 사항

- `PaymentController.PrepareRequest` record 시그니처를 변경하지 마라. 이유: 본 step 은 현 시그니처가 변조 방어를 보장한다는 회귀 방지 — 시그니처 변경은 별 phase 의사 결정.
- PaymentEntity 의 생성자 시그니처를 변경하지 마라. 이유: 동일.

## Status 규칙

- 성공: step 2 `completed`, summary 에 "prepare amount 변조 방어 회귀 방지 테스트 2-3건 추가, 전체 test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: PrepareRequest 시그니처 변경 필요성 발견 시 `blocked`.
