# Step 2 — DailyUsageEntity + Repository

## 배경

V94 마이그레이션으로 만든 `daily_usage` 테이블을 JPA Entity 로 매핑. 백엔드 가이드(`backend/CLAUDE.md`) 의 패키지 구조 규칙 준수:

```
persistent/usage/
  DailyUsageEntity.java
  DailyUsageId.java          (복합 PK)
  DailyUsageRepository.java
```

복합 PK 매핑은 기존 코드 컨벤션 확인 후 `@IdClass` 또는 `@EmbeddedId` 중 일관된 패턴 사용. **기존 `persistent/` 하위 다른 entity 중 복합 PK 가 있으면 그 패턴 그대로**, 없으면 `@IdClass` 추천 (단순).

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 파일 3개:

| 파일 | 목적 |
|------|------|
| `backend/src/main/java/com/sqldpass/persistent/usage/DailyUsageId.java` | 복합 PK 클래스 (memberId, usageDate) |
| `backend/src/main/java/com/sqldpass/persistent/usage/DailyUsageEntity.java` | JPA Entity — daily_usage 매핑 |
| `backend/src/main/java/com/sqldpass/persistent/usage/DailyUsageRepository.java` | Spring Data JPA Repository |

## DailyUsageId 작성 가이드

```java
package com.sqldpass.persistent.usage;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageId implements Serializable {
    private Long memberId;
    private LocalDate usageDate;

    @Override public boolean equals(Object o) { /* memberId + usageDate */ }
    @Override public int hashCode() { return Objects.hash(memberId, usageDate); }
}
```

## DailyUsageEntity 작성 가이드

- `@Entity`, `@Table(name = "daily_usage")`, `@IdClass(DailyUsageId.class)`
- 필드: `@Id Long memberId`, `@Id LocalDate usageDate`, `int questionCount`, `int mockSessionCount`, `LocalDateTime createdAt`, `LocalDateTime updatedAt`
- 생성자: 신규 row 용 `(Long memberId, LocalDate usageDate)` — count 는 0으로 시작, created/updated 는 `LocalDateTime.now()`
- Lombok `@Getter`, `@NoArgsConstructor(access = PROTECTED)` 허용 (CLAUDE.md 가이드)
- 빌더 패턴 금지 (백엔드 가이드)

증가 메서드:
```java
public void addQuestionCount(int delta) {
    this.questionCount += delta;
    this.updatedAt = LocalDateTime.now();
}
public void incrementMockSession() {
    this.mockSessionCount += 1;
    this.updatedAt = LocalDateTime.now();
}
```

## DailyUsageRepository 작성 가이드

```java
public interface DailyUsageRepository extends JpaRepository<DailyUsageEntity, DailyUsageId> {

    // 단순 조회 — 표시용
    Optional<DailyUsageEntity> findByMemberIdAndUsageDate(Long memberId, LocalDate usageDate);

    // Step 3 에서 atomic UPSERT 용으로 native query 추가. 이 step 에서는 시그니처만 잡고 구현은 다음 step.
}
```

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
```

가능하면 `.\gradlew.bat test` 도 통과 확인 (기존 테스트가 영향받지 않아야 함).

## Acceptance Criteria

1. 3개 파일 (Id, Entity, Repository) 추가.
2. CLAUDE.md 의 데이터 흐름·패키지 규칙 준수 (persistent/ 위치, 생성자 주입, Builder 금지).
3. `gradlew.bat compileJava` 통과.
4. 기존 테스트 깨지지 않음.

## 금지 사항

- Builder 패턴 사용 금지. 이유: backend/CLAUDE.md 정책.
- Entity 에 Domain import 금지. 이유: 백엔드 레이어 규칙.
- 이 step 에서 native UPSERT 쿼리를 구현하지 마라. 이유: step 3 의 책임이며, 트랜잭션 동작과 함께 검증해야 함.

## Status 규칙

- 성공: step 2 `completed` + summary.
- 실패: 3회 후 `error`.
