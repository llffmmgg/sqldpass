# Step 1 — Flyway V80, V81 마이그레이션 추가

## 배경

P0/P2 이슈를 코드보다 먼저 스키마 레벨에서 막거나 받쳐 두기 위해 두 개 마이그레이션을 추가한다.

- V80: `subscription.payment_id` 에 UNIQUE 제약 — `PaymentService.verify` 가 같은 paymentId 로 두 번 호출돼도 중복 SubscriptionEntity 가 만들어지지 않게 DB 가 거부하도록 한다 (P0-1 의 안전망).
- V81: `subscription_history` 감사 테이블 — `expireManual` 의 delete 패턴을 revoke + history 로 바꾸고, 환불·만료·관리자 발급 이력을 보존한다 (P2-8).

운영은 Flyway + Hibernate `ddl-auto: validate` 라 마이그레이션 누락 시 부팅 실패. 새 V## 번호는 `V79` 다음의 `V80`, `V81` 로 발급한다.

## 작업 디렉터리

```
backend/
```

## 변경 대상

신규 파일 2개:

| 파일 | 목적 |
|------|------|
| `backend/src/main/resources/db/migration/V80__add_subscription_payment_unique.sql` | subscription.payment_id UNIQUE |
| `backend/src/main/resources/db/migration/V81__create_subscription_history.sql` | 감사 테이블 |

## V80 작성 가이드

```sql
-- subscription.payment_id 에 UNIQUE 제약 — verify 재호출 시 중복 발급 방어.
-- V78 에서 nullable 로 변경됐으나 MySQL 은 UNIQUE 컬럼의 NULL 다중 허용이라
-- admin 수동 발급(payment_id=null) 행이 여러 개 있어도 영향 없음.

ALTER TABLE subscription
    ADD CONSTRAINT uk_subscription_payment_id UNIQUE (payment_id);
```

## V81 작성 가이드

요구 컬럼:

- `id BIGINT AUTO_INCREMENT PK`
- `member_id BIGINT NOT NULL`
- `plan VARCHAR(20) NOT NULL` — SubscriptionPlan enum 값
- `action VARCHAR(20) NOT NULL` — GRANTED / REVOKED / EXPIRED / REFUNDED
- `reason VARCHAR(500) NULL`
- `actor_admin_id BIGINT NULL` — 어드민 액션이면 admin memberId, 시스템이면 NULL
- `payment_id BIGINT NULL` — 연결된 결제 row (있다면)
- `occurred_at DATETIME(6) NOT NULL`
- `created_at DATETIME(6) NOT NULL`
- `updated_at DATETIME(6) NOT NULL`
- INDEX `idx_history_member_occurred (member_id, occurred_at)` — DESC 는 MySQL 8 에서 무시되거나 실제 사용 시 ORDER BY DESC 로 처리되므로 컬럼 인덱스만 작성
- FK 미설정 — 회원 hard delete 와 분리, audit 테이블 특성

ENGINE/CHARSET/COLLATE 는 기존 마이그레이션과 동일하게 (`InnoDB` / `utf8mb4` / `utf8mb4_unicode_ci`).

기존 V74/V75 의 SQL 스타일과 들여쓰기 일관 유지.

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
```

Java 컴파일이 통과해야 함 — 마이그레이션은 부팅 시 적용되지만, 컴파일 단계는 SQL 영향 없음.

가능하면 로컬 MySQL 에 `flyway:migrate` (있으면) 또는 `bootRun` 으로 V80, V81 적용을 한 번 검증. 부팅 실패 없이 두 테이블 변경/생성 확인.

## Acceptance Criteria

1. `V80__add_subscription_payment_unique.sql` 가 위 SQL 형태로 추가된다.
2. `V81__create_subscription_history.sql` 가 컬럼/인덱스/FK 부재 형태로 추가된다.
3. `backend/src/main/resources/db/migration/` 안의 V## 번호 충돌 없음 (V79 다음 V80, V81).
4. `gradlew.bat compileJava` 통과.
5. 두 SQL 파일 모두 한국어 주석으로 의도/이유를 명시 (3줄 내외).

## 금지 사항

- 기존 V74~V79 마이그레이션 파일을 수정하지 마라. 이유: Flyway checksum 이 바뀌면 운영 부팅이 거부된다.
- V80/V81 안에 `SELECT` 또는 데이터 백필을 넣지 마라. 이유: 본 step 은 스키마 변경만이며, 백필/리프레시는 별도 step 또는 운영 SQL.
- subscription_history 에 FK 를 걸지 마라. 이유: 회원 hard delete 시 audit 가 깨진다.

## Status 규칙

- 성공: `phases/payment-hardening/index.json` 의 step 1 status 를 `completed`, `summary` 에 "V80 unique + V81 history 마이그레이션 추가, compileJava OK" 기록.
- 실패: 3회 재시도 후 컴파일 실패면 `error` + `error_message`.
- blocked: V## 번호 정책 또는 운영 마이그레이션 정책에 사용자 결정 필요 시 `blocked`.
