# Step 7 — Admin audit 통합 (expireManual revoke + history 기록)

## 배경

P2-8: `AdminSubscriptionService.expireManual` (`backend/src/main/java/com/sqldpass/service/payment/AdminSubscriptionService.java:96-101`) 이 SubscriptionEntity 를 delete 한다. 환불·만료 추적이 끊겨 운영상 사용자에게 다시 부여하거나 분쟁 시 근거가 사라진다.

해결: delete → `sub.revoke(LocalDateTime.now())` (이미 SubscriptionEntity 에 존재 — `SubscriptionEntity.java:75`). 그리고 step 2 에서 만든 `SubscriptionHistoryService.record(...)` 를 호출해 audit row 생성.

추가로 다음 시점에도 audit 기록:

- `AdminSubscriptionService.grantManual` → action=GRANTED
- `AdminSubscriptionService.expireManual` → action=EXPIRED
- `PaymentService.revokePlayBillingByToken` → action=REFUNDED, actorAdminId=null (시스템 통보)
- `SubscriptionService.revokeByPaymentId` → action=REVOKED, actorAdminId=null

`actorAdminId` 는 어드민 컨트롤러가 인터셉터 attribute (`request.getAttribute("memberId")`) 로 추출해 service 메서드에 인자로 전달.

## 작업 디렉터리

```
backend/
```

## 변경 대상

수정 4개:

| 파일 | 변경 |
|------|------|
| `backend/src/main/java/com/sqldpass/service/payment/AdminSubscriptionService.java` | expireManual: delete → revoke + history. grantManual: history. 시그니처에 `Long actorAdminId` 추가 |
| `backend/src/main/java/com/sqldpass/service/payment/SubscriptionService.java` | revokeByPaymentId 에 history 기록 |
| `backend/src/main/java/com/sqldpass/service/payment/PaymentService.java` | revokePlayBillingByToken 에 history 기록 |
| 어드민 컨트롤러 (예: `controller/admin/AdminSubscriptionController.java` — grep 으로 정확 위치 확인) | service 호출 시 actorAdminId 전달 |

신규 1개 (테스트):

| 파일 | 역할 |
|------|------|
| `backend/src/test/java/com/sqldpass/service/payment/AdminSubscriptionServiceTest.java` | grantManual/expireManual 의 history mock 호출 검증 |

`PaymentServiceTest` 의 `revokePlayBillingByToken_*` 케이스에 `historyService.record(...)` mock 호출 검증 추가.

## AdminSubscriptionService 변경

### 시그니처 변경

```java
public AdminSubscriptionRow grantManual(Long memberId, SubscriptionPlan plan, String reason, Long actorAdminId)
public void expireManual(Long subscriptionId, String reason, Long actorAdminId)
```

### 생성자 주입에 historyService 추가

`@RequiredArgsConstructor` + `private final SubscriptionHistoryService historyService;`

### grantManual 내부

기존 처리 후, save 직후:

```java
historyService.record(memberId, plan, SubscriptionHistoryAction.GRANTED, reason,
        actorAdminId, /* paymentId */ null);
```

### expireManual 내부 — 동작 변경

```java
SubscriptionEntity sub = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new SqldpassException(ErrorCode.INVALID_INPUT, "구독을 찾을 수 없습니다."));
sub.revoke(LocalDateTime.now());                      // ← delete 가 아니라 expiresAt=now
historyService.record(sub.getMemberId(), sub.getPlan(),
        SubscriptionHistoryAction.EXPIRED, reason, actorAdminId, sub.getPaymentId());
```

`subscriptionRepository.delete(sub)` 호출 제거.

`@Transactional` 호출자 트랜잭션 안에서 `sub.revoke` 는 더티 체크로 flush 됨.

## SubscriptionService.revokeByPaymentId 변경

`@RequiredArgsConstructor` 에 `SubscriptionHistoryService historyService` 추가.

```java
@Transactional
public boolean revokeByPaymentId(Long paymentId) {
    if (paymentId == null) return false;
    var found = subscriptionRepository.findByPaymentId(paymentId);
    if (found.isEmpty()) return false;
    SubscriptionEntity sub = found.get();
    sub.revoke(LocalDateTime.now());
    historyService.record(sub.getMemberId(), sub.getPlan(),
            SubscriptionHistoryAction.REVOKED, "revokeByPaymentId", null, paymentId);
    return true;
}
```

