# Step 1 — 백엔드 SubscriptionPlan enum + 테스트 보정

## 배경

All Pass(UNLIMITED) 플랜을 평생 → 6개월(180일) 로 전환한다. 운영상 LTV 예측·신규 콘텐츠 약속 부담을 줄이려는 정책 변경. **사용자 결정 사항**:

- enum 이름·DB 컬럼·Play Billing SKU 모두 그대로 (`UNLIMITED` / `iap_unlimited`)
- 가격 29,900원 그대로
- 라벨 "All Pass" 그대로
- **기존 UNLIMITED 구독자(subscription.expires_at=NULL) 는 평생 유지** — 신규 결제부터만 180일 적용

핵심 변경은 `SubscriptionPlan.UNLIMITED` 의 `days` 필드 한 글자 (`null → 180`). 기존 만료 계산 식 `paidAt.toLocalDate().plusDays(plan.getDays() + 1L).atStartOfDay()` 는 그대로 — `isLifetime()` 이 자동으로 `false` 가 되면서 신규 구매는 `paidAt 의 KR 일자 + 181일 00:00 KST` 로 만료된다.

`isLifetime()` 메서드 이름은 의미상 어색해지지만, 호출부 4 곳 모두 "expires_at 을 null 로 저장할지 vs 날짜 계산할지" 분기용이라 의미가 자연스럽게 정합. **새 동작에서 이 메서드는 어디서도 true 를 반환하지 않으며, 사실상 dead-true-branch 이다. 다만 기존 인터페이스 변경 비용 회피 + 향후 다른 lifetime plan 도입 여지를 위해 메서드는 보존한다.**

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionPlan.java` | UNLIMITED days `null → 180`, 클래스 javadoc 갱신 |
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionEntity.java` | 클래스 javadoc 의 "UNLIMITED 는 expiresAt = null" 문장 갱신 |
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | L145 메시지 `"이미 무제한 이용권을 이용 중입니다."` → `"이미 All Pass 를 이용 중입니다."` |
| `backend/src/test/java/com/sqldpass/service/payment/SubscriptionServiceTest.java` | `unlimitedActive` DisplayName/어설션 — UNLIMITED 신규는 expires_at 이 더 이상 null 아님. 단, 본 테스트는 **expires_at=null 인 기존 평생 구독자가 활성으로 잡히는지** 의 회귀 케이스로 유지 (DisplayName 만 명확화). |
| `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java` | `verifyUnlimitedCreatesLifetimeSubscription` (L654) → `verifyUnlimitedCreates6MonthSubscription` 으로 이름·DisplayName·어설션 변경; `reissueSubscription_UNLIMITED_은_expiresAt_null` (L1247) → 어설션 변경; `previewUnlimitedActive` (L370) 어설션은 메시지 변경에 맞게 `.contains("All Pass")` 로 갱신 |

## SubscriptionPlan.java 변경

```diff
-    UNLIMITED(null, true, true,  true, /* allowsPremium */ true);
+    UNLIMITED(180,  true, true,  true, /* allowsPremium */ true);
```

javadoc:
```diff
- * - days = null 이면 평생 (UNLIMITED).
+ * - UNLIMITED 는 180 일 (=6개월). 사용자 노출 라벨은 "All Pass" 유지.
+ * - days = null 인 plan 은 더 이상 없음. 단, 기존 subscription 테이블에 expires_at=NULL
+ *   로 저장된 행은 평생 활성으로 유지 — 활성 판정 (expires_at IS NULL OR > now) 에서
+ *   자동 처리. 새로운 lifetime plan 도입 시에는 days=null 다시 사용 가능.
```

`isLifetime()` 메서드는 이름·시그니처 그대로 유지. 주석은 `days == null 면 true. 현재 모든 plan 이 days 를 가지므로 항상 false; 기존 NULL 행을 정상적으로 다루던 호출부 호환을 위해 보존.`

## SubscriptionEntity.java javadoc

```diff
- * UNLIMITED 는 expiresAt = null (평생).
+ * UNLIMITED 는 paidAt + 181일 00:00 KR 만료 (6개월 정책). 단 기존 데이터에
+ * expiresAt=NULL 로 저장된 평생권은 그대로 유지된다.
```

## PaymentService.java 메시지

L145 `evaluateUpgrade` 분기 — 활성 UNLIMITED 가 있으면 추가 결제 차단. "무제한" 이라는 표현은 사용자 혼동 유발 가능 (이제 6개월 만료).

```diff
-                    "이미 무제한 이용권을 이용 중입니다.");
+                    "이미 All Pass 를 이용 중입니다.");
```

## 테스트 보정 상세

### SubscriptionServiceTest.unlimitedActive (L108-124)

본 테스트는 `expiresAt=null` 인 UNLIMITED 행을 명시적으로 만들어 활성 판정을 검증한다. 이건 **기존 평생 구독자 회귀 케이스** 로 살아남는다. 변경:

```diff
-    @DisplayName("UNLIMITED(All Pass) 활성 → expiresAt=null, allowsPdf=true, premium=true")
+    @DisplayName("UNLIMITED 기존 평생 구독자(expires_at=NULL) → 활성, allowsPdf=true, premium=true")
```
어설션은 그대로 둔다 (`expiresAt()).isNull()` 그대로 — 본 테스트는 데이터셋이 null 인 케이스이므로 유효).

### PaymentServiceTest.verifyUnlimitedCreatesLifetimeSubscription (L653-675)

이제 verify 후 expiresAt 은 null 이 아니라 `paidAt 일자 + 181일 00:00`. paidAt 이 `OffsetDateTime.now()` 라 어설션이 까다로워 — 다음 형태로 단순화:

