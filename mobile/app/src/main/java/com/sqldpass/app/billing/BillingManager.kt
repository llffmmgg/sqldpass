package com.sqldpass.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.sqldpass.app.data.VerifyPlayBillingRequest
import com.sqldpass.app.data.remote.SqldpassApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class BillingProductSnapshot(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
)

/**
 * Play Billing 결제 상태 이벤트. [com.sqldpass.app.MainActivity] 가 수집해
 * 사용자 메시지 + entitlement refresh 를 트리거한다.
 */
sealed class BillingEvent {
    /** Play 결제 UI 가 닫히고 백엔드 검증 진행 중. */
    object Processing : BillingEvent()

    /** 사용자 결제가 PENDING 상태 (예: 송금 대기). 확정될 때까지 entitlement 부여 금지. */
    object Pending : BillingEvent()

    /** 사용자가 결제 시트를 닫음. */
    object Canceled : BillingEvent()

    /**
     * 검증 + ack 가 모두 성공. [warning] 이 null 이 아니면 부분 성공 (예: ack 실패).
     * 본 시점에 호출자는 subscription 상태를 백엔드에서 다시 가져와야 한다.
     */
    data class Success(val productId: String, val warning: String? = null) : BillingEvent()

    /** 결제/검증/ack 중 한 단계 실패. [message] 는 사용자에게 그대로 표시 가능. */
    data class Failed(val message: String) : BillingEvent()
}

class BillingManager(
    context: Context,
    private val api: SqldpassApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var productDetails: Map<String, ProductDetails> = emptyMap()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .setListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases.orEmpty().forEach { handlePurchase(it) }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _events.tryEmit(BillingEvent.Canceled)
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    // 사용자가 이미 보유한 상품을 다시 구매 시도 — entitlement 만 갱신.
                    _events.tryEmit(
                        BillingEvent.Failed("이미 보유 중인 상품입니다. 구독 상태를 갱신했어요."),
                    )
                    scope.launch { recoverPendingPurchases() }
                }
                else -> {
                    val detail = result.debugMessage
                        .takeIf { it.isNotBlank() }
                        ?: "code ${result.responseCode}"
                    _events.tryEmit(BillingEvent.Failed("결제에 실패했어요 ($detail)."))
                }
            }
        }
        .build()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadProducts()
                        // 앱이 결제 직후 종료됐을 경우를 위해 미확인(unacknowledged) 결제를 복구.
                        recoverPendingPurchases()
                    }
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    suspend fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PRODUCT_IDS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        productDetails = result.productDetailsList.orEmpty().associateBy { it.productId }
    }

    fun launch(activity: Activity, productId: String) {
        val details = productDetails[productId] ?: run {
            _events.tryEmit(BillingEvent.Failed("상품 정보를 아직 불러오지 못했어요. 잠시 후 다시 시도해 주세요."))
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(BillingEvent.Failed("결제 화면을 열 수 없어요 (code ${result.responseCode})."))
        }
    }

    /**
     * 카탈로그 화면이 사용하는 가격·이름 스냅샷. `loadProducts()` 호출 후 채워진다.
     * 빈 리스트면 카탈로그가 "가격 정보 로드 중" 으로 표시한다.
     */
    fun productSnapshot(): List<BillingProductSnapshot> =
        productDetails.values.map { d ->
            val offer = d.oneTimePurchaseOfferDetails
            BillingProductSnapshot(
                productId = d.productId,
                title = d.name,
                description = d.description,
                formattedPrice = offer?.formattedPrice.orEmpty(),
            )
        }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                _events.tryEmit(BillingEvent.Processing)
                scope.launch { verifyAndAcknowledge(purchase) }
            }
            Purchase.PurchaseState.PENDING -> {
                // 송금/현금 결제 등이 확정될 때까지 entitlement 부여 금지.
                _events.tryEmit(BillingEvent.Pending)
            }
            else -> Unit // UNSPECIFIED — 무시
        }
    }

    /**
     * 백엔드 검증 → 성공 시 ack. 검증 실패 시 ack 하지 않아 다음 [recoverPendingPurchases] 에서 재시도.
     * Google Play 정책: 구매 후 3일 내 ack 없으면 자동 환불.
     */
    private suspend fun verifyAndAcknowledge(purchase: Purchase) {
        val productId = purchase.products.firstOrNull()
        if (productId == null) {
            _events.tryEmit(BillingEvent.Failed("결제 정보가 비어 있어요."))
            return
        }

        try {
            api.verifyPlayBilling(
                VerifyPlayBillingRequest(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                ),
            )
        } catch (_: Exception) {
            // 검증 실패 시 ack 하지 않음 — 다음 launch 의 recoverPendingPurchases 가 다시 시도.
            // 3일 안에 ack 되지 않으면 Google 이 환불 처리.
            _events.tryEmit(
                BillingEvent.Failed("결제 검증에 실패했어요. 잠시 후 다시 앱을 켜면 자동으로 복구돼요."),
            )
            return
        }

        if (purchase.isAcknowledged) {
            _events.tryEmit(BillingEvent.Success(productId))
            return
        }

        val ackResult = try {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build(),
            )
        } catch (_: Exception) {
            null
        }

        if (ackResult?.responseCode == BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(BillingEvent.Success(productId))
        } else {
            // 검증은 됐는데 ack 만 실패 — entitlement 는 있으니 사용자에겐 성공으로 보이게.
            // 다음 launch 에서 recoverPendingPurchases 가 ack 재시도.
            _events.tryEmit(
                BillingEvent.Success(
                    productId = productId,
                    warning = "구매가 완료됐어요. 잠시 후 자동으로 동기화돼요.",
                ),
            )
        }
    }

    /**
     * 앱 시작 시 호출. Google 보유 결제 목록 중 ack 되지 않은 PURCHASED 항목을
     * 백엔드 검증 + ack 재시도한다.
     *
     * - 결제 직후 앱이 강제 종료된 경우
     * - 백엔드 검증이 일시 실패해 ack 못 했던 경우
     * 모두 본 경로로 복구된다.
     */
    private suspend fun recoverPendingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        for (purchase in result.purchasesList) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (purchase.isAcknowledged) continue
            verifyAndAcknowledge(purchase)
        }
    }

    companion object {
        private val PRODUCT_IDS = listOf(
            "iap_three_day",
            "iap_focus",
            "iap_one_month",
            "iap_unlimited",
        )
    }
}