## PaymentService.revokePlayBillingByToken 변경

생성자 주입에 `SubscriptionHistoryService historyService` 추가.

기존 success 분기 (line 312~) 에:

```java
sub.get().revoke(LocalDateTime.now());
historyService.record(payment.getMemberId(), payment.getPlan(),
        SubscriptionHistoryAction.REFUNDED, "play:rtdn-refund", null, payment.getId());
```

## 어드민 컨트롤러 수정

`controller/admin/` 안의 구독 관련 컨트롤러 위치를 grep:

```
grep -rn "AdminSubscriptionService" backend/src/main/java/com/sqldpass/controller/
```

해당 컨트롤러에서 service 호출 시 `Long actorAdminId = (Long) request.getAttribute("memberId");` 로 추출 후 메서드 인자에 전달. 인터셉터가 `/api/admin/**` 에 적용되므로 `memberId` 는 admin memberId.

호출 예:

```java
adminSubscriptionService.grantManual(memberId, plan, reason, actorAdminId);
adminSubscriptionService.expireManual(subscriptionId, reason, actorAdminId);
```

## 새 테스트 (AdminSubscriptionServiceTest)

`@ExtendWith(MockitoExtension.class)` mock 4개: SubscriptionRepository, MemberRepository, SubscriptionHistoryService, SubscriptionEntity helper.

- `grantManual_GRANTED_history_기록` — historyService.record 1회 + 인자 일치 검증
- `expireManual_revoke_now_호출_및_EXPIRED_history_기록` — sub.getExpiresAt() 이 now 근처 + history 기록 + delete 호출 0회

## PaymentServiceTest 보강

기존 `revokePlayBillingByTokenExpiresSubscription` 에 `verify(historyService).record(...)` 추가. setUp 에 `@Mock SubscriptionHistoryService historyService` 추가 + 생성자 인자 갱신.

## 검증

```powershell
cd backend
.\gradlew.bat test --tests "com.sqldpass.service.payment.*"
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Acceptance Criteria

1. `AdminSubscriptionService.expireManual` 가 `subscriptionRepository.delete(...)` 호출하지 않고 `sub.revoke(now)` + `historyService.record(EXPIRED, ...)` 한다.
2. `grantManual`, `expireManual` 시그니처에 `Long actorAdminId` 가 추가됐다.
3. `SubscriptionService.revokeByPaymentId` 가 history REVOKED 기록.
4. `PaymentService.revokePlayBillingByToken` 가 history REFUNDED 기록.
5. 어드민 컨트롤러가 `actorAdminId` 를 추출해 service 호출에 전달.
6. AdminSubscriptionServiceTest 신규 케이스 + PaymentServiceTest 보강 케이스 모두 통과.
7. `gradlew.bat test` 전체 통과.

## 금지 사항

- SubscriptionEntity 에 새 필드를 추가하지 마라. 이유: audit 정보는 history 테이블에 분리 — entity 가 비대해지지 않도록.
- history 기록 실패가 본 비즈니스 로직(revoke/grant/expire) 을 깨도록 두지 마라 — REQUIRES_NEW + try/catch 가 step 2 에서 swallow 보장. 이유: 핵심 비즈니스 가용성 우선.
- expireManual 의 revoke(now) 시 audit 의 reason 이 비어 있어도 무방하지만 null 은 허용하지 마라 (빈 문자열 OK). 이유: NOT NULL 컬럼 아닌데 운영상 검색 편의.
- 어드민 컨트롤러의 인증 인터셉터 등록 (`/api/admin/**`) 을 변경하지 마라. 이유: 별도 책임.

## Status 규칙

- 성공: step 7 `completed`, summary 에 "expireManual revoke+history, grant/refund/revoke 4 시점 audit, 어드민 컨트롤러 actorAdminId 전달, 테스트 보강 OK".
- 실패: 3회 재시도 후 실패면 `error`.
- blocked: 어드민 컨트롤러 위치/시그니처 변경 충돌 시 `blocked`.
