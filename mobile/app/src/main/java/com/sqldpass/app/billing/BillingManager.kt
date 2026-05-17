package com.sqldpass.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import com.sqldpass.app.data.VerifyPlayBillingRequest
import com.sqldpass.app.data.remote.SqldpassApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BillingManager(
    context: Context,
    private val api: SqldpassApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var productDetails: Map<String, ProductDetails> = emptyMap()

    private val billingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .setListener { _, purchases ->
            purchases.orEmpty().forEach { purchase ->
                verifyPurchase(purchase)
            }
        }
        .build()

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { loadProducts() }
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    suspend fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf("iap_three_day", "iap_focus", "iap_one_month", "iap_unlimited").map {
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
        val details = productDetails[productId] ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    private fun verifyPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        scope.launch {
            api.verifyPlayBilling(
                VerifyPlayBillingRequest(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                ),
            )
        }
    }
}
