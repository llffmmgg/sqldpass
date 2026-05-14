# Step 1 — 백엔드: `SubscriptionPlan.allowsPremium` + 권한 분기

## 배경

`thunder-focus-paywall` phase 의 권한 매트릭스에서 Focus 는 PASS+ 회차 접근 ❌ 로 정의됐다. 그러나 enum/서비스에 PASS+ 분기가 도입되지 않아 Focus 사용자도 PASS+ 회차에 접근 가능 — 권한 누수 버그.

- `SubscriptionPlan.java` L6 주석: *"모든 plan 은 PREMIUM 풀이를 허용한다 (allowsPremium = true 고정)"* — 매트릭스 이전 가정.
- `SubscriptionService.java` L70-72: `getActive(memberId).isPresent()` — plan 무관, 활성 구독만 보면 통과.
- `MockExamService.getForUser` L179 단일 게이트가 `hasPremiumAccess(memberId)` 만 호출하므로 service 단에서 분기하면 모든 진입 경로 동시 해결.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `persistent/payment/SubscriptionPlan.java` | enum 시그니처에 `allowsPremium` boolean 추가. FOCUS=false, 나머지 유료=true. `isAllowsPremium()` getter. 주석을 phase 매트릭스 기준으로 갱신. |
| `service/payment/SubscriptionService.java` | `hasPremiumAccess` 가 `getActive().map(ActiveSubscription::allowsPremium).orElse(false)` 로 분기. `ActiveSubscription` record 에 `allowsPremium` 필드 추가. `getActive()` 매핑부 + DEV_BYPASS_SUBSCRIPTION 분기에도 새 필드 채움(UNLIMITED 시뮬레이션이므로 true). |
| `test/.../SubscriptionServiceTest.java` (신규 또는 보강) | hasPremiumAccess: FOCUS → false, THREE_DAY/ONE_MONTH/UNLIMITED → true, 비구독 → false. 회귀: hasLibraryAccess/removesAds 는 Focus 도 true 유지. |

## SubscriptionPlan 변경

```java
public enum SubscriptionPlan {
    THREE_DAY(3,    true, false, true, /* allowsPremium */ true,  1),
    FOCUS    (30,   true, false, true, /* allowsPremium */ false, 2),
    ONE_MONTH(30,   true, false, true, /* allowsPremium */ true,  3),
    UNLIMITED(null, true, true,  true, /* allowsPremium */ true,  4);

    private final Integer days;
    private final boolean removesAds;
    private final boolean allowsPdf;
    private final boolean hasLibraryAccess;
    /** PASS+ (premium) 회차 풀이 허용. Focus 만 false — paywall 정책. */
    private final boolean allowsPremium;
    private final int tier;

    SubscriptionPlan(Integer days, boolean removesAds, boolean allowsPdf,
                     boolean hasLibraryAccess, boolean allowsPremium, int tier) {
        this.days = days;
        this.removesAds = removesAds;
        this.allowsPdf = allowsPdf;
        this.hasLibraryAccess = hasLibraryAccess;
        this.allowsPremium = allowsPremium;
        this.tier = tier;
    }

    public boolean isAllowsPremium() { return allowsPremium; }
    // ... 기존 getter 그대로
}
```

클래스 javadoc 의 *"모든 plan 은 PREMIUM 풀이를 허용한다"* 한 줄은 *"removesAds/allowsPdf/hasLibraryAccess/allowsPremium 은 plan 별 차등"* 로 수정.

## SubscriptionService 변경

```java
public boolean hasPremiumAccess(Long memberId) {
    return getActive(memberId)
            .map(ActiveSubscription::allowsPremium)
            .orElse(false);
}

public Optional<ActiveSubscription> getActive(Long memberId) {
    // ... 기존 로직
    return Optional.of(new ActiveSubscription(
            top.getPlan(), top.getExpiresAt(),
            top.getPlan().isRemovesAds(), top.getPlan().isAllowsPdf(),
            top.getPlan().isHasLibraryAccess(),
            top.getPlan().isAllowsPremium()));
}

public record ActiveSubscription(
        SubscriptionPlan plan,
        LocalDateTime expiresAt,
        boolean removesAds,
        boolean allowsPdf,
        boolean hasLibraryAccess,
        boolean allowsPremium
) {}
```

