# Step 3 — DailyUsageService (MySQL atomic UPSERT)

## 배경

무료 회원의 일일 사용량을 검사·증가하는 서비스. **핵심: 동시 요청 race condition 방어 + 활성 구독자 면제.**

- DB: MySQL → PostgreSQL 의 `ON CONFLICT ... RETURNING` 미지원
- 패턴: MySQL native `INSERT ... ON DUPLICATE KEY UPDATE` + 그 직후 `SELECT` (같은 트랜잭션) → 새 카운트 확인
- 한도 초과 시 `QuotaExceededException` throw → `@Transactional` 롤백 → 카운트 미증가
- 활성 구독자(`SubscriptionService.getActive(memberId).isPresent()`)는 UPSERT 자체를 스킵 → daily_usage 에 row 생성 안 됨

**중요: `hasPremiumAccess()` 가 아니라 `getActive().isPresent()` 사용.** 이유: Focus 도 활성 구독이면 일일 한도 면제. `hasPremiumAccess()` 는 Focus 를 제외하는 PASS+ 한정 권한이라 부적합.

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 파일 3개:

| 파일 | 목적 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/usage/DailyUsageService.java` | 검사·증가 서비스 |
| `backend/src/main/java/com/sqldpass/service/usage/QuotaExceededException.java` | 한도 초과 예외 (step 4 에서 advice 변환) |
| `backend/src/test/java/com/sqldpass/service/usage/DailyUsageServiceTest.java` | 단위·동시성 테스트 |

기존 파일 수정 1개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/persistent/usage/DailyUsageRepository.java` | native UPSERT 쿼리 + KST today 의 row SELECT 추가 |

## 한도 상수

```java
public static final int DAILY_QUESTION_LIMIT = 30;
public static final int DAILY_MOCK_SESSION_LIMIT = 1;
public static final ZoneId KST = ZoneId.of("Asia/Seoul");
```

## DailyUsageRepository — native UPSERT

```java
@Modifying
@Query(value = """
    INSERT INTO daily_usage (member_id, usage_date, question_count, mock_session_count, created_at, updated_at)
    VALUES (:memberId, :usageDate, :questionDelta, :mockDelta, NOW(6), NOW(6))
    ON DUPLICATE KEY UPDATE
        question_count = question_count + VALUES(question_count),
        mock_session_count = mock_session_count + VALUES(mock_session_count),
        updated_at = NOW(6)
""", nativeQuery = true)
int upsertAndAdd(@Param("memberId") Long memberId,
                 @Param("usageDate") LocalDate usageDate,
                 @Param("questionDelta") int questionDelta,
                 @Param("mockDelta") int mockDelta);
```

UPSERT 자체는 `RETURNING` 이 없으므로 직후 같은 트랜잭션에서 SELECT — `findByMemberIdAndUsageDate` 재사용. MySQL 의 row-level lock 하에 ON DUPLICATE KEY UPDATE 가 atomic 이라 race condition 없음.

## DailyUsageService 작성 가이드

```java
@Service
@RequiredArgsConstructor
public class DailyUsageService {

    public static final int DAILY_QUESTION_LIMIT = 30;
    public static final int DAILY_MOCK_SESSION_LIMIT = 1;
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyUsageRepository repository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void consumeQuestion(Long memberId, int delta) {
        if (memberId == null) return;  // 비로그인은 별도 IP 쿼터 시스템(V62)에서 처리
        if (subscriptionService.getActive(memberId).isPresent()) return;  // 활성 구독자 면제

        LocalDate today = LocalDate.now(KST);
        repository.upsertAndAdd(memberId, today, delta, 0);
        DailyUsageEntity row = repository.findByMemberIdAndUsageDate(memberId, today)
                .orElseThrow(); // UPSERT 직후라 반드시 존재
        if (row.getQuestionCount() > DAILY_QUESTION_LIMIT) {
            throw new QuotaExceededException(
                "DAILY_QUESTION_LIMIT",
                row.getQuestionCount(),
                DAILY_QUESTION_LIMIT,
                nextResetAt(today)
            );
        }
    }

    @Transactional
    public void consumeMockSession(Long memberId) {
        if (memberId == null) return;
        if (subscriptionService.getActive(memberId).isPresent()) return;

        LocalDate today = LocalDate.now(KST);
        repository.upsertAndAdd(memberId, today, 0, 1);
        DailyUsageEntity row = repository.findByMemberIdAndUsageDate(memberId, today)
                .orElseThrow();
        if (row.getMockSessionCount() > DAILY_MOCK_SESSION_LIMIT) {
            throw new QuotaExceededException(
                "DAILY_MOCK_LIMIT",
                row.getMockSessionCount(),
                DAILY_MOCK_SESSION_LIMIT,
                nextResetAt(today)
            );
        }
    }

    @Transactional(readOnly = true)
    public Quota getQuota(Long memberId) {
        if (memberId == null || subscriptionService.getActive(memberId).isPresent()) {
            return Quota.unlimited(nextResetAt(LocalDate.now(KST)));
        }
        LocalDate today = LocalDate.now(KST);
        var row = repository.findByMemberIdAndUsageDate(memberId, today);
        int q = row.map(DailyUsageEntity::getQuestionCount).orElse(0);
        int m = row.map(DailyUsageEntity::getMockSessionCount).orElse(0);
        return new Quota(q, DAILY_QUESTION_LIMIT, m, DAILY_MOCK_SESSION_LIMIT, nextResetAt(today));
    }

    private LocalDateTime nextResetAt(LocalDate today) {
        // KST 익일 00:00. 메모리 project_kst_naive_serialization — naive LocalDateTime, 프론트에서 +09:00 부착
        return today.plusDays(1).atStartOfDay();
    }

    public record Quota(int questionUsed, Integer questionLimit,
                        int mockUsed, Integer mockLimit,
                        LocalDateTime resetAt) {
        public static Quota unlimited(LocalDateTime resetAt) {
            return new Quota(0, null, 0, null, resetAt);
        }
    }
}
```

