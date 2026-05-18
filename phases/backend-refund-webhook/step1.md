# Step 1 — 백엔드 결제 환불 webhook 실 구현

## 작업 디렉터리
`backend/`

## 배경 / Why
- `backend/src/main/java/com/sqldpass/controller/payment/PaymentWebhookController.java` 는 두 endpoint 의 수신·인증까지만 구현되고, 실제 entitlement 동기화는 TODO 주석으로 남아 있음 (P0-3):
  - `POST /api/webhook/play-billing/rtdn` (line 56) — RTDN refund 분기 TODO (line 81-84).
  - `POST /api/webhook/app-store/notifications` (line 128) — notificationType 분기 전부 TODO (line 153-162).
- 결제 모델 결정: 양쪽 일회성/Non-Renewing. 자동 갱신·청구 실패·구독 만료 webhook 은 발생하지 않거나 무시 가능. **환불만 처리하면 됨.**
- 환불 발생 시 백엔드 `subscription.archived_at` 을 세팅해 사용자 entitlement 차단.

## 변경 대상

### 1. SubscriptionService 에 archive API 추가
- 파일: `backend/src/main/java/com/sqldpass/service/payment/SubscriptionService.java` (또는 동등 위치)
- 신규 메서드: `archiveByProviderToken(String provider, String purchaseTokenOrTransactionId)`
  - PROVIDER = PLAY_BILLING | APP_STORE
  - persistent repository 에서 해당 토큰의 구독 row 찾아 `archived_at = now()` 셋
  - 이미 archived 면 idempotent 통과
  - 없으면 WARN log 만 (webhook duplicate / 처리 불가능 케이스)
- Existing pattern 따르기 — `admin-subscription-stats` phase 에서 archived_at 도입됐고 admin 삭제 액션이 동일 패턴 사용 중.

### 2. PaymentWebhookController — RTDN refund 분기 구현
- 파일: `PaymentWebhookController.java`
- Pub/Sub envelope payload (base64 디코딩 후 JSON) 의 `subscriptionNotification.notificationType` 값:
  - `2` = SUBSCRIPTION_RENEWED (Non-Renewing 모델이므로 발생 안 함, 무시)
  - `3` = SUBSCRIPTION_CANCELED (자동 갱신 취소 — Non-Renewing 무관)
  - `12` = SUBSCRIPTION_REVOKED (**환불·차지백·정책 위반으로 entitlement 회수**) → archive
- 또는 `oneTimeProductNotification.notificationType`:
  - `2` = ONE_TIME_PRODUCT_CANCELED (환불) → archive
- 구현:
  ```java
  if (oneTimeProductNotification != null && oneTimeProductNotification.notificationType == 2) {
      subscriptionService.archiveByProviderToken("PLAY_BILLING", oneTimeProductNotification.purchaseToken);
  }
  if (subscriptionNotification != null && subscriptionNotification.notificationType == 12) {
      subscriptionService.archiveByProviderToken("PLAY_BILLING", subscriptionNotification.purchaseToken);
  }
  ```
- TODO 주석(line 81-84) 제거.

### 3. PaymentWebhookController — ASSN v2 REFUND/REVOKE 분기 구현
- `notificationType`:
  - `REFUND` = 환불 → archive
  - `REVOKE` = 환불 (가족 공유 회수) → archive
  - `EXPIRED` = NonRenewing 에서는 자동 갱신 안 함, 백엔드 만료일로 이미 차단 가능, 받아도 archive 호출하면 안전.
  - `DID_RENEW`, `SUBSCRIBED`, `OFFER_REDEEMED` = NonRenewing 무관 → 무시.
- ASSN v2 payload `transactionInfo.transactionId` 또는 `originalTransactionId` 로 토큰 매칭.
- JWS 서명 검증은 본 phase 에서 미구현 (Apple public key 통신·기간성). TODO 코멘트만 남기고 후속 phase 로.
- TODO 주석 분기 (line 153-162) 를 실 구현으로 대체.

### 4. 단위 테스트
- `backend/src/test/java/com/sqldpass/controller/payment/PaymentWebhookControllerTest.java`
- 기존 테스트 패턴 참고 (`admin-refunds-ui` phase 가 webhook 관련 테스트 추가했을 수 있음).
- 시나리오:
  - Play Billing oneTimeProductNotification refund (type=2) → `subscriptionService.archiveByProviderToken` 호출 검증
  - Play Billing subscriptionNotification revoke (type=12) → 동일
  - Play Billing renewed (type=2 of subscription) → archive 호출 안 됨
  - ASSN v2 REFUND → archive 호출
  - ASSN v2 REVOKE → archive 호출
  - ASSN v2 DID_RENEW → archive 호출 안 됨
- Mockito 로 SubscriptionService 모킹.

## 작업 절차
1. SubscriptionService 의 archive 호출 가능 메서드 존재 여부 확인 — `admin-subscription-stats` phase 에서 admin 환불 액션이 이미 archive 메서드를 사용 중일 가능성.
2. 필요한 메서드만 추가 (이미 있으면 그대로).
3. PaymentWebhookController RTDN refund 분기 실 구현.
4. PaymentWebhookController ASSN v2 REFUND 분기 실 구현.
5. JUnit 5 테스트 추가.
6. `.\\gradlew.bat test` 통과.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\backend
.\\gradlew.bat test
```
- 모든 테스트 통과 필수. 컴파일만 되고 테스트 실패는 not OK.

## 금지사항
- ASSN v2 JWS 서명 검증 구현하지 말 것. 이유: Apple public key 인증서 체인 검증은 별도 phase 로 분리. 본 phase 는 페이로드 신뢰만 처리.
- 새 컬럼/마이그레이션 추가하지 말 것. 이유: `archived_at` 컬럼은 이미 V86 (admin-subscription-stats) 에서 존재.
- Provider enum 변경 금지. 이유: PaymentProvider 는 이전 phase 에서 결정된 값.

## 산출물
- 변경 파일 목록 + 핵심 로직 요약.
- `gradlew test` 결과 마지막 10줄.
