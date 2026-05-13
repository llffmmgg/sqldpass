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
 * Google Play Billing 결제창 호출 + purchaseToken 반환 전용 Capacitor 플러그인.
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
                    rejectCall(
                        call,
                        "BILLING_UNAVAILABLE",
                        "Play Billing 서비스에 연결할 수 없어요 (${result.responseCode})"
                    )
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
                    resolvePendingError(
                        "LAUNCH_FAILED",
                        "결제창을 열 수 없어요 (${launchResult.responseCode})"
                    )
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
