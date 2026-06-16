package com.fantasyfoodplanner.logic

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PREMIUM_MONTHLY = "formix_premium_monthly"
        const val PREMIUM_YEARLY  = "formix_premium_yearly"
    }

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun connect() {
        if (billingClient.isReady) {
            _isConnected.value = true
            queryProducts()
            queryExistingPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryProducts()
                    queryExistingPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
            }
        })
    }

    fun disconnect() {
        billingClient.endConnection()
        _isConnected.value = false
    }

    private fun queryProducts() {
        val productList = listOf(
            PREMIUM_MONTHLY, PREMIUM_YEARLY
        ).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { _, details ->
            _products.value = details
        }
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, purchases ->
            val activePurchases = purchases.filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            val newTier = activePurchases
                .mapNotNull { p ->
                    when {
                        p.products.any { it.contains("premium") } -> SubscriptionTier.PREMIUM
                        else -> null
                    }
                }
                .maxByOrNull { it.ordinal }
                ?: SubscriptionTier.FREE

            SubscriptionManager.setTier(context, newTier)

            activePurchases
                .filter { !it.isAcknowledged }
                .forEach { purchase ->
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                    billingClient.acknowledgePurchase(params) { }
                }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()?.offerToken ?: return
        val paramsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val tier = when {
            purchase.products.any { it.contains("premium") } -> SubscriptionTier.PREMIUM
            else -> return
        }
        SubscriptionManager.setTier(context, tier)

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { }
        }
    }
}
