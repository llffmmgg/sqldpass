# Step 1 — iap_three_day → iap_thunder 일괄 치환 + All Pass 39000 정정

## 작업 디렉터리
모노레포 전체 (ios/ + mobile/ + backend/ + frontend/ + docs/)

## 배경 / Why
- App Store Connect 에서 사용자가 `iap_three_day` product 를 한 번 만들었다가 삭제. Apple 정책상 **삭제된 product ID 는 sqldpass 의 어떤 앱에서도 영구 재사용 불가**.
- 사용자가 새로 `iap_thunder` 로 등록 — 코드 측 매핑 일치 필요.
- 동시에 사용자 결정 가격 ₩39,000 (All Pass) 으로 정정. 직전 잘못 +900 정정 되돌리기.
- `SubscriptionPlan.THREE_DAY` enum 자체는 무변경. DB `subscription.plan` 컬럼 값 그대로.

## 변경 대상 (7 파일)

### A. `ios/Sqldpass/Sqldpass.storekit`
- `productID: "iap_three_day"` → `"iap_thunder"`
- `internalID: "iap_three_day_id"` → `"iap_thunder_id"`
- `displayPrice: "39900"` → `"39000"` (iap_unlimited 행만)

### B. `backend/src/main/resources/application.yaml`
- line 152 (Apple 매핑): `THREE_DAY: ${APP_STORE_SKU_THREE_DAY:iap_three_day}` → `:iap_thunder`
- line 83 (Play Billing 매핑): `THREE_DAY: ${PLAY_BILLING_SKU_THREE_DAY:iap_three_day}` → `:iap_thunder`

### C. `mobile/app/src/main/java/com/sqldpass/app/billing/BillingManager.kt`
- line 276 (PRODUCT_IDS 리스트): `"iap_three_day"` → `"iap_thunder"`

### D. `mobile/app/src/main/java/com/sqldpass/app/ui/passplus/PassPlusCatalogScreen.kt`
- line 55 (CatalogEntry.productId): `"iap_three_day"` → `"iap_thunder"`

### E. `frontend/src/lib/payment.ts`
- line 38 (NEXT_PUBLIC_PLAY_BILLING_SKU_THREE_DAY 기본값): `"iap_three_day"` → `"iap_thunder"`

### F. `backend/src/test/java/com/sqldpass/service/payment/PaymentServiceTest.java`
- `"iap_three_day"` 문자열 일괄 치환 (line 84, 703, 715, 718, 760, 782, 804, 868, 877 — verifyPlayBilling/playBillingProperties 관련)

### G. `docs/ANDROID_LAUNCH.md`
- line 103, 331, 338 의 `iap_three_day` → `iap_thunder` (문서 정합성)

## 검증
- Backend: `.\gradlew.bat test --tests "*PaymentServiceTest*"` — verifyPlayBilling 시나리오들이 iap_thunder 로 매핑되어 통과
- Android: `.\gradlew.bat :app:assembleDebug` — BillingManager / PassPlusCatalogScreen 컴파일 통과
- frontend: `npm run build` 또는 본 변경이 빌드 자체는 안 깨므로 lint 만 가능

## 금지사항
- `SubscriptionPlan.THREE_DAY` enum 이름 변경 금지. 이유: DB subscription.plan 컬럼에 enum name 으로 저장돼 변경 시 기존 구독 데이터 매핑 깨짐.
- 백엔드 `PaymentService.verify` (PortOne 흐름) 의 PAYMENT_AMOUNT_MISMATCH 검증 변경 금지. 이유: 본 phase 는 iOS/Android SKU 매핑만 다루며, 웹 결제 가격(3900) 은 무관.
- iOS .storekit 의 Thunder 외 다른 상품 productID/internalID 변경 금지. 이유: iap_focus / iap_one_month / iap_unlimited 는 그대로 유효.

## 산출물
- 7 파일 diff
- gradlew test 결과 마지막 10줄
- gradlew :app:assembleDebug 결과 마지막 5줄
