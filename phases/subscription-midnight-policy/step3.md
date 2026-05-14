# Step 3 — V87 마이그레이션 (기존 데이터 보정)

## 배경

이미 발급된 active 구독은 기존 정책으로 시각 단위 만료가 박혀 있다. 새 정책에 맞춰 일괄 재계산. 운영 DB 의 LocalDateTime 은 UTC 기준(JDBC `preserveInstants` 가 LocalDateTime 에 영향 없음 — 사용자 케이스 "야비한 부추 5/14~5/17" 표시로 확인). `+9h` 로 KR 일자 추정.

다른 환경(로컬/스테이지)의 시각 가정이 다를 수 있는 위험을 줄이려고 **WHERE 안전 조건** — 기존 정책 흔적이 있는 row(`expires_at - paid_at` 이 정확히 plan.days × 24h)만 보정. 이미 다른 방식으로 박힌 row 는 보존.

## 의존성

- Step 1, 2 완료 후 (코드 + TZ 환경이 새 정책으로 일관된 상태에서 데이터 보정).

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/resources/db/migration/V87__subscription_expires_at_kst_midnight.sql` (신규) | active 구독 expires_at 재계산. 결제 연결 + 어드민 수동 발급 두 분기. UNLIMITED 제외. |

## V87 SQL

```sql
-- 새 정책: paidAt 의 KR 일자 + (plan.days + 1)일 의 00:00 KST.
-- 기존 정책(시각 단위 +plan.days)으로 정확히 박힌 row 만 재계산.
-- 운영 LocalDateTime 은 UTC 기준 → +9h 로 KR 일자 추정.
-- UNLIMITED 는 expires_at NULL 이라 영향 없음.

-- ============================================================
-- 1) 결제 연결 구독 (paymentId NOT NULL) — payment.paid_at 기준
-- ============================================================
UPDATE subscription s
JOIN payment p ON p.id = s.payment_id
SET s.expires_at = DATE(DATE_ADD(p.paid_at, INTERVAL 9 HOUR)) + INTERVAL (
    CASE s.plan
        WHEN 'THREE_DAY' THEN 4
        WHEN 'FOCUS'     THEN 31
        WHEN 'ONE_MONTH' THEN 31
    END
) DAY
WHERE s.archived_at IS NULL
  AND s.plan IN ('THREE_DAY', 'FOCUS', 'ONE_MONTH')
  AND s.expires_at IS NOT NULL
  -- 기존 정책 흔적 — 이미 다른 방식 보정된 row 보존
  AND TIMESTAMPDIFF(HOUR, p.paid_at, s.expires_at) = CASE s.plan
        WHEN 'THREE_DAY' THEN 72
        WHEN 'FOCUS'     THEN 720
        WHEN 'ONE_MONTH' THEN 720
      END;

-- ============================================================
-- 2) 어드민 수동 발급 (paymentId IS NULL) — subscription.purchased_at 기준
-- ============================================================
UPDATE subscription s
SET s.expires_at = DATE(DATE_ADD(s.purchased_at, INTERVAL 9 HOUR)) + INTERVAL (
    CASE s.plan
        WHEN 'THREE_DAY' THEN 4
        WHEN 'FOCUS'     THEN 31
        WHEN 'ONE_MONTH' THEN 31
    END
) DAY
WHERE s.archived_at IS NULL
  AND s.payment_id IS NULL
  AND s.plan IN ('THREE_DAY', 'FOCUS', 'ONE_MONTH')
  AND s.expires_at IS NOT NULL
  AND TIMESTAMPDIFF(HOUR, s.purchased_at, s.expires_at) = CASE s.plan
        WHEN 'THREE_DAY' THEN 72
        WHEN 'FOCUS'     THEN 720
        WHEN 'ONE_MONTH' THEN 720
      END;
```

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test
```

Spring Boot 부팅 시 Flyway 가 자동 적용. 테스트 환경에선 시드 데이터 없으면 영향 없음(빈 UPDATE).

수동 (운영 배포 후):
- 사전 점검 SELECT 로 영향 row 확인 (plan 파일의 SELECT)
- 마이그레이션 적용 결과 `subscription.expires_at` 이 정수 자정 시각
- 권한 게이트(`/api/mock-exams/...`) 정상 동작

## Acceptance Criteria

1. V87 파일 작성, naming convention 정확(`V87__`).
2. SQL 두 UPDATE 가 안전 조건(WHERE TIMESTAMPDIFF 일치) 포함.
3. UNLIMITED 와 archived 구독 영향 없음.
4. 로컬/CI 환경에서 부팅 시 Flyway 적용 통과 (`gradlew test` 가 spring boot 시작 포함하면).
5. `gradlew test`, `gradlew compileJava` 통과.

## 금지 사항

- WHERE `TIMESTAMPDIFF(...)` 안전 조건을 빼지 마라. **이유**: 어드민이 수동으로 만료를 늦춘 row 등 다른 방식 보정 row 에 잘못 적용되면 권한 회복 불가.
- `payment.paid_at` / `subscription.purchased_at` 컬럼 자체에 +9h SQL 을 적용하지 마라. **이유**: 본 phase 는 만료 시각만 정정. 결제 시각은 audit 가치 — 별도 결정 필요.
- 같은 V87 안에 ALTER TABLE 추가하지 마라. **이유**: 마이그레이션 한 파일은 한 의도. 데이터 보정 vs DDL 분리.
- 기존 V## 파일(V86 이하) 을 수정하지 마라. **이유**: Flyway 체크섬 깨짐 → 운영 부팅 실패.

## Status 규칙

- 성공: step 3 `completed`, summary "V87__subscription_expires_at_kst_midnight.sql 신규 — paid_at/purchased_at +9h 기반 KR 일자 + (plan.days+1)일 자정 재계산, 안전 조건 적용, gradle test/compile OK".
- 실패: 3회 재시도 후 `error`.
