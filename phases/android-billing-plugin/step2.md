# Step 2 — BillingPlugin.kt 작성 + MainActivity 등록

## 배경

Step 1 에서 Kotlin/Billing Library 빌드 인프라가 준비됐다. 이제 `frontend/src/lib/platform.ts:15-24` 의 `CapacitorBillingPlugin` 인터페이스를 만족하는 Kotlin 플러그인을 작성한다.

frontend 가 기대하는 시그니처:
```typescript
purchase(options: { productId: string }) => Promise<{
  success: boolean;
  purchaseToken?: string;
  orderId?: string;
  errorCode?: string;
  errorMessage?: string;
}>;
```

호출 흐름:
```
window.Capacitor.Plugins.Billing.purchase({productId: "iap_one_month"})
  → BillingPlugin.purchase(PluginCall)
    → BillingClient.queryProductDetailsAsync(productId)
      → ProductDetails 1개 받기
        → BillingClient.launchBillingFlow(activity, params)
          → 사용자 결제 후 onPurchasesUpdated(result, purchases)
            → call.resolve({success, purchaseToken, orderId})
```

**책임 분리**: `acknowledge()` / `consume()` 호출은 backend `PaymentService.verifyPlayBilling` 가 Google Play Developer API 로 처리한다 (이미 구현됨). plugin 은 `purchaseToken` 만 frontend 로 돌려준다.

## 작업 디렉터리

```
mobile/android/app/src/main/java/com/sqldpass/app/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `mobile/android/app/src/main/java/com/sqldpass/app/BillingPlugin.kt` | 신규 작성 (~180줄) |
| `mobile/android/app/src/main/java/com/sqldpass/app/MainActivity.java` | `onCreate` override 1개, `registerPlugin` 1줄 추가 |

## BillingPlugin.kt 변경 내용

```kotlin
package com.sqldpass.app

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Google Play Billing 결제창을 띄우고 purchaseToken 을 frontend 로 돌려주는 Capacitor 플러그인.
 *
 * frontend(`frontend/src/lib/platform.ts:CapacitorBillingPlugin`) 가 기대하는 시그니처:
 *   purchase({productId}) -> {success, purchaseToken?, orderId?, errorCode?, errorMessage?}
 *
 * acknowledge / consume 은 backend `PaymentService.verifyPlayBilling` 가 처리하므로
 * 이 플러그인은 결제창 호출 + 토큰 반환까지만 담당한다.
 */
