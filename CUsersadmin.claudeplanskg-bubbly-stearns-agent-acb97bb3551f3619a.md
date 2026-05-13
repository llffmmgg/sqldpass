# Admin Refund UI 업그레이드 누적 결제 행 환불 위험 분석

## A) 업그레이드 시점에 옛 SubscriptionEntity의 expires_at이 NOW로 업데이트되는가?

**답: 아니오 — 기존 행은 유지되고, verify 시점에 새 SubscriptionEntity 추가됨**

### 증거
**PaymentService.java:98-100, 176-242**
```java
SubscriptionEntity active = subscriptionRepository
    .findActiveByMemberId(memberId, LocalDateTime.now())
    .stream().findFirst().orElse(null);
UpgradeEvaluation eval = evaluateUpgrade(active, plan, baseAmount);
```
- prepare/verify 모두 활성 구독을 **조회만 함** — 기존 row를 만료시키지 않는다
- verify 성공 시 새 SubscriptionEntity 생성 (Line 234-236):
```java
SubscriptionEntity subscription = new SubscriptionEntity(
    memberId, plan, entity.getId(), paidAt, expiresAt);
subscriptionRepository.save(subscription);
```

### 결론
- 회원이 THREE_DAY ₩3,900 → ONE_MONTH 업그레이드 시
- 기존 subscription row (plan=THREE_DAY, expires_at=3일후) **그대로 유지**
- 새 subscription row (plan=ONE_MONTH, expires_at=30일후) **추가**
- subscription.payment_id UNIQUE (V80) 때문에 payment 1개 = subscription 1개 × N = 다건 가능
- SubscriptionService.getActive()는 isActive()=true인 행들 중 가장 강한 plan 선택 (Line 54-56):
```java
List<SubscriptionEntity> rows = 
    subscriptionRepository.findActiveByMemberId(memberId, LocalDateTime.now());
if (rows.isEmpty()) return Optional.empty();
SubscriptionEntity top = rows.get(0);  // 첫 번째 = 가장 강한 plan
```

---

## B) AdminPaymentRow에 "superseded" 또는 "subscription_active" 필드를 join으로 추가 가능한가?

**답: 가능하나, LEFT JOIN하면 복잡함 — 더 나은 방법 있음**

### 현재 Projection (PaymentRepository.java:27-41)
```java
SELECT new com.sqldpass.controller.admin.AdminPaymentRow(
    p.id, p.paymentId, p.memberId, m.nickname,
    p.plan, p.amount, p.baseAmount, p.prorateDiscount,
    p.status, p.provider,
    p.buyerName, p.buyerEmail, p.buyerPhoneNumber,
    p.paidAt, p.createdAt
)
FROM PaymentEntity p LEFT JOIN MemberEntity m ON m.id = p.memberId
WHERE ...
```

### 문제
1. Subscription JOIN하면:
   ```sql
   LEFT JOIN SubscriptionEntity s ON s.payment_id = p.id
   ```
   - 같은 payment가 다건 subscription을 가질 수 있으므로 (X) — UNIQUE 제약이 있어서 1:1 (O)
   - 그런데 subscription.expiresAt만으로는 판별 불충분:
     - "이 결제의 subscription이 활성인가?" ≠ "이 결제가 현재 활성 권한에 영향을 주는가?"
     - 예: 옛 결제는 subscription이 expires_at < now인 상태 → 마땅히 표시할 상태

2. **"superseded" 판별이 이 쿼리에서 불가능**:
   ```
   같은 memberId의 더 최신 PAID 결제가 존재하는가?
   + 해당 결제의 subscription이 활성인가?
   ```
   → 서브쿼리 필요

### 추천 해결책
**AdminPaymentRow record 확장 (3가지 옵션)**

#### 옵션1: subscription_active 필드 추가 (간단)
```java
public record AdminPaymentRow(
    ...기존 13개...
    Boolean subscriptionActive  // 새 필드: subscription.expiresAt > now
)
```
Projection 수정:
```java
(s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP) AS subscriptionActive
```
JOIN: `LEFT JOIN SubscriptionEntity s ON s.payment_id = p.id`

**장점**: DB 쿼리 간단, 프론트에서 활성 여부 즉시 판별
**단점**: "이 결제가 현재 활성 권한에 영향을 주는가"는 여전히 불명확 (같은 memberId 최신 PAID 필터 필요)

#### 옵션2: subscription_exists 필드만 추가 (가장 간단)
subscription의 존재 여부만 표시 — expiresAt 검사 없음
```sql
s.id IS NOT NULL AS subscriptionExists
```
**장점**: 최소 변경, 쿼리 빠름
**단점**: "활성인지" 판별 못함 (프론트/admin이 expired 구독 row 찾기 어려움)

---

## C) 가장 작은 변경으로 운영 사고를 막을 방법

