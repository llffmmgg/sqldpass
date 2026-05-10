# Step 2 — 감사(Audit) 도메인 신설

## 배경

Step 1 에서 만든 `subscription_history` 테이블에 대응하는 JPA 도메인을 추가한다.
`expireManual` 이 row 를 delete 해 환불 추적이 끊긴다는 P2-8 이슈를 해결하기 위한 기반.

기존 백엔드의 `service/generation/GenerationLockService.java:21-46` 가 `@Transactional(propagation = REQUIRES_NEW)` 패턴으로 별도 트랜잭션 커밋을 보장한다 — 동일 패턴을 audit 기록에 재사용한다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 파일 4개:

| 파일 | 역할 |
|------|------|
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionHistoryAction.java` | enum (GRANTED/REVOKED/EXPIRED/REFUNDED) |
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionHistoryEntity.java` | JPA Entity, BaseTimeEntity 상속 |
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionHistoryRepository.java` | JpaRepository, 회원별 이력 조회 |
| `backend/src/main/java/com/sqldpass/service/payment/SubscriptionHistoryService.java` | REQUIRES_NEW 로 record(...) 호출 |

신규 테스트 1개:

| 파일 | 역할 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/SubscriptionHistoryServiceTest.java` | record() 가 entity 를 생성/저장하는지 mock 으로 검증 |

## SubscriptionHistoryAction (enum)

```java
public enum SubscriptionHistoryAction {
    GRANTED,   // 어드민 수동 발급 또는 결제 후 신규 발급 (옵션)
    REVOKED,   // 운영자 수동 회수 (환불 외)
    EXPIRED,   // 어드민 수동 만료
    REFUNDED   // Play Billing RTDN 환불
}
```

## SubscriptionHistoryEntity 구조

- `BaseTimeEntity` 상속
- `@Table(name = "subscription_history", indexes = { @Index(name="idx_history_member_occurred", columnList="member_id,occurred_at") })`
- 필드: `id`, `memberId`, `plan` (`@Enumerated(EnumType.STRING)`), `action` (동), `reason`, `actorAdminId`, `paymentId`, `occurredAt`
- 생성자 1개: `(Long memberId, SubscriptionPlan plan, SubscriptionHistoryAction action, String reason, Long actorAdminId, Long paymentId, LocalDateTime occurredAt)`
- 백엔드 CLAUDE.md 규칙대로 `@Getter` + `@NoArgsConstructor(access = PROTECTED)` 사용, Builder 금지.

## Repository

```java
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistoryEntity, Long> {
    List<SubscriptionHistoryEntity> findByMemberIdOrderByOccurredAtDesc(Long memberId);
}
```

## Service (REQUIRES_NEW 핵심)

```java
@Service
@RequiredArgsConstructor
public class SubscriptionHistoryService {
    private final SubscriptionHistoryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long memberId, SubscriptionPlan plan, SubscriptionHistoryAction action,
                       String reason, Long actorAdminId, Long paymentId) {
        SubscriptionHistoryEntity entity = new SubscriptionHistoryEntity(
                memberId, plan, action, reason, actorAdminId, paymentId, LocalDateTime.now());
        repository.save(entity);
    }
}
```

REQUIRES_NEW 는 호출자 트랜잭션이 롤백돼도 history 는 보존되도록 분리 (예: 환불 처리 중 다른 단계가 실패해도 환불 시도 자체는 audit 에 남는다).

## 테스트 케이스 (SubscriptionHistoryServiceTest)

- `record_호출시_entity_가_저장된다` — Mockito 로 repository.save 1회 호출 + ArgumentCaptor 로 필드 검증
- `record_의_occurredAt_은_now_근사값` — 1분 이내 차이 검증 (LocalDateTime.now)

`@ExtendWith(MockitoExtension.class)` 사용, repository mock.

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test --tests "com.sqldpass.service.payment.SubscriptionHistoryServiceTest"
```

전체 test 회귀가 없도록 추가:

```powershell
.\gradlew.bat test
```

## Acceptance Criteria

1. 위 4개 파일이 추가되고, 패키지 위치가 `persistent/payment` (3개) + `service/payment` (1개) 에 위치한다.
2. SubscriptionHistoryEntity 의 컬럼·인덱스가 V81 SQL 과 정확히 일치한다 (컬럼명, 길이, nullable).
3. SubscriptionHistoryService.record 가 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 로 어노테이션돼 있다.
4. SubscriptionHistoryServiceTest 가 통과한다.
5. `gradlew.bat compileJava` + `gradlew.bat test` 모두 통과 (기존 테스트 회귀 없음).

## 금지 사항

- Builder 패턴을 쓰지 마라. 이유: 백엔드 CLAUDE.md 가 생성자 주입을 강제한다.
- Entity → Domain 매퍼를 만들지 마라. 이유: audit 는 직접 조회만 쓰고 도메인 변환 없이 Entity 단위로 다룰 수 있다.
- record(...) 메서드의 throw 를 호출자에 전파하지 마라. 이유: audit 실패가 본 비즈니스 로직을 깨면 안 된다 — try/catch 로 swallow 하고 log.warn 만.

## Status 규칙

- 성공: step 2 status 를 `completed`, summary 에 "audit Entity/Action/Repo/Service + REQUIRES_NEW 테스트 추가, gradle test OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: enum action 종류에 사용자 추가 결정 필요 시 `blocked`.
