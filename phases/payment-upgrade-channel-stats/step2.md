# Step 2 — 백엔드: provider 별 매출 통계 API

## 배경

현재 `/api/admin/stats/revenue` 와 `/api/admin/stats/revenue/by-plan` 는 `payment.amount` 를 단순 SUM 하고 `provider` 컬럼을 무시. iOS / Play Billing / PortOne 채널별 매출이 통합 합산되어 어드민 대시보드에서 채널별 분리 불가.

본 step 에서 provider 별 분리 통계 쿼리/엔드포인트 2개 신설. 기존 통합 엔드포인트는 호환성 위해 그대로 유지.

`subscription.archived_at IS NULL` 필터는 기존 쿼리와 동일하게 유지 — admin 본인 테스트 결제 분리.

**병렬 안전**: 본 step 은 Step 1 과 같은 PR 에 들어가지만, 다른 파일군을 손대므로 worktree 격리해서 병렬 작업 가능. PaymentService.java / SubscriptionHistoryAction.java 는 안 건드림.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `controller/admin/AdminRevenueByProviderPoint.java` (신규 record) | `LocalDate date, String provider, long revenue, long refundAmount, int count` |
| `controller/admin/AdminRevenueByProviderPlan.java` (신규 record) | `String provider, String plan, int count, long revenue` |
| `persistent/payment/PaymentRepository.java` | `findDailyRevenueByProviderRaw(since)` + `findRevenueByProviderAndPlanRaw(since)` native 쿼리 추가 |
| `service/admin/AdminStatsService.java` | `revenueByProvider(days)` + `revenueByProviderAndPlan(days)` 메서드. days 가드 7~365. Object[] → record 매핑. |
| `controller/admin/AdminStatsController.java` | `GET /api/admin/stats/revenue/by-provider?days=N` + `GET /api/admin/stats/revenue/by-provider/by-plan?days=N` 엔드포인트 2개. |
| 신규 테스트 | `AdminStatsServiceTest` 또는 `PaymentRepository` 통합 테스트: provider 분리 sum 검증, archived 제외, days 가드. |

## 일자 × provider 분리 쿼리 (native)

```java
@Query(value = """
        SELECT
            DATE(p.paid_at) AS date,
            p.provider AS provider,
            COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0) AS revenue,
            COALESCE(SUM(CASE WHEN p.status = 'CANCELLED' THEN p.amount ELSE 0 END), 0) AS refund_amount,
            SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END) AS count
        FROM payment p
        LEFT JOIN subscription s ON s.payment_id = p.id
        WHERE p.paid_at >= :since
          AND (s.id IS NULL OR s.archived_at IS NULL)
        GROUP BY DATE(p.paid_at), p.provider
        ORDER BY DATE(p.paid_at) ASC, p.provider ASC
        """, nativeQuery = true)
List<Object[]> findDailyRevenueByProviderRaw(@Param("since") LocalDateTime since);
```

## plan × provider 분리 쿼리

```java
@Query(value = """
        SELECT
            p.provider AS provider,
            p.plan AS plan,
            COUNT(*) AS count,
            COALESCE(SUM(p.amount), 0) AS revenue
        FROM payment p
        LEFT JOIN subscription s ON s.payment_id = p.id
        WHERE p.status = 'PAID'
          AND p.paid_at >= :since
          AND p.plan IS NOT NULL
          AND (s.id IS NULL OR s.archived_at IS NULL)
        GROUP BY p.provider, p.plan
        ORDER BY revenue DESC
        """, nativeQuery = true)
List<Object[]> findRevenueByProviderAndPlanRaw(@Param("since") LocalDateTime since);
```

## Service 매핑

```java
public List<AdminRevenueByProviderPoint> revenueByProvider(int days) {
    int clamped = clamp(days, 7, 365);
    LocalDateTime since = LocalDateTime.now().minusDays(clamped);
    return paymentRepository.findDailyRevenueByProviderRaw(since).stream()
            .map(row -> new AdminRevenueByProviderPoint(
                    ((java.sql.Date) row[0]).toLocalDate(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).intValue()))
            .toList();
}
```

`AdminStatsService` 의 기존 `clamp` 와 `days` 가드를 그대로 재사용.

## Controller

```java
@GetMapping("/revenue/by-provider")
public List<AdminRevenueByProviderPoint> revenueByProvider(@RequestParam(defaultValue = "30") int days) {
    return service.revenueByProvider(days);
}

@GetMapping("/revenue/by-provider/by-plan")
public List<AdminRevenueByProviderPlan> revenueByProviderAndPlan(@RequestParam(defaultValue = "30") int days) {
    return service.revenueByProviderAndPlan(days);
}
```

## 응답 예시

`GET /api/admin/stats/revenue/by-provider?days=7`:

```json
[
  {"date": "2026-05-14", "provider": "PORTONE", "revenue": 19800, "refundAmount": 0, "count": 2},
  {"date": "2026-05-14", "provider": "APP_STORE", "revenue": 9900, "refundAmount": 0, "count": 1},
  {"date": "2026-05-15", "provider": "PORTONE", "revenue": 29900, "refundAmount": 0, "count": 1}
]
```

같은 날짜의 다른 provider 가 별 row. 프론트가 채널별 group 또는 합산 선택 가능.

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.admin.*"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `GET /api/admin/stats/revenue/by-provider?days=30` 200 + 일자×provider 배열.
2. `GET /api/admin/stats/revenue/by-provider/by-plan?days=30` 200 + provider×plan 배열.
3. `archived_at IS NULL` 필터 동작 (기존 통합 쿼리와 동치).
4. days 가드 7~365 clamp.
5. 기존 `/revenue` `/revenue/by-plan` 엔드포인트 응답 형식 회귀 0.
6. 신규 테스트 ≥ 3건 통과.

## 금지 사항

- `archived_at IS NULL` 조건을 빼지 마라. **이유**: admin 테스트 결제가 통계에 잡힘.
- INNER JOIN 으로 바꾸지 마라. **이유**: 일부 결제는 구독 row 매핑이 없을 수 있음 (환불·실패).
- 기존 `findDailyRevenue` / `findRevenueByPlan` 쿼리를 수정·삭제하지 마라. **이유**: 프론트 기존 차트가 그대로 호출 중. 호환성 깨면 차트 깨짐.
- provider 컬럼이 NULL 인 row 를 그룹에서 제외하지 마라. **이유**: 옛 PortOne 결제는 provider=NULL 일 수 있음 (V79 이전). 그것도 통계에 잡혀야 함. 대신 service 매핑 단계에서 `provider == null ? "PORTONE" : provider` 로 보정.

## Status 규칙

- 성공: `completed` + summary "AdminRevenueByProviderPoint/Plan record + Repository 2 native 쿼리(archived 제외) + Service 매핑 + 2 endpoint + 테스트 N건, test/compile OK".
- 실패: 3회 재시도 후 `error`.
