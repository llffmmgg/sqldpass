# Step 1 — 백엔드: 구독 soft-delete (archived_at)

## 배경

현재 `SubscriptionEntity`는 `expiresAt`으로만 활성/만료를 판별한다. admin 본인이 결제 동선 검증·환불 점검 등 **테스트 결제**를 만들면 그게 매출 통계에 그대로 잡혀 실제 수익을 왜곡한다. 만료된 구독 중 admin이 명시적으로 "삭제"한 row를 통계 집계에서 빼기 위해 `archived_at` soft-delete 컬럼을 도입한다.

활성 구독은 삭제 불가(먼저 만료 처리). 멱등 보장. `subscription_history` 에 `DELETED` action 기록.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/main/resources/db/migration/V86__subscription_archived_at.sql` (신규) | `ALTER TABLE subscription ADD COLUMN archived_at TIMESTAMP NULL` + `idx_subscription_archived_at` |
| `persistent/subscription/SubscriptionEntity.java` | `archivedAt` 필드 + `isArchived()` + `archive(now)` 헬퍼 |
| `persistent/subscription/SubscriptionRepository.java` | 기존 list 쿼리(또는 service 가 사용하는 finder)에 `archived_at IS NULL` 조건 추가. 신규 finder가 필요하면 `findActiveOrExpired(...)` 같은 명시 메서드로. |
| `persistent/subscription/history/SubscriptionHistory.java` (또는 동등 파일) | action 상수에 `DELETED` 추가. `static SubscriptionHistory archived(sub, reason, actorAdminId)` 헬퍼. |
| `service/admin/AdminSubscriptionService.java` | `archive(subscriptionId, reason, actorAdminId)` 메서드 — 활성 거부, 멱등, history 기록. |
| `controller/admin/AdminSubscriptionController.java` | `DELETE /api/admin/subscriptions/{id}/archive?reason=...` endpoint. 기존 `DELETE /api/admin/subscriptions/{id}` (수동 만료)는 그대로 유지. |
| 신규/보강 테스트 | `AdminSubscriptionServiceTest`: archive 활성거부, archive 멱등, archive 후 history.DELETED 1줄. `AdminSubscriptionControllerTest`: 200 응답, 404 미존재. 목록 쿼리에 archived_at IS NULL 필터가 걸려 archived row 가 빠지는 회귀 테스트. |

> 최신 V## 는 V85(`add_payment_buyer_info` 다음 V85 `create_app_setting`). 이번이 **V86** 임을 확인했다.

## V86 마이그레이션

```sql
-- V86__subscription_archived_at.sql
ALTER TABLE subscription
    ADD COLUMN archived_at TIMESTAMP NULL COMMENT '운영자가 통계에서 분리하려고 정리한 시점. NULL=정상.';

CREATE INDEX idx_subscription_archived_at ON subscription(archived_at);
```

## SubscriptionEntity 변경

```java
@Column(name = "archived_at")
private LocalDateTime archivedAt;

public boolean isArchived() {
    return archivedAt != null;
}

public void archive(LocalDateTime now) {
    this.archivedAt = now;
}
```

기존 `isActive(now)` 는 그대로(`archived_at`은 통계 차원의 분리지 권한엔 영향 없음 — archived 라도 만료 전이라면 권한 유지하는 것이 안전). 단 일반 활성 구독에 archive 호출은 거부하므로 실제로는 만료된 구독만 archived 상태가 됨.

## AdminSubscriptionService.archive 시그니처

```java
@Transactional
public void archive(Long subscriptionId, String reason, Long actorAdminId) {
    SubscriptionEntity sub = repo.findById(subscriptionId)
        .orElseThrow(() -> new IllegalArgumentException("구독을 찾을 수 없습니다: " + subscriptionId));
    LocalDateTime now = LocalDateTime.now();
    if (sub.isActive(now)) {
        throw new IllegalStateException("활성 구독은 삭제할 수 없습니다. 먼저 만료 처리하세요.");
    }
    if (sub.isArchived()) return; // 멱등
    sub.archive(now);
    repo.save(sub);
    historyRepo.save(SubscriptionHistory.archived(sub, reason, actorAdminId));
    log.info("어드민 구독 삭제(archive): id={}, memberId={}, reason={}", sub.getId(), sub.getMemberId(), reason);
}
```

## Controller endpoint

기존 `DELETE /api/admin/subscriptions/{id}` (expireManual) 와 충돌하지 않도록 path 분리:

```java
@DeleteMapping("/{id}/archive")
public ResponseEntity<Void> archive(
    @PathVariable Long id,
    @RequestParam(required = false, defaultValue = "") String reason,
    HttpServletRequest request
) {
    Long actorAdminId = (Long) request.getAttribute("memberId"); // admin 토큰은 null 가능, V81 nullable 기준
    service.archive(id, reason.isBlank() ? "(사유 미입력)" : reason, actorAdminId);
    return ResponseEntity.noContent().build();
}
```

`AdminAuthInterceptor` 가 이미 `/api/admin/**` 보호하므로 별도 등록 불필요. `WebMvcConfig` 변경 없음.

## 목록 쿼리 archived_at IS NULL

`AdminSubscriptionService.list(...)` 또는 그 안에서 호출하는 Repository 메서드의 JPQL/Native 쿼리에 `AND s.archivedAt IS NULL` 조건 추가. 기존 호출자 영향 없도록 새 옵션 파라미터 `includeArchived` (기본 false)로 둘 수도 있지만, 이번 step 에선 **기본 동작이 archived 제외**만 보장하면 충분. `includeArchived=true` 향후 필요 시 확장.

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `V86__subscription_archived_at.sql` 작성, gradle 부팅 시 Flyway 적용(테스트에서 schema validate 통과).
2. `SubscriptionEntity` 에 `archivedAt` 필드 + getter + `isArchived()` + `archive(now)`.
3. `AdminSubscriptionService.archive(...)` — 활성 거부, 멱등, history `DELETED` 1줄 기록.
4. `DELETE /api/admin/subscriptions/{id}/archive?reason=...` 200/204 응답. 기존 `DELETE /api/admin/subscriptions/{id}` (만료) 그대로 동작.
5. `list(...)` 가 archived row 제외 반환.
6. 신규 테스트 ≥ 4건 통과 (활성거부, 멱등, history 기록, 목록 제외 회귀).
7. `gradlew test` 전체 통과, `gradlew compileJava` 통과.

## 금지 사항

- `DELETE /api/admin/subscriptions/{id}` 의 시그니처/기존 만료 동작을 바꾸지 마라. **이유**: 회귀 위험 + UX 분리 (만료와 삭제는 다른 행동).
- 활성 구독의 archive 호출을 통과시키지 마라. **이유**: 활성 권한이 살아있는 row 가 통계에서 사라지면 운영 혼란.
- `archived_at` 을 NOT NULL 로 만들지 마라. **이유**: 기본은 NULL(정상)이고, 운영자가 선택적으로 설정.
- 기존 V## 파일을 수정해서 컬럼을 추가하지 마라. **이유**: Flyway 체크섬 깨짐 → 운영 부팅 실패. 반드시 V86 신규.
- Hard delete 하지 마라. **이유**: payment FK 제약 + 감사 로그(history) 보존 필요.

## Status 규칙

- 성공: step 1 `completed`, summary "V86 archived_at + Entity.archive + AdminSubscriptionService.archive(활성거부/멱등) + DELETE /archive + 목록 archived 제외 + 테스트 4건+, test/compile OK".
- 실패: 3회 재시도 후 `error`, `error_message` 에 마지막 stacktrace 핵심 줄.
