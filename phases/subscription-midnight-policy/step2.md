# Step 2 — expiresAt 정책 변경 (paidDate +N+1일 자정)

## 배경

사용자 결정: 결제 시각의 시·분·초 무시, **결제 일자(KR) 기준** + plan.days + 1일 자정에 만료. 결제일 자체 포함 = +1일 보너스.

| Plan | 5/14 결제 시 만료 시각 (KR) |
|---|---|
| Thunder (THREE_DAY=3) | 5/18 00:00 |
| Focus (FOCUS=30) | 6/14 00:00 |
| Pro (ONE_MONTH=30) | 6/14 00:00 |
| All Pass (UNLIMITED) | null |

Step 1 의 JVM TZ=Asia/Seoul 이 적용된 환경에서 `paidAt.toLocalDate()` 가 KR 일자가 되어야 정확. 두 step 같은 배포에 묶여 적용.

## 의존성

- Step 1 (`container-tz-asia-seoul`) 완료 필수.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `service/payment/PaymentService.java` `verify(...)` (L235 부근) | `expiresAt = paidAt.plusDays(plan.getDays())` → `paidAt.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay()` |
| `service/payment/PaymentService.java` `verifyPlayBilling(...)` (L339 부근) | 동일 패턴 |
| `service/payment/AdminSubscriptionService.java` `grantManual(...)` (L81) | `now.plusDays(plan.getDays())` → `now.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay()` |
| `persistent/payment/SubscriptionPlan.java` (javadoc) | days 의 의미를 "결제일 이후 추가 일수, 실제 사용 = (days+1)일치" 로 명시 |
| `test/.../PaymentServiceTest.java` | 기존 `verifyThreeDayCreatesSubscription` 단언 갱신 + 자정 경계/시각 무시/30일 케이스 신규 |
| `test/.../AdminSubscriptionServiceTest.java` (있으면 보강) | grantManual 의 expiresAt 자정 정렬 확인 |

## PaymentService.verify 패치

```java
// 기존
LocalDateTime expiresAt = plan.isLifetime() ? null : paidAt.plusDays(plan.getDays());

// 변경
// 사용자 정책: 결제 시각의 시·분·초 무시, paidAt 의 KR 일자 + (plan.days + 1)일 의 00:00 KR 에 만료.
// 결제일 자체가 사용 가능 일자에 포함되므로 사실상 +1일 보너스.
// 예: KR 5/14 02:00 Thunder → 5/14·5/15·5/16·5/17 사용 → 5/18 00:00 만료.
LocalDateTime expiresAt = plan.isLifetime()
        ? null
        : paidAt.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay();
```

`verifyPlayBilling` 도 같은 한 줄 적용. `AdminSubscriptionService.grantManual` 의 `now.plusDays(plan.getDays())` 도 동일 패턴.

## 테스트 갱신

`PaymentServiceTest.verifyThreeDayCreatesSubscription` 의 단언:
```java
// 기존
assertThat(saved.getExpiresAt()).isEqualToIgnoringSeconds(saved.getPurchasedAt().plusDays(3));

// 변경
assertThat(saved.getExpiresAt()).isEqualTo(
        saved.getPurchasedAt().toLocalDate().plusDays(4).atStartOfDay());
```

신규 테스트 (PaymentServiceTest 또는 별도):
- `verifyThreeDay_시각_늦은오후_여전히_자정만료`: paidAt `2026-05-14T23:50` → expiresAt `2026-05-18T00:00`
- `verifyThreeDay_자정직후_다음날_기준`: paidAt `2026-05-15T00:01` → expiresAt `2026-05-19T00:00`
- `verifyFocus_31일치`: paidAt `2026-05-14T10:00`, plan=FOCUS → expiresAt `2026-06-14T00:00`
- `verifyOneMonth_31일치`: 동일
- `verifyUnlimited_null_회귀`: plan=UNLIMITED → expiresAt null

## 검증

```powershell
cd backend
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test
```

## Acceptance Criteria

1. `PaymentService.verify`, `verifyPlayBilling`, `AdminSubscriptionService.grantManual` 의 expiresAt 계산이 `paidAt.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay()`.
2. 기존 단언 갱신, 신규 테스트 4건 이상 통과.
3. UNLIMITED 회귀 검증 (null).
4. `gradlew test` 전체 통과.

## 금지 사항

- `+ 1L` 을 빼지 마라. **이유**: 사용자 정책상 결제일 포함 N+1일. 빼면 결제일 손해.
- SubscriptionPlan enum 의 `days` 값(3/30/30) 자체를 4/31/31 로 바꾸지 마라. **이유**: 가격/마케팅 표기와 다른 서비스(`isUpgradeFrom` 등) 회귀 위험. 보너스는 service 단에서.
- `paidAt.plusDays(plan.getDays() + 1L)` 만 하지 마라(LocalDate 변환 누락). **이유**: 시각 보존되어 자정 정렬 안 됨 — 사용자 정책 미충족.
- 어드민 expireManual 의 `sub.revoke(now)` 를 자정으로 바꾸지 마라. **이유**: 만료는 "지금 즉시" 의도. 시각 정밀도 필요.

## Status 규칙

- 성공: step 2 `completed`, summary "verify/verifyPlayBilling/grantManual 에 paidDate +N+1일 자정 정책 적용 + 단언 갱신 + 신규 케이스 N건, gradle test OK".
- 실패: 3회 재시도 후 `error`.
