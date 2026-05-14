# Step 2 — 백엔드: 구독 매출 통계 API

## 배경

어드민 구독 페이지에 매출 추이·환불 추이·플랜별 분포 그래프를 그리기 위한 집계 endpoint 2개. 모두 archived 된 구독(Step 1)을 **제외**한 결과만 반환해 admin 본인 테스트 결제가 통계에 안 잡히도록 한다.

기존 `/api/admin/stats`, `/api/admin/stats/trend` 패턴과 일관.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `controller/admin/AdminRevenuePoint.java` (신규 record) | `LocalDate date, long revenue, long refundAmount, int count` |
| `controller/admin/AdminRevenueByPlan.java` (신규 record) | `String plan, String label, int count, long revenue` (label 은 백엔드에서 한국어/표시명 채움 또는 plan key 만 주고 프론트에서 label 매핑 — 본 step 에서는 plan key 만 주는 게 단순. label 필드 제거 가능) |
| `persistent/payment/PaymentRepository.java` | `findDailyRevenue(LocalDateTime since)` 커스텀 쿼리, `findRevenueByPlan(LocalDateTime since)` 커스텀 쿼리 — 둘 다 `subscription.archived_at IS NULL` 조건 포함. |
| `service/admin/AdminStatsService.java` (기존이면 보강, 없으면 신규) | `revenueTrend(days)`, `revenueByPlan(days)` 메서드. days 가드 7~365. |
| `controller/admin/AdminStatsController.java` | `GET /api/admin/stats/revenue?days=N`, `GET /api/admin/stats/revenue/by-plan?days=N` endpoint 2개. |
| 신규 테스트 | `AdminStatsServiceTest`: 일별 합산, 환불액 분리(status=CANCELLED 또는 환불 처리된 payment), archived 구독 제외, by-plan 정렬(revenue DESC). `AdminStatsControllerTest`: 200 응답, days 가드. |

> 기존 `/api/admin/stats/trend` 가 `AdminStatsController` 에 있는지 확인하고 동일 컨트롤러에 메서드 추가. 없으면 신규 컨트롤러.

## 일별 매출 JPQL

```java
@Query("""
        SELECT new com.sqldpass.controller.admin.AdminRevenuePoint(
            CAST(p.paidAt AS LocalDate),
            COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN p.status = 'CANCELLED' THEN p.amount ELSE 0 END), 0),
            COUNT(CASE WHEN p.status = 'PAID' THEN 1 END)
        )
        FROM PaymentEntity p
        LEFT JOIN SubscriptionEntity s ON s.paymentId = p.id
        WHERE p.paidAt >= :since
          AND (s.id IS NULL OR s.archivedAt IS NULL)
        GROUP BY CAST(p.paidAt AS LocalDate)
        ORDER BY CAST(p.paidAt AS LocalDate) ASC
        """)
List<AdminRevenuePoint> findDailyRevenue(@Param("since") LocalDateTime since);
```

**중요**: `LEFT JOIN s.paymentId = p.id` — 결제는 있는데 구독 row 없는 경우(드물지만 환불·실패 등) 도 일단 PAID 매출은 카운트하되, archived 구독에 연결된 결제만 제외. JPQL의 `CAST(p.paidAt AS LocalDate)` 가 DB 방언에서 작동하는지 확인(MySQL에서는 `DATE(p.paid_at)`. JPQL `function('DATE', p.paidAt)` 형태가 안전).

대안: native query 로 `DATE(p.paid_at)` 직접 사용:

```java
@Query(value = """
        SELECT
            DATE(p.paid_at) AS date,
            COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0) AS revenue,
            COALESCE(SUM(CASE WHEN p.status = 'CANCELLED' THEN p.amount ELSE 0 END), 0) AS refund_amount,
            SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END) AS count
        FROM payment p
        LEFT JOIN subscription s ON s.payment_id = p.id
        WHERE p.paid_at >= :since
          AND (s.id IS NULL OR s.archived_at IS NULL)
        GROUP BY DATE(p.paid_at)
        ORDER BY DATE(p.paid_at) ASC
        """, nativeQuery = true)
List<Object[]> findDailyRevenueRaw(@Param("since") LocalDateTime since);
```

native 사용 시 Service 단에서 Object[] → AdminRevenuePoint 매핑. **agent 가 어느 쪽 쉬운지 선택 가능** — 단, archived_at IS NULL 조건이 빠지면 본 step 의 핵심이 깨지므로 반드시 포함.

## 플랜별 분포 쿼리

```java
@Query(value = """
        SELECT
            p.plan AS plan,
            COUNT(*) AS count,
            COALESCE(SUM(p.amount), 0) AS revenue
        FROM payment p
        LEFT JOIN subscription s ON s.payment_id = p.id
        WHERE p.status = 'PAID'
          AND p.paid_at >= :since
          AND (s.id IS NULL OR s.archived_at IS NULL)
        GROUP BY p.plan
        ORDER BY revenue DESC
        """, nativeQuery = true)
List<Object[]> findRevenueByPlanRaw(@Param("since") LocalDateTime since);
```

`p.plan` 은 enum 컬럼이므로 String 으로 반환됨(JPA 가 enum 을 String 으로 저장). Service 에서 `AdminRevenueByPlan` 으로 매핑.

## Controller

```java
@GetMapping("/revenue")
public List<AdminRevenuePoint> revenue(@RequestParam(defaultValue = "30") int days) {
    return service.revenueTrend(clamp(days, 7, 365));
}

@GetMapping("/revenue/by-plan")
public List<AdminRevenueByPlan> revenueByPlan(@RequestParam(defaultValue = "30") int days) {
    return service.revenueByPlan(clamp(days, 7, 365));
}

private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
```

## 검증

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `GET /api/admin/stats/revenue?days=30` 200 응답, 일자별 `{date, revenue, refundAmount, count}` 배열.
2. `GET /api/admin/stats/revenue/by-plan?days=30` 200 응답, 플랜별 `{plan, count, revenue}` 배열, revenue DESC.
3. Step 1 의 archived 구독에 연결된 결제는 두 endpoint 모두에서 빠짐(테스트로 검증).
4. days 가드: 7 미만 또는 365 초과 입력 → clamp.
5. PAID 만 revenue, CANCELLED 만 refundAmount 로 분리(환불 정의가 다르면 service 주석으로 명시).
6. 신규 테스트 ≥ 4건 통과.
7. `gradlew test` 전체 통과.

## 금지 사항

- archived_at 필터를 빼지 마라. **이유**: Step 1 의 분리 의도가 깨지면 phase 전체 의미 상실.
- payment 와 subscription 을 INNER JOIN 하지 마라. **이유**: 일부 결제는 구독 row 매핑이 없을 수 있음(환불·실패 케이스). LEFT JOIN + `(s.id IS NULL OR s.archived_at IS NULL)`.
- 통계 결과를 캐시하지 마라. **이유**: 본 step 범위 외 + admin 페이지 트래픽 미미.
- days 를 무제한 허용하지 마라. **이유**: 365일치 daily group by 도 충분한 상한.

## Status 규칙

- 성공: step 2 `completed`, summary "AdminRevenuePoint + AdminRevenueByPlan + findDailyRevenue/findRevenueByPlan(archived 제외) + GET /api/admin/stats/revenue & /revenue/by-plan + 테스트 4건+, test/compile OK".
- 실패: 3회 재시도 후 `error`.