DEV_BYPASS_SUBSCRIPTION 분기(L55-58) 도 6번째 인자로 `true` 추가 — UNLIMITED 시뮬레이션이므로 PASS+ 허용 유지.

## 테스트 (`SubscriptionServiceTest`)

기존 파일 없으면 신규. Mockito + JUnit 5. `SubscriptionRepository` 만 mock, `SubscriptionHistoryService` 도 mock.

```java
@Test
void hasPremiumAccess_FOCUS_false() {
    SubscriptionEntity focus = new SubscriptionEntity(
            7L, SubscriptionPlan.FOCUS, 100L,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(29));
    given(subscriptionRepository.findActiveByMemberId(eq(7L), any()))
            .willReturn(List.of(focus));

    assertThat(service.hasPremiumAccess(7L)).isFalse();
}

@Test
void hasPremiumAccess_THREE_DAY_true() { /* ... */ }
@Test
void hasPremiumAccess_ONE_MONTH_true() { /* ... */ }
@Test
void hasPremiumAccess_UNLIMITED_true() { /* ... */ }
@Test
void hasPremiumAccess_노구독_false() { /* ... */ }

@Test
void hasLibraryAccess_Focus_true() { /* 회귀 — Focus 도 라이브러리 권한은 가짐 */ }
```

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `SubscriptionPlan.FOCUS.isAllowsPremium()` == false, 다른 3 plan 은 true.
2. `SubscriptionService.hasPremiumAccess(focusMember)` == false. 다른 plan 회원은 true. 비구독 false.
3. `ActiveSubscription.allowsPremium` 필드 노출. `getActive` 매핑 정상.
4. DEV_BYPASS_SUBSCRIPTION 분기 컴파일 통과(`allowsPremium=true`).
5. `hasLibraryAccess(focusMember)` == true (회귀 — Focus 의 오답노트 권한 유지).
6. 테스트 신규 5건 이상 통과.
7. `gradlew test` 전체 통과, `gradlew compileJava` 통과.

## 금지 사항

- enum name(THREE_DAY/FOCUS/ONE_MONTH/UNLIMITED)을 바꾸지 마라. **이유**: DB `subscription.plan` 컬럼에 enum name 으로 저장 — 변경 시 운영 데이터 깨짐.
- `MockExamService.getForUser` 의 게이트 위치를 옮기지 마라. **이유**: 단일 게이트라 본 수정 면적 최소. 추가 진입점은 회귀 위험.
- DEV_BYPASS_SUBSCRIPTION 분기에서 `allowsPremium=false` 로 두지 마라. **이유**: UNLIMITED 시뮬레이션 — true 유지가 일관성.
- 다른 권한(removesAds/allowsPdf/hasLibraryAccess) enum 값을 건드리지 마라. **이유**: phase 매트릭스와 이미 일치. 본 phase 는 PASS+ 만 수정.
- 새 필드 이름을 `allowsPassPlus` 같은 변형으로 쓰지 마라. **이유**: `MockExam.isPremium()`, `MOCK_EXAM_LOCKED`, `MockExamVisibility.PREMIUM` 등 기존 코드가 모두 "PREMIUM" 어휘 — 일관성.

## Status 규칙

- 성공: step 1 `completed`, summary "SubscriptionPlan.allowsPremium(FOCUS=false) + SubscriptionService.hasPremiumAccess 분기 + ActiveSubscription record 필드 + 회귀 테스트 N건, test/compile OK".
- 실패: 3회 재시도 후 `error`.