@CapacitorPlugin(name = "Billing")
class BillingPlugin : Plugin(), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private var pendingCall: PluginCall? = null

    override fun load() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        connect(null)
    }

    private fun connect(onReady: ((BillingResult) -> Unit)?) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                onReady?.invoke(billingResult)
            }

            override fun onBillingServiceDisconnected() {
                // 다음 purchase() 호출 시 재연결 시도
            }
        })
    }

    @PluginMethod
    fun purchase(call: PluginCall) {
        val productId = call.getString("productId")
        if (productId.isNullOrBlank()) {
            call.reject("productId is required", "INVALID_INPUT")
            return
        }

        if (!billingClient.isReady) {
            connect { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    launchPurchase(call, productId)
                } else {
                    rejectCall(call, "BILLING_UNAVAILABLE", "Play Billing 서비스에 연결할 수 없어요 (${result.responseCode})")
                }
            }
            return
        }

        launchPurchase(call, productId)
    }

    private fun launchPurchase(call: PluginCall, productId: String) {
        pendingCall = call

        val productQuery = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(productQuery))
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { result, productDetailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                resolvePendingError("ITEM_UNAVAILABLE", "상품을 찾을 수 없어요 ($productId)")
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList[0]
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            activity.runOnUiThread {
                val launchResult = billingClient.launchBillingFlow(activity, flowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    resolvePendingError("LAUNCH_FAILED", "결제창을 열 수 없어요 (${launchResult.responseCode})")
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase == null) {
                    resolvePendingError("PURCHASE_EMPTY", "결제 결과가 비어있어요")
                    return
                }
                resolvePendingSuccess(purchase)
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                resolvePendingError("USER_CANCELED", "사용자가 결제를 취소했어요")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                resolvePendingError(
                    "ITEM_ALREADY_OWNED",
                    "이미 보유한 상품이에요. 앱을 다시 시작 후 시도해주세요"
                )
            else ->
                resolvePendingError(
                    "BILLING_ERROR_${result.responseCode}",
                    result.debugMessage ?: "알 수 없는 결제 오류"
                )
        }
    }

    private fun resolvePendingSuccess(purchase: Purchase) {
        val call = pendingCall ?: return
        pendingCall = null
        val response = JSObject().apply {
            put("success", true)
            put("purchaseToken", purchase.purchaseToken)
            put("orderId", purchase.orderId)
        }
        call.resolve(response)
    }

    private fun resolvePendingError(code: String, message: String) {
        val call = pendingCall ?: return
        pendingCall = null
        rejectCall(call, code, message)
    }

    private fun rejectCall(call: PluginCall, code: String, message: String) {
        val response = JSObject().apply {
            put("success", false)
            put("errorCode", code)
            put("errorMessage", message)
        }
        call.resolve(response)
    }

    override fun handleOnDestroy() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
        super.handleOnDestroy()
    }
}
```

## MainActivity.java 변경 내용

기존:
```java
package com.sqldpass.app;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {}
```

수정 후:
```java
package com.sqldpass.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BillingPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
```

`registerPlugin` 은 `super.onCreate` 호출 전에 와야 한다 (Capacitor 7 가이드). Bundle import 추가.

## 검증

```powershell
cd mobile/android
.\gradlew.bat assembleDebug
```

성공 조건:
- 컴파일 통과 — Kotlin/Java 혼합 모듈에서 BillingPlugin.kt 가 정상 인식.
- `app-debug.apk` 생성.

**런타임 검증** (실기기 또는 에뮬레이터):
1. 디버그 APK 설치 → 앱 실행.
2. 사이트 로그인 후 `/checkout` 진입 → 플랜 선택 → 결제 카드 클릭.
3. **테스트 환경** (Play Console > 라이선스 테스트 등록 후): "테스트 카드" 옵션 노출 → 결제 완료 → 백엔드 `/api/payment/play-billing/verify` 200 OK → 구독 발급.

**확인 포인트**:
- 결제 취소 → frontend payment.ts 가 받는 `errorCode === "USER_CANCELED"`.
- 미등록 SKU → `errorCode === "ITEM_UNAVAILABLE"`.
- 정상 결제 → `purchaseToken`, `orderId` 가 frontend 로 전달 → backend verify 통과.

## Acceptance Criteria

1. `mobile/android/app/src/main/java/com/sqldpass/app/BillingPlugin.kt` 신규 파일 작성, `@CapacitorPlugin(name = "Billing")` 어노테이션 + `purchase` PluginMethod 포함.
2. `MainActivity.java` 에 `onCreate(Bundle)` override + `registerPlugin(BillingPlugin.class)` 추가.
3. `gradlew assembleDebug` 통과 (환경 가능 시).
4. frontend `payment.ts:288~310` 의 `startPaymentPlayBilling` 호출이 `window.Capacitor?.Plugins?.Billing` 을 non-undefined 로 받음 (런타임).

## 금지 사항

- BillingPlugin 안에서 acknowledge / consume 을 호출하지 마라. **이유**: backend `PaymentService.verifyPlayBilling` 가 Google Play Developer API 로 acknowledge 처리. plugin 에서 중복 호출 시 정책 분기점이 두 곳으로 갈라져 일관성 깨짐.
- `BillingFlowParams.setObfuscatedAccountId` / `setObfuscatedProfileId` 를 채워 넣지 마라. **이유**: 토큰 도용 방어는 backend `PaymentService.verifyPlayBilling` 의 memberId↔purchaseToken 바인딩(`payment-hardening` phase) 이 담당. 모바일에서 사용자 식별자를 native 로 노출하면 토큰 누출 시 PII 영향 확대.
- `setListener(this)` 외에 별도 BroadcastReceiver 나 WorkManager 를 등록하지 마라. **이유**: 결제 완료 후 사후 상태 변경은 RTDN webhook 이 처리. 클라이언트 사이드 폴링/리스너 추가 시 동기화 충돌.
- `MainActivity.kt` 로 변환하지 마라. **이유**: Capacitor 7 기본 템플릿이 Java. Kotlin 변환은 plugin 한 파일만으로 충분 (Kotlin/Java 혼합 모듈 정상 동작). MainActivity 변환은 별도 cleanup phase.
- BillingClient 를 여러 인스턴스로 만들지 마라. **이유**: Google Play Billing Library 는 BillingClient 하나로 충분하고, 다중 인스턴스 시 `onPurchasesUpdated` 콜백이 분산돼 race condition 발생.
- BillingPlugin 을 npm 패키지로 분리하지 마라. **이유**: sqldpass-mobile 전용 통합. inline 플러그인이 단순/명확.
- Google Play Billing v7 으로 올리지 마라. **이유**: Step 1 의 v6.2.1 결정 유지. v7 도입은 backend `PlayBillingClient` 동시 검증 필요한 별도 phase.

## 커밋

```powershell
git add mobile/android/app/src/main/java/com/sqldpass/app/BillingPlugin.kt \
        mobile/android/app/src/main/java/com/sqldpass/app/MainActivity.java
git commit -m "feat(android-billing): BillingPlugin.kt + MainActivity 등록

- @CapacitorPlugin(name=\"Billing\") + BillingClient + PurchasesUpdatedListener
- purchase(productId) → queryProductDetailsAsync → launchBillingFlow
  → JSObject(success, purchaseToken, orderId) 또는 (errorCode, errorMessage) 응답
- MainActivity.onCreate 에 registerPlugin(BillingPlugin.class) 1줄 추가
- acknowledge/consume 은 backend PaymentService.verifyPlayBilling 가 처리 (책임 분리)
- 토큰 도용 방어는 backend memberId↔purchaseToken 바인딩이 담당 (obfuscatedAccountId 미사용)

frontend `payment.ts:startPaymentPlayBilling` 이 호출하는 인터페이스(CapacitorBillingPlugin) 충족.

Refs: phases/android-billing-plugin/step2.md, frontend/src/lib/platform.ts:15-24"
```

## Status 규칙

- 성공: step 2 status `completed`, summary "BillingPlugin.kt + MainActivity registerPlugin 추가, frontend CapacitorBillingPlugin 인터페이스 충족".
- 실패: 3회 재시도 후에도 컴파일 깨지면 `error` + `error_message`. 흔한 원인: Capacitor Plugin 클래스 시그니처 변경, BillingClient API 시그니처 변경.
- blocked: Android SDK 미설치로 컴파일 검증 불가하면 `blocked` + `blocked_reason: "docs/ANDROID_LAUNCH.md Step 2-2 (Android Studio 설치) 필요"`.