### 문제 다시 정리
1. 환불 버튼이 모든 PAID 결제에 활성화됨
2. 옛 결제 (superseded) 환불 시 PG 환불은 되지만, 활성 구독(새 결제의 것)은 살아있음
3. 사용자: 환불받고도 서비스 이용 가능

### 해결책 Top 3 (복잡도 순)

#### 1️⃣ 백엔드 가드 (가장 안전, 권장)
**PaymentService.revokePortOnePayment()** 입구에 검증 추가:

```java
@Transactional
public void revokePortOnePayment(Long paymentEntityId, String reason, Long actorAdminId) {
    PaymentEntity entity = paymentRepository.findById(paymentEntityId)
        .orElseThrow(...);
    
    // NEW: 더 최신의 활성 결제가 존재하면 환불 불가
    boolean hasNewerActivePayment = paymentRepository
        .existsNewerActivePaidPaymentForMember(
            entity.getMemberId(), 
            entity.getId(), 
            entity.getPaidAt()
        );
    if (hasNewerActivePayment) {
        throw new SqldpassException(
            ErrorCode.INVALID_INPUT,
            "더 최신의 활성 구독이 있어 환불할 수 없습니다. 해당 결제를 먼저 환불하세요."
        );
    }
    
    // 기존 환불 로직...
    portOneClient.cancel(...);
    ...
}
```

**PaymentRepository 신규 메서드**:
```java
@Query("""
    SELECT COUNT(p) > 0 FROM PaymentEntity p
    WHERE p.memberId = :memberId
      AND p.id != :paymentId
      AND p.paidAt > :paidAt
      AND p.status = 'PAID'
      AND EXISTS (
        SELECT 1 FROM SubscriptionEntity s 
        WHERE s.payment_id = p.id
        AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
      )
    """)
boolean existsNewerActivePaidPaymentForMember(
    @Param("memberId") Long memberId,
    @Param("paymentId") Long paymentId,
    @Param("paidAt") LocalDateTime paidAt
);
```

**장점**:
- 백엔드에서 강제 → 프론트 우회 불가
- PG 환불 전 차단 → "환불받고 서비스 이용" 불가
- 운영자에게 명확한 에러 메시지

**단점**: 쿼리 한 번 추가 (negligible)

---

#### 2️⃣ UI 경고 모달 (빠른 배포, 완전하지 않음)
**AdminPaymentRow에 subscriptionActive 필드 추가** (옵션1):
```java
public record AdminPaymentRow(
    ...
    Boolean subscriptionActive
)
```

프론트: 같은 memberId의 다른 PAID 결제 중 더 최신이면서 subscriptionActive=true인 행이 존재 시
```
⚠️ 경고: 더 최신의 활성 구독이 있습니다. 
이 결제를 환불하면 현재 이용권이 유지됩니다.
정말 진행하시겠습니까?
```

**장점**:
- 즉시 배포 가능
- DB 쿼리 추가 없음 (UNIQUE constraint 존재하므로 JOIN 비용 낮음)

**단점**:
- 운영자가 무시하고 진행 가능 (경고일 뿐)
- 프론트 로직 복잡 (같은 회원 최신 PAID 필터)

---

#### 3️⃣ 환불 버튼 disable 조건부 (UI만)
AdminPaymentRow 프론트에서:
```
refundEnabled = (subscriptionActive == null || !subscriptionActive) 
             && status == "PAID"
```

**장점**: 변경 최소

**단점**: 
- 프론트 조건뿐 → API 직접 호출 우회 가능
- 같은 memberId 최신 활성 결제 판별 안 함

---

### 최종 권장
**백엔드 가드 (1️⃣) + UI 경고 (2️⃣) 조합**:
1. PaymentRepository에 `existsNewerActivePaidPaymentForMember()` 추가
2. PaymentService.revokePortOnePayment() 입구에 검증 추가 → 에러 throw
3. AdminPaymentRow에 subscriptionActive 필드 추가
4. 프론트: subscriptionActive=false 그룹만 환불 버튼 활성

**비용**: 백엔드 20줄 + Projection 쿼리 1줄 + 프론트 조건 1줄

---

## 파일 경로 참고

| 파일 | 역할 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java:362-382` | revokePortOnePayment() 메서드 |
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionEntity.java:67-77` | isActive() 메서드 |
| `backend/src/main/java/com/sqldpass/persistent/payment/SubscriptionService.java:36-47` | revokeByPaymentId() 메서드 |
| `backend/src/main/java/com/sqldpass/controller/admin/AdminPaymentRow.java` | Projection record |
| `backend/src/main/java/com/sqldpass/persistent/payment/PaymentRepository.java:27-46` | findAdminPage() 쿼리 |
| `backend/src/main/java/com/sqldpass/controller/admin/AdminPaymentController.java:74-82` | /refund endpoint |
