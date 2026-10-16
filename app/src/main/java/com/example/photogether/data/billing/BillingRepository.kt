package com.example.photogether.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.photogether.core.analytics.Analytics
import com.example.photogether.core.analytics.AnalyticsEvent
import com.example.photogether.core.analytics.NoOpAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing for the Premium subscription.
 *
 * [isPremium] is true only while Play reports a purchased subscription. It is refreshed from
 * queryPurchasesAsync when the client connects, so a reinstalled app regains Premium through
 * restore without a separate restore button.
 *
 * The product ids must exist as subscriptions in Play Console. Prices are read from Play at
 * runtime and are never hardcoded.
 */
class BillingRepository(
    context: Context,
    private val analytics: Analytics = NoOpAnalytics,
) : PurchasesUpdatedListener {

    companion object {
        const val MONTHLY_PRODUCT_ID = "premium_monthly"
        const val YEARLY_PRODUCT_ID = "premium_yearly"
        val PRODUCT_IDS: Set<String> = setOf(MONTHLY_PRODUCT_ID, YEARLY_PRODUCT_ID)
    }

    private val appContext = context.applicationContext

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val productDetails = mutableMapOf<String, ProductDetails>()
    private val _prices = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Formatted price from Play per product id, once loaded. Empty until then. */
    val prices: StateFlow<Map<String, String>> = _prices.asStateFlow()

    private var connecting = false

    /** Opens the Play connection, then loads products and restores purchases. Safe to call again. */
    fun connect() {
        if (client.isReady || connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    refreshPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
            }
        })
    }

    /** Starts the purchase sheet for [productId]. Does nothing if the product is not loaded yet. */
    fun launchPurchase(activity: Activity, productId: String): Boolean {
        val details = productDetails[productId] ?: return false
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            .build()
        return client.launchBillingFlow(activity, flow).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
            // User cancelled, or a Play error. Premium state is left as it was.
            return
        }
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                acknowledgeIfNeeded(purchase)
                analytics.log(AnalyticsEvent.PURCHASE_COMPLETED, mapOf("product" to purchase.products.joinToString()))
            }
        }
        refreshPurchases()
    }

    /** Re-reads owned subscriptions from Play. Keeps the last known value if the query fails. */
    fun refreshPurchases() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val statuses = purchases.map { purchase ->
                purchaseStatusOf(purchase.purchaseState, purchase.products, PRODUCT_IDS)
            }
            purchases.filter { purchaseStatusOf(it.purchaseState, it.products, PRODUCT_IDS) == PurchaseStatus.PURCHASED }
                .forEach { acknowledgeIfNeeded(it) }
            _isPremium.value = isPremiumFrom(statuses)
        }
    }

    private fun loadProducts() {
        val products = PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            productDetails.clear()
            details.forEach { productDetails[it.productId] = it }
            _prices.value = details.mapNotNull { d ->
                val phase = d.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()
                phase?.let { d.productId to it.formattedPrice }
            }.toMap()
        }
    }

    /** Unacknowledged purchases are refunded by Play after three days, so acknowledge at once. */
    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { }
    }
}
