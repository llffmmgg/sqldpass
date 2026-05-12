# Step 1 — application.yaml product-name 3개 + 테스트 보정

## 배경

영수증 메일에 표시되는 상품명을 /checkout 요금제 카드 표기와 일치. 사용자가 카드에서 "Pro" 본 뒤 영수증에 "문어CBT 한달 이용권" 이 와서 일관성 부족. 새 값: `문어CBT Starter / Pro / Lifetime`.

`PlanCard` UI 는 이미 가격 바로 아래에 CTA, 그 다음 features(설명) — Supabase 톤. 추가 재배치 불필요.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/resources/application.yaml` (L125-133) | three-day/one-month/unlimited 의 `product-name` 기본값 3개 |
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | 하드코딩된 상품명 어설션 보정 + setUp 의 PlanConfig 초기값 보정 |

## application.yaml 변경

```diff
  three-day:
    amount: ${PAYMENT_3DAY_AMOUNT:3900}
-   product-name: ${PAYMENT_3DAY_NAME:문어CBT 3일 이용권}
+   product-name: ${PAYMENT_3DAY_NAME:문어CBT Starter}
  one-month:
    amount: ${PAYMENT_1MONTH_AMOUNT:9900}
-   product-name: ${PAYMENT_1MONTH_NAME:문어CBT 한달 이용권}
+   product-name: ${PAYMENT_1MONTH_NAME:문어CBT Pro}
  unlimited:
    amount: ${PAYMENT_UNLIMITED_AMOUNT:29900}
-   product-name: ${PAYMENT_UNLIMITED_NAME:문어CBT 평생 무제한 이용권}
+   product-name: ${PAYMENT_UNLIMITED_NAME:문어CBT Lifetime}
```

env override 변수명(PAYMENT_3DAY_NAME 등) 은 유지 — 운영자가 운영 환경에서 별도 값 주입 가능.

## PaymentService 가드 검증

`PaymentService.java:91-95`:
```java
if (productName == null || productName.isBlank() || productName.toUpperCase().contains("TEST")) {
    throw ...;
}
```

- `"문어CBT Starter"`.toUpperCase() → `"문어CBT STARTER"` → contains("TEST") = false ✓
- `"문어CBT Pro"`.toUpperCase() → `"문어CBT PRO"` → contains("TEST") = false ✓
- `"문어CBT Lifetime"`.toUpperCase() → `"문어CBT LIFETIME"` → contains("TEST") = false ✓

가드 통과 — 변경 안전.

## PaymentServiceTest 보정

`setUp()` 의 PaymentProperties.PlanConfig 초기값:
```diff
- properties.setThreeDay(new PaymentProperties.PlanConfig(3900, "문어CBT 3일 이용권"));
- properties.setOneMonth(new PaymentProperties.PlanConfig(9900, "문어CBT 한달 이용권"));
- properties.setUnlimited(new PaymentProperties.PlanConfig(29900, "문어CBT 평생 무제한 이용권"));
+ properties.setThreeDay(new PaymentProperties.PlanConfig(3900, "문어CBT Starter"));
+ properties.setOneMonth(new PaymentProperties.PlanConfig(9900, "문어CBT Pro"));
+ properties.setUnlimited(new PaymentProperties.PlanConfig(29900, "문어CBT Lifetime"));
```

그리고 어설션에 하드코딩된 곳:
```diff
- assertThat(result.productName()).isEqualTo("문어CBT 한달 이용권");
+ assertThat(result.productName()).isEqualTo("문어CBT Pro");
```

grep 으로 전부 찾아서 일괄 보정.

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. application.yaml 의 product-name 3개가 새 값으로 변경됨.
2. env override 변수명(PAYMENT_3DAY_NAME 등) 유지.
3. PaymentServiceTest 의 setUp + 어설션 보정 완료.
4. `gradlew test` 전체 통과 + compileJava OK.

## 금지 사항

- "TEST" 가드 코드를 변경하지 마라. **이유**: 카드사 심사 정책 유지.
- 기존 PaymentEntity row 의 product_name 을 UPDATE 마이그레이션으로 바꾸지 마라. **이유**: 회계 보존 정책. 결제 시점 캡처가 영구 기록.
- amount 값을 함께 바꾸지 마라. **이유**: 본 plan 은 상품명만.
- PlanCard / CheckoutLanding UI 를 변경하지 마라. **이유**: 이미 CTA→features 순서. 추가 변경 불필요.

## Status 규칙

- 성공: step 1 `completed`, summary "application.yaml product-name 3개 → 문어CBT Starter/Pro/Lifetime + PaymentServiceTest setUp/어설션 보정, test+compile OK".
- 실패: 3회 재시도 후 `error`.