## QuotaExceededException 작성

```java
package com.sqldpass.service.usage;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class QuotaExceededException extends RuntimeException {
    private final String code;     // DAILY_QUESTION_LIMIT | DAILY_MOCK_LIMIT
    private final int used;
    private final int limit;
    private final LocalDateTime resetAt;

    public QuotaExceededException(String code, int used, int limit, LocalDateTime resetAt) {
        super(code);
        this.code = code;
        this.used = used;
        this.limit = limit;
        this.resetAt = resetAt;
    }
}
```

## 테스트 (필수)

`DailyUsageServiceTest` — `@SpringBootTest` 또는 `@DataJpaTest + @Import` 슬라이스. 메모리 H2 또는 testcontainers MySQL. 기존 결제 테스트(`payment-test-coverage` 산출물)에서 사용한 패턴 확인 후 동일.

필수 시나리오:

1. `consumeQuestion(memberId, 20)` 후 잔여 = 10
2. `consumeQuestion(memberId, 20)` 두 번 호출 시 두 번째에 `QuotaExceededException` + **카운트는 20에서 멈춤(롤백)**
3. `consumeQuestion(memberId, 31)` 단일 호출도 예외
4. 활성 구독자(`SubscriptionService.getActive` mock 으로 Present 반환) → `consumeQuestion(..., 100)` 호출해도 예외 없음 + daily_usage row 미생성
5. `consumeMockSession` 1회 후 2회째에 예외
6. KST 자정 경계 — `LocalDate.now(KST)` 가 일자 바뀌면 새 row 생성 (Clock mock 또는 시간 조작)
7. **동시성 테스트** (선택, 어렵다면 단일 스레드 반복 30번 → 31번째 예외로 대체): 멀티스레드 ExecutorService 로 동시에 30번 호출, 정확히 30 까지만 통과하는지 검증

## 검증

```powershell
cd backend
.\gradlew.bat test
```

## Acceptance Criteria

1. DailyUsageService, QuotaExceededException, Repository UPSERT 메서드 3개 모두 추가.
2. SubscriptionService.**getActive().isPresent()** 로 면제 판정 (hasPremiumAccess 아님).
3. 한도 초과 시 카운트 롤백되어 daily_usage 에 잘못된 누적값이 남지 않는다.
4. 위 7개 시나리오 중 최소 1~5번 + 6번 통과.
5. `gradlew.bat test` 전체 통과 (기존 테스트 회귀 없음).

## 금지 사항

- `hasPremiumAccess()` 로 면제 판정하지 마라. 이유: Focus 도 활성 구독자라 일일 한도 면제 대상인데, 이 함수는 PASS+ 한정이라 Focus 사용자가 무료 한도에 걸린다.
- 외부 캐시(Redis 등) 도입 금지. 이유: 본 작업은 SQL 만으로 충분하며, 인프라 의존성 추가는 별도 결정.
- `@Lock(PESSIMISTIC_WRITE)` 대신 `ON DUPLICATE KEY UPDATE` 사용. 이유: 비관적 락은 hot row 에서 throughput 저하, ON DUPLICATE 은 row lock 만 점유.
- 한도 초과 시 부분 카운트(잔여 5개일 때 5개만 주기) 로직 만들지 마라. 이유: 정책상 한도 초과 호출은 전부 거부 + 클라가 size 조절.

## Status 규칙

- 성공: step 3 `completed` + summary.
- 실패: 3회 후 `error`.