```java
@Test
@DisplayName("UNLIMITED verify 성공 시 expiresAt = paidAt 일자 + 181일 00:00 (6개월)")
void verifyUnlimitedCreates6MonthSubscription() {
    MemberEntity m = newMember(1L, "pay-rv-7f2a91");
    given(memberRepository.findById(1L)).willReturn(Optional.of(m));

    PaymentEntity entity = new PaymentEntity("p-1", 1L, null, "All Pass",
            SubscriptionPlan.UNLIMITED, 29900);
    given(paymentRepository.findByPaymentId("p-1")).willReturn(Optional.of(entity));

    OffsetDateTime paidOffset = OffsetDateTime.now();
    var info = new PortOneClient.PortOnePaymentInfo("p-1", "PAID", 29900, "KRW",
            paidOffset, Map.of("id", "p-1", "status", "PAID"));
    given(portOneClient.getPayment("p-1")).willReturn(info);

    var result = service.verify(1L, "p-1");

    assertThat(result.plan()).isEqualTo(SubscriptionPlan.UNLIMITED);
    LocalDateTime expectedExpiry = paidOffset
            .atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
            .plusDays(181).atStartOfDay();
    assertThat(result.expiresAt()).isEqualTo(expectedExpiry);

    ArgumentCaptor<SubscriptionEntity> captor = ArgumentCaptor.forClass(SubscriptionEntity.class);
    verify(subscriptionRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getExpiresAt()).isEqualTo(expectedExpiry);
}
```

필요 import: `java.time.ZoneId`, `java.time.LocalDate` 는 기존 import 활용 가능 (이미 다른 테스트가 LocalDateTime 등 사용).

### PaymentServiceTest.reissueSubscription_UNLIMITED_은_expiresAt_null (L1247-1267)

`reissueSubscription` 은 `paidAt.plusDays(plan.getDays())` 공식을 쓴다 (다른 verify 와 달리 `+1` 과 atStartOfDay 없음 — 의도적 차이 또는 누락이지만 본 변경 범위 밖). 어설션 변경:

```java
@Test
@DisplayName("reissueSubscription: UNLIMITED 결제는 paidAt + 180일 만료로 재발급 (6개월)")
void reissueSubscription_UNLIMITED_은_180일_만료() {
    PaymentEntity entity = new PaymentEntity("p-life", 1L, null, "All Pass",
            SubscriptionPlan.UNLIMITED, 29900);
    LocalDateTime paidAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);
    entity.markPaid("{...}", paidAt);
    setField(entity, "id", 88L);
    given(paymentRepository.findById(88L)).willReturn(Optional.of(entity));
    given(subscriptionRepository.findByPaymentId(88L)).willReturn(Optional.empty());

    var result = service.reissueSubscription(88L, 7L);

    LocalDateTime expectedExpiry = paidAt.plusDays(180);
    assertThat(result.issued()).isTrue();
    assertThat(result.expiresAt()).isEqualTo(expectedExpiry);

    ArgumentCaptor<SubscriptionEntity> subCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
    verify(subscriptionRepository, times(1)).save(subCaptor.capture());
    assertThat(subCaptor.getValue().getExpiresAt()).isEqualTo(expectedExpiry);
    assertThat(subCaptor.getValue().getPlan()).isEqualTo(SubscriptionPlan.UNLIMITED);
}
```

### PaymentServiceTest.previewUnlimitedActive (L370-382)

`evaluateUpgrade` 의 메시지 변경에 맞춰:
```diff
-        assertThat(preview.reason()).contains("무제한");
+        assertThat(preview.reason()).contains("All Pass");
```

## 검증

```powershell
cd backend
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test --tests "com.sqldpass.service.payment.*"
.\gradlew.bat test
```

## Acceptance Criteria

1. `SubscriptionPlan.UNLIMITED.getDays() == 180`, `isLifetime() == false`
2. PaymentService.evaluateUpgrade 의 활성 UNLIMITED 차단 메시지가 "이미 All Pass 를 이용 중입니다." 로 변경됨
3. PaymentServiceTest 의 verify/reissue/preview UNLIMITED 테스트 3건이 새 만료 동작에 맞게 통과
4. SubscriptionServiceTest.unlimitedActive 회귀 케이스 통과 (기존 expires_at=NULL 평생 구독자 활성 판정 유지)
5. `./gradlew test` 전체 통과 + `./gradlew compileJava` OK

## 금지 사항

- enum 이름 `UNLIMITED` 를 변경하지 마라. **이유**: DB `subscription.plan` 컬럼에 enum name 그대로 저장됨. 변경 시 기존 행 매핑 실패 + JPA 부팅 실패.
- `PaymentService.verify` 의 만료 계산식 (`paidAt.toLocalDate().plusDays(days + 1L).atStartOfDay()`) 을 만지지 마라. **이유**: 다른 plan(THREE_DAY/FOCUS/ONE_MONTH) 의 만료 동작에도 영향. 사용자 정책 범위 밖.
- `PaymentService.reissueSubscription` (L474) 의 다른 공식 (`paidAt.plusDays(days)`, +1 없음) 을 verify 와 통일하지 마라. **이유**: 의도적 차이 가능성 + 본 step 범위 밖. 별도 phase 에서 검토.
- 신규 Flyway 마이그레이션을 만들지 마라. **이유**: 기존 expires_at=NULL 데이터를 보존해야 평생 구독자 권리 유지.
- 가격(29,900원) / SKU(iap_unlimited) / Play Billing 설정을 변경하지 마라. **이유**: 사용자 결정 — 가격·SKU 유지.

## Status 규칙

- 성공: step 1 `completed`, summary "SubscriptionPlan.UNLIMITED days null→180 + PaymentService 메시지 + 테스트 3건 보정, gradlew test 통과".
- 실패: 3회 재시도 후 `error`.
