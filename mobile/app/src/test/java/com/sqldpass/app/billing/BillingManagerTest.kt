package com.sqldpass.app.billing

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.sqldpass.app.data.VerifyPaymentResponse
import com.sqldpass.app.data.VerifyPlayBillingRequest
import com.sqldpass.app.data.remote.SqldpassApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {

    private fun mockBillingResult(code: Int): BillingResult = mockk(relaxed = true) {
        every { responseCode } returns code
        every { debugMessage } returns ""
    }

    private fun mockPurchase(
        state: Int = Purchase.PurchaseState.PURCHASED,
        productId: String = "iap_one_month",
        acknowledged: Boolean = false,
        token: String = "tok-abc",
    ): Purchase = mockk(relaxed = true) {
        every { purchaseState } returns state
        every { products } returns listOf(productId)
        every { isAcknowledged } returns acknowledged
        every { purchaseToken } returns token
    }

    private fun buildManager(
        api: SqldpassApi,
        billingClient: BillingClient,
        scope: CoroutineScope,
    ): BillingManager = BillingManager(
        api = api,
        scope = scope,
        billingClientFactory = { billingClient },
    )

    private fun stubAcknowledge(billingClient: BillingClient, resultCode: Int) {
        val listenerSlot = slot<AcknowledgePurchaseResponseListener>()
        every {
            billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), capture(listenerSlot))
        } answers {
            listenerSlot.captured.onAcknowledgePurchaseResponse(mockBillingResult(resultCode))
        }
    }

    private suspend fun startCollector(
        scope: CoroutineScope,
        events: SharedFlow<BillingEvent>,
        target: MutableList<BillingEvent>,
    ): kotlinx.coroutines.Job {
        val job = scope.launch { events.toList(target) }
        // replay=0 SharedFlow 라 emit 이전에 구독이 실제 등록될 때까지 양보(yield).
        // TestDispatcher 위에서 yield 한 번이면 launch 가 subscribe 까지 도달.
        repeat(3) { yield() }
        return job
    }

    private fun stubEmptyQueryPurchases(billingClient: BillingClient) {
        val listenerSlot = slot<PurchasesResponseListener>()
        every {
            billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), capture(listenerSlot))
        } answers {
            listenerSlot.captured.onQueryPurchasesResponse(
                mockBillingResult(BillingClient.BillingResponseCode.OK),
                emptyList(),
            )
        }
    }

    @Test
    fun purchased_verifyOk_ack_emitsProcessingThenSuccess() = runTest {
        val api = mockk<SqldpassApi>()
        val billingClient = mockk<BillingClient>(relaxed = true)
        coEvery { api.verifyPlayBilling(any()) } returns VerifyPaymentResponse(null, null, null, null, null)
        stubAcknowledge(billingClient, BillingClient.BillingResponseCode.OK)

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.handlePurchase(mockPurchase())
        advanceUntilIdle()
        job.cancel()

        assertTrue("Processing emit", collected.any { it is BillingEvent.Processing })
        val success = collected.firstOrNull { it is BillingEvent.Success } as? BillingEvent.Success
        assertNotNull("Success emit", success)
        assertEquals("iap_one_month", success!!.productId)
        coVerify(exactly = 1) {
            api.verifyPlayBilling(VerifyPlayBillingRequest("iap_one_month", "tok-abc"))
        }
        verify(exactly = 1) {
            billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any())
        }
    }

    @Test
    fun purchased_verifyThrows_doesNotAck_emitsFailed() = runTest {
        val api = mockk<SqldpassApi>()
        val billingClient = mockk<BillingClient>(relaxed = true)
        coEvery { api.verifyPlayBilling(any()) } throws IOException("server down")

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.handlePurchase(mockPurchase())
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is BillingEvent.Processing })
        assertTrue(collected.any { it is BillingEvent.Failed })
        verify(exactly = 0) {
            billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any())
        }
    }

    @Test
    fun purchased_alreadyAcknowledged_skipsAck_emitsSuccess() = runTest {
        val api = mockk<SqldpassApi>()
        val billingClient = mockk<BillingClient>(relaxed = true)
        coEvery { api.verifyPlayBilling(any()) } returns VerifyPaymentResponse(null, null, null, null, null)

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.handlePurchase(mockPurchase(acknowledged = true))
        advanceUntilIdle()
        job.cancel()

        val success = collected.firstOrNull { it is BillingEvent.Success } as? BillingEvent.Success
        assertNotNull(success)
        assertEquals(null, success!!.warning)
        verify(exactly = 0) {
            billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any())
        }
    }

    @Test
    fun purchased_verifyOk_ackFails_emitsSuccessWithWarning() = runTest {
        val api = mockk<SqldpassApi>()
        val billingClient = mockk<BillingClient>(relaxed = true)
        coEvery { api.verifyPlayBilling(any()) } returns VerifyPaymentResponse(null, null, null, null, null)
        stubAcknowledge(billingClient, BillingClient.BillingResponseCode.NETWORK_ERROR)

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.handlePurchase(mockPurchase())
        advanceUntilIdle()
        job.cancel()

        val success = collected.firstOrNull { it is BillingEvent.Success } as? BillingEvent.Success
        assertNotNull(success)
        assertTrue(
            "warning should mention 자동으로 동기화",
            success!!.warning?.contains("자동으로 동기화") == true,
        )
    }

    @Test
    fun pendingState_emitsPendingOnly() = runTest {
        val api = mockk<SqldpassApi>(relaxed = true)
        val billingClient = mockk<BillingClient>(relaxed = true)

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.handlePurchase(mockPurchase(state = Purchase.PurchaseState.PENDING))
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is BillingEvent.Pending })
        assertTrue(collected.none { it is BillingEvent.Processing })
        coVerify(exactly = 0) { api.verifyPlayBilling(any()) }
        verify(exactly = 0) {
            billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any())
        }
    }

    @Test
    fun listenerUserCanceled_emitsCanceled() = runTest {
        val api = mockk<SqldpassApi>(relaxed = true)
        val billingClient = mockk<BillingClient>(relaxed = true)

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.onPurchasesUpdated(
            mockBillingResult(BillingClient.BillingResponseCode.USER_CANCELED),
            emptyList(),
        )
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is BillingEvent.Canceled })
    }

    @Test
    fun listenerItemAlreadyOwned_emitsFailedAndRecovers() = runTest {
        val api = mockk<SqldpassApi>(relaxed = true)
        val billingClient = mockk<BillingClient>(relaxed = true)
        stubEmptyQueryPurchases(billingClient)

        val manager = buildManager(api, billingClient, this)
        val collected = mutableListOf<BillingEvent>()
        val job = startCollector(this, manager.events, collected)

        manager.onPurchasesUpdated(
            mockBillingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
            null,
        )
        advanceUntilIdle()
        job.cancel()

        assertTrue(collected.any { it is BillingEvent.Failed })
        verify(atLeast = 1) {
            billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any())
        }
    }
}
