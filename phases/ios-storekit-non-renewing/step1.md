# Step 1 — iOS .storekit Non-Renewing 전환 + StoreKitService 점검

## 작업 디렉터리
`ios/`

## 배경 / Why
- 이전 phase 의 결제 모델 결정: 양쪽 일회성/Non-Renewing 으로 통일.
- 현재 iOS `Sqldpass.storekit` 의 4 상품(iap_three_day/iap_focus/iap_one_month/iap_unlimited)은 `"type": "RecurringSubscription"` 인 자동 갱신 구독으로 정의됨 (P0-2).
- Android (`BillingManager.kt`) 는 `ProductType.INAPP` 일회성 비소모성 상품으로 호출. 백엔드는 양측을 기간 만료식 entitlement 로 통일 처리 중.
- 자동 갱신 → 일회성으로 바꾸지 않으면 App Store 가 갱신 이벤트를 발생시키고 백엔드 webhook 이 stub 이라 사용자 매출/취소 추적이 깨짐.

## 변경 대상

### 1. `ios/Sqldpass/Sqldpass.storekit`
- 현재 `"subscriptionGroups"` 배열에 4개의 `RecurringSubscription` 정의가 있음.
- StoreKit Configuration 파일의 NonRenewing 표현은 최상위 `"nonRenewingSubscriptions": []` 배열에 항목을 두는 형식.
- 변환: `subscriptionGroups[0].subscriptions[]` 4개를 `nonRenewingSubscriptions[]` 4개로 옮긴다.
- 각 NonRenewing 항목 스키마:
  ```
  {
    "displayPrice": "3900",
    "familyShareable": false,
    "internalID": "iap_three_day_id",
    "localizations": [...],
    "productID": "iap_three_day",
    "referenceName": "Thunder",
    "type": "NonRenewingSubscription"
  }
  ```
- `recurringSubscriptionPeriod`, `subscriptionGroupID`, `groupNumber` 는 NonRenewing 에서 의미 없으므로 제거.
- `subscriptionGroups` 는 빈 배열 `[]` 로 둔다 (또는 유지하되 subscriptions 만 비움).

### 2. `ios/Sqldpass/Services/StoreKitService.swift` 점검
- `Product.products(for: ids)` — productID 만으로 fetch 하므로 NonRenewing 도 그대로 동작 (확인 필요).
- `product.purchase()` 결과의 `.success(let verification)` → `Transaction.PurchaseResult` 분기는 동일.
- **차이점:**
  - NonRenewing 은 `Transaction.currentEntitlements` 에 포함되지 않음 (Apple 문서: "Returns all the auto-renewable subscriptions, non-consumables, and non-renewing subscriptions the customer is currently entitled to."). 실제로는 포함되지만 만료 판단이 다름 — Apple 은 만료일을 추적 안 함, 앱이 추적해야 함.
  - 본 앱은 entitlement 를 **백엔드 `/api/payment/subscription`** 에 위임하므로 `currentEntitlements()` 사용 위치만 점검하면 됨.
- `Transaction.updates` — NonRenewing 도 새 구매 시 이벤트 발생. 갱신 이벤트는 없음 (자동 갱신이 아니므로). 그대로 OK.
- `AppStore.sync()` — 복원 시 NonRenewing 도 동일 동작.

### 3. `ios/Sqldpass/Features/Paywall/PaywallViewModel.swift` 점검
- `syncCurrentEntitlements()` 가 모든 entitlement 를 순회 verify 함. NonRenewing 도 entitlement 에 포함되니 그대로 동작.
- 다만 "구독 자동 갱신" 표현이 UI 에 있으면 NonRenewing 에 맞춰 톤 조정 필요.

### 4. `ios/Sqldpass/Features/Paywall/PaywallView.swift` 점검 (선택)
- "구독" 단어를 "이용권" 으로 바꿔야 할 수 있음 (App Store 정책상 NonRenewing 은 subscription 표기 가능하지만 자동 갱신 안내는 부정확).
- 사용자 가시 문구만 1-2 곳 점검.

## 작업 절차
1. `Sqldpass.storekit` 의 4 상품을 `nonRenewingSubscriptions` 배열로 옮기는 JSON 편집.
2. `StoreKitService.swift` 전체 읽고 `currentEntitlements` / `Transaction.updates` / `AppStore.sync` 사용처가 NonRenewing 에서도 안전한지 확인. 필요 시 주석 추가.
3. `PaywallViewModel.swift` 와 `PaywallView.swift` 의 UI 문구 점검. "자동 갱신" 같은 표현이 있으면 NonRenewing 에 맞게 수정.
4. 변경 요약 README 없음 — commit message 만.

## 검증
- **빌드:** macOS 환경 아님 (Windows). `xcodebuild` 실행 불가 → 이 step 은 **코드 변경만**. 사용자가 macOS 에서 다음 검증 수행 필요:
  ```bash
  cd ios
  xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass \\
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \\
    -configuration Debug build
  ```
- **JSON 정합성:** `python -c "import json; json.load(open('ios/Sqldpass/Sqldpass.storekit'))"` 로 파싱 가능 여부만 빠르게 확인 가능 (Windows 에서 가능).
- **App Store Connect 실 등록:** 본 step 의 변경은 시뮬레이션용. 실제 출시 시 App Store Connect 의 상품 타입도 NonRenewing 으로 등록 필요 — 이건 사용자가 콘솔에서 별도 작업.

## 금지사항
- StoreKitService.swift 의 핵심 로직(purchase 흐름, verification 콜, finish 시점)을 바꾸지 말 것. 이유: 본 phase 는 product type 정합만 다루며 결제 흐름은 ios-payment phase 에서 검증된 그대로 유지.
- productID 4개를 바꾸지 말 것. 이유: 백엔드 `SubscriptionPlan` enum 의 `iap_*` 매핑과 일치해야 함.
- 가격(`displayPrice`) 을 바꾸지 말 것. 이유: 영수증 검증 시 백엔드가 검증.

## 산출물
- 수정된 파일 목록과 각 변경의 이유 1-2줄 요약을 보고할 것.
- macOS 빌드 미수행 사실을 명시할 것.
