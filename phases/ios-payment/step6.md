# Step 6 — StoreKit Configuration + 최종 빌드 검증

## Background

App Store Connect 에 실 상품을 등록하기 전에 시뮬레이터에서 가짜 구매를 테스트하려면 Xcode StoreKit Configuration 파일이 필요. `.storekit` 파일에 정의된 상품을 시뮬레이터의 StoreKit 이 그대로 사용.

또한 본 phase 의 통합 빌드 검증 + 시뮬레이터 스크린샷.

## Workdir

```bash
ios/
```

## Scope

| File | Change |
| --- | --- |
| `ios/Sqldpass/Sqldpass.storekit` | 신규 — StoreKit Configuration (4 상품 정의) |
| `ios/project.yml` | scheme 의 storeKitConfiguration 설정 추가 (또는 수동 scheme 편집 안내) |
| 빌드 + 스크린샷 | 통합 검증 |

## Implementation

### `Sqldpass.storekit` (신규 — JSON 형식)

```json
{
  "identifier": "F8B1A2C3-1111-2222-3333-444455556666",
  "nonRenewingSubscriptions": [],
  "products": [],
  "settings": {
    "_applicationInternalID": "1234567890",
    "_developerTeamID": "TEAMIDXXXX",
    "_lastSynchronizedDate": 0,
    "_locale": "ko_KR",
    "_storefront": "KOR",
    "_storeKitErrors": []
  },
  "subscriptionGroups": [
    {
      "id": "F8B1A2C3-AAAA-BBBB-CCCC-DDDDEEEEFFFF",
      "localizations": [
        {
          "description": "문어CBT 프리미엄",
          "displayName": "프리미엄 구독",
          "locale": "ko_KR"
        }
      ],
      "name": "sqldpass_premium",
      "subscriptions": [
        {
          "adHocOffers": [],
          "codeOffers": [],
          "displayPrice": "3900",
          "familyShareable": false,
          "groupNumber": 4,
          "internalID": "iap_three_day_id",
          "introductoryOffer": null,
          "localizations": [
            {
              "description": "3일 동안 모든 기능 이용",
              "displayName": "Thunder (3일)",
              "locale": "ko_KR"
            }
          ],
          "productID": "iap_three_day",
          "recurringSubscriptionPeriod": "P3D",
          "referenceName": "Thunder",
          "subscriptionGroupID": "F8B1A2C3-AAAA-BBBB-CCCC-DDDDEEEEFFFF",
          "type": "RecurringSubscription",
          "winbackOffers": []
        },
        {
          "adHocOffers": [],
          "codeOffers": [],
          "displayPrice": "2900",
          "familyShareable": false,
          "groupNumber": 3,
          "internalID": "iap_focus_id",
          "introductoryOffer": null,
          "localizations": [
            {
              "description": "30일 집중 학습 플랜",
              "displayName": "Focus (30일)",
              "locale": "ko_KR"
            }
          ],
          "productID": "iap_focus",
          "recurringSubscriptionPeriod": "P1M",
          "referenceName": "Focus",
          "subscriptionGroupID": "F8B1A2C3-AAAA-BBBB-CCCC-DDDDEEEEFFFF",
          "type": "RecurringSubscription",
          "winbackOffers": []
        },
        {
          "adHocOffers": [],
          "codeOffers": [],
          "displayPrice": "9900",
          "familyShareable": false,
          "groupNumber": 2,
          "internalID": "iap_one_month_id",
          "introductoryOffer": null,
          "localizations": [
            {
              "description": "한 달간 모든 기능 + 광고 제거",
              "displayName": "Pro (한 달)",
              "locale": "ko_KR"
            }
          ],
          "productID": "iap_one_month",
          "recurringSubscriptionPeriod": "P1M",
          "referenceName": "Pro",
          "subscriptionGroupID": "F8B1A2C3-AAAA-BBBB-CCCC-DDDDEEEEFFFF",
          "type": "RecurringSubscription",
          "winbackOffers": []
        },
        {
          "adHocOffers": [],
          "codeOffers": [],
          "displayPrice": "29900",
          "familyShareable": false,
          "groupNumber": 1,
          "internalID": "iap_unlimited_id",
          "introductoryOffer": null,
          "localizations": [
            {
              "description": "6개월 무제한 이용권",
              "displayName": "All Pass (6개월)",
              "locale": "ko_KR"
            }
          ],
          "productID": "iap_unlimited",
          "recurringSubscriptionPeriod": "P6M",
          "referenceName": "All Pass",
          "subscriptionGroupID": "F8B1A2C3-AAAA-BBBB-CCCC-DDDDEEEEFFFF",
          "type": "RecurringSubscription",
          "winbackOffers": []
        }
      ]
    }
  ],
  "version": {
    "major": 4,
    "minor": 0
  }
}
```

위 파일을 `ios/Sqldpass/Sqldpass.storekit` 경로로 생성. xcodegen 의 sources path 가 `Sqldpass` 디렉토리라 자동 포함.

### `project.yml` 의 scheme 설정 (선택, xcodegen 한정)

xcodegen 의 `schemes` 섹션에 storeKit 설정을 명시할 수 있지만, 옵션이 안 잡힐 수 있다. 대신 빌드 후 Xcode 에서 한 번 scheme → Edit Scheme → Run → Options → StoreKit Configuration 에 `Sqldpass.storekit` 지정. 본 step 은 파일 생성까지만, scheme 연결은 향후 Xcode UI 작업 안내.

> **시뮬레이터 실 가짜 구매 동작**: Xcode 에서 Run 으로 띄우면 .storekit 활성. `xcrun simctl launch` 로는 활성 안 됨 (scheme 옵션). 다만 본 phase 의 자동 빌드는 `xcodebuild` 라 .storekit 무시 — 사용자가 Xcode UI 에서 한 번 띄워야 진짜 IAP 흐름 검증 가능.

## Validation

### 빌드 검증 (필수)

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build 2>&1 | grep -E "BUILD SUCCEEDED|BUILD FAILED|error:" | head -5
```

기대: `** BUILD SUCCEEDED **`

### 시뮬레이터 통합 (필수)

```bash
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/Sqldpass-* -name "Sqldpass.app" -type d | head -1)
xcrun simctl uninstall booted com.sqldpass.app 2>&1 | head -1
xcrun simctl install booted "$APP_PATH"
xcrun simctl launch booted com.sqldpass.app
sleep 4
xcrun simctl io booted screenshot /tmp/ios-payment-final.png
```

스크린샷 — 로그인 화면(SessionGate) 또는 메인 탭 정상 진입 확인. PaywallView 자체는 Profile → "프리미엄 보기" 탭해야 보이며 자동 캡처는 어려움.

### .storekit 파일 존재

```bash
ls ios/Sqldpass/Sqldpass.storekit
```

## 금지사항

- `.storekit` 파일을 Sqldpass 폴더 밖에 두지 마라. 이유: xcodegen sources path 가 `Sqldpass` — 자동 포함되어야 Xcode 가 인식.
- `displayPrice` 를 ₩ 또는 콤마 포함으로 두지 마라. 이유: StoreKit Testing 은 number 문자열만 인식. `"3900"` 처럼 raw integer 문자열.
- 4개 상품의 `subscriptionGroupID` 가 다르지 않게 하라. 이유: 같은 그룹 내에서만 업그레이드/다운그레이드 가능. 다 같은 ID 사용.
- App Store Connect 의 실 productID 와 다르게 명명 금지. 이유: 출시 시 백엔드 product-id-mapping 과 일치해야 함 — `iap_three_day`, `iap_focus`, `iap_one_month`, `iap_unlimited` 고정.
