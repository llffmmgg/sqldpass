# Step 2 — BillingManager 단위 테스트

## 작업 디렉터리
`mobile/`

## 배경 / Why
- BillingManager 는 Play Store 정책상 가장 민감 — ack 누락 시 자동 환불, 검증 누락 시 entitlement 사고. 회귀 가능성 큰 영역.
- 핵심 분기: PURCHASED / PENDING / UNSPECIFIED, USER_CANCELED / ITEM_ALREADY_OWNED / 기타, verify 실패 시 ack 호출 안 함.

## 변경 대상

### 1. `mobile/app/src/test/java/com/sqldpass/app/billing/BillingManagerTest.kt`
테스트 시나리오:

1. **PURCHASED 결제 → verify 성공 → acknowledgePurchase 호출 → Success 이벤트**
   - mockk<SqldpassApi> 의 verifyPlayBilling 이 정상 반환.
   - mockk<BillingClient> 의 acknowledgePurchase 가 BillingResult OK 반환.
   - events SharedFlow 에 Processing → Success 순으로 emit.

2. **PURCHASED 결제 → verify throw → ack 호출 안 됨 + Failed 이벤트**
   - verifyPlayBilling 이 IOException throw.
   - acknowledgePurchase mock 이 verify { 0 * any() } 검증 (호출 안 됨).
   - events 에 Processing → Failed emit.

3. **PURCHASED + isAcknowledged=true → ack 재호출 안 됨 + Success**
   - 이미 ack 된 결제 (recover 경로) — ack 재시도 안 함.
   - Success emit.

4. **PURCHASED + verify OK + ack 실패 → Success(warning)**
   - verifyPlayBilling OK.
   - acknowledgePurchase 가 NETWORK_ERROR 반환.
   - events 에 Success(warning="...자동으로 동기화...") emit.

5. **PENDING 상태 → handlePurchase 가 Pending 이벤트만**
   - Purchase.PurchaseState.PENDING 인 mock purchase.
   - verifyPlayBilling 호출 안 됨, acknowledgePurchase 호출 안 됨.

6. **listener 의 USER_CANCELED 응답 → Canceled 이벤트**
   - BillingClient listener mock 호출 시 BillingResult.responseCode=USER_CANCELED + 빈 purchases.
   - events 에 Canceled emit.

7. **listener 의 ITEM_ALREADY_OWNED → Failed + recoverPendingPurchases 호출**
   - 응답 코드 ITEM_ALREADY_OWNED.
   - events 에 Failed emit + queryPurchasesAsync 호출 검증.

### 2. BillingClient mocking 전략
- BillingClient.newBuilder() 가 final 이라 직접 모킹 어려움. 대안:
  - **BillingManager 구조 변경 최소화** 하되, listener 노출 가능한 internal 접근자 추가 (단순히 internal val 로 setListener block 을 캡처).
  - 또는 BillingManager 의 핵심 로직 (handlePurchase, verifyAndAcknowledge) 을 internal/visible 로 expose 해 직접 호출.
  - 가장 깔끔: `verifyAndAcknowledge` 와 `handlePurchase` 를 `internal` 가시성으로 + `@VisibleForTesting` 어노테이션 추가.
- MockK 의 `mockkConstructor(BillingClient.Builder::class)` 도 가능하지만 복잡 — 내부 메서드 직접 호출이 더 명확.

### 3. (선택) `mobile/app/build.gradle`
- `testImplementation "androidx.annotation:annotation:1.9.0"` — `@VisibleForTesting` 어노테이션용. 이미 transitive 로 있을 가능성 큼.

## 작업 절차
1. BillingManager 의 internal 메서드 가시성 조정 + `@VisibleForTesting`.
2. BillingManagerTest.kt 작성 — 7 시나리오.
3. gradle 테스트 통과.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\mobile
.\\gradlew.bat :app:testDebugUnitTest --tests "com.sqldpass.app.billing.BillingManagerTest"
```

## 금지사항
- 실제 Play Store IPC 호출 시도 금지. 이유: unit test 영역이라 BillingClient 가 startConnection 못 함 — 모든 BillingClient 호출은 mock.
- public API 표면 (launch, connect, loadProducts, events, productSnapshot) 변경 금지. 이유: 호출처가 안정적이어야 함. 새 internal 가시성만 추가.

## 산출물
- 신규 test 파일 + 시나리오 수.
- BillingManager 의 visibility 변경 diff (한 두 줄).
- 테스트 결과 마지막 5줄.
