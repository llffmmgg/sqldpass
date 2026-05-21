# Step 1 — Flyway V94 daily_usage 마이그레이션

## 배경

무료 회원의 일일 사용량을 추적할 단일 테이블을 만든다. 운영은 Flyway + Hibernate `ddl-auto: validate` 라 마이그레이션 누락 시 부팅 실패.

- 복합 PK `(member_id, usage_date)` → 매일 새 row 자연 생성 → 별도 리셋 cron 불필요
- `usage_date`는 KST `LocalDate` 기준 (메모리 `project_kst_naive_serialization` 참조)
- `question_count`(일일 30 한도) + `mock_session_count`(미니+모의 합산, 일일 1 한도) 두 카운터를 한 row에 둠

기존 `anonymous_solve_count`(V41) / `anonymous_solve_ip_quota`(V62)와 별개 — 그 두 개는 비로그인 IP 기반이라 이번 작업 범위 밖. **V41/V62는 절대 수정하지 마라.**

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 파일 1개:

| 파일 | 목적 |
|------|------|
| `backend/src/main/resources/db/migration/V94__create_daily_usage.sql` | 회원별 일일 사용량 카운터 |

**V## 번호 확인**: `Get-ChildItem backend/src/main/resources/db/migration` 으로 최신 V##를 확인. 현재 V93이 최신이면 V94 발급. **만약 누군가 이미 V94를 점유했다면 V95로 올려라 (충돌 금지).**

## V94 작성 가이드

```sql
-- 회원별 일일 사용량 카운터 (무료 한도 트래킹).
-- 복합 PK (member_id, usage_date) — 매일 새 row 자연 생성으로 자정 리셋 효과.
-- 활성 구독자는 서비스 레이어에서 카운터 생성 자체를 스킵하므로 이 테이블엔 들어오지 않음.

CREATE TABLE daily_usage (
    member_id           BIGINT      NOT NULL,
    usage_date          DATE        NOT NULL,
    question_count      INT         NOT NULL DEFAULT 0,
    mock_session_count  INT         NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id, usage_date),
    INDEX idx_daily_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- ENGINE/CHARSET/COLLATE 기존 마이그레이션과 동일하게 유지.
- FK 미설정 — 회원 hard delete 와 분리하고, audit 가 아니라 일일 캐시성 데이터라 유실되어도 무해.
- `idx_daily_usage_date` 는 향후 운영 통계용 (e.g. "어제 30문제 초과 도달한 사용자 수").

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
```

컴파일 통과해야 함 — 마이그레이션은 부팅 시 적용되지만, 컴파일 단계는 SQL 영향 없음.

가능하면 로컬 MySQL 에 `bootRun` 으로 V94 적용을 한 번 확인.

## Acceptance Criteria

1. `V94__create_daily_usage.sql`(또는 충돌 시 V95) 가 위 SQL 형태로 추가된다.
2. 기존 V## 파일을 수정하지 않았다.
3. `gradlew.bat compileJava` 통과.
4. SQL 파일 첫 3줄에 한국어 주석으로 의도 명시.

## 금지 사항

- 기존 V41/V62 익명 카운터 테이블을 건드리지 마라. 이유: 비로그인 IP 쿼터는 별도 시스템.
- daily_usage 에 FK 를 걸지 마라. 이유: 회원 hard delete 시 부팅 실패.
- 일일 카운터에 reset cron 을 만들지 마라. 이유: 복합 PK 가 자동 일별 분리.

## Status 규칙

- 성공: `phases/free-daily-quota-backend/index.json` step 1 status `completed` + summary 한 줄.
- 실패: 3회 재시도 후 컴파일 실패면 `error` + `error_message`.
- blocked: V## 번호 충돌이 다른 PR 과 겹쳐 결정 필요 시 `blocked`.
