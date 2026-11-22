package com.neurasamu.build.sl_tasker.security

import android.app.Activity
import android.app.Application
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlayPlan(
    val productId: String,
    val productType: String,
    val fallbackPrice: String
) {
    MONTHLY("sl_tasker_pro_monthly", BillingClient.ProductType.SUBS, "Rs. 250"),
    YEARLY("sl_tasker_pro_yearly", BillingClient.ProductType.SUBS, "Rs. 2,700"),
    LIFETIME("sl_tasker_pro_lifetime_v2", BillingClient.ProductType.INAPP, "Rs. 5,000")
}

data class PlayBillingUiState(
    val isReady: Boolean = false,
    val isBusy: Boolean = false,
    val monthlyPrice: String = PlayPlan.MONTHLY.fallbackPrice,
    val yearlyPrice: String = PlayPlan.YEARLY.fallbackPrice,
    val lifetimePrice: String = PlayPlan.LIFETIME.fallbackPrice,
    val statusMessage: String? = null
)

object PlayBillingManager {

    private var appContext: Context? = null
    private var billingClient: BillingClient? = null
    private val productMap = HashMap<String, ProductDetails>()

    private val _uiState = MutableStateFlow(PlayBillingUiState())
    val uiState: StateFlow<PlayBillingUiState> = _uiState

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    handlePurchases(purchases)
                }
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    statusMessage = "Purchase canceled."
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                syncActivePurchases(userTriggered = true)
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    statusMessage = billingResult.debugMessage.ifBlank { "Google Play purchase failed. Try again." }
                )
            }
        }
    }

    fun init(app: Application) {
        if (billingClient != null) return
        appContext = app.applicationContext
        billingClient = BillingClient.newBuilder(app.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        connectAndQuery()
    }

    fun refresh() {
        val client = billingClient ?: return
        if (client.isReady) {
            queryAllProducts()
            syncActivePurchases(userTriggered = false)
        } else {
            connectAndQuery()
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    private fun connectAndQuery() {
        val client = billingClient ?: return
        if (client.isReady) {
            _uiState.value = _uiState.value.copy(isReady = true)
            queryAllProducts()
            syncActivePurchases(userTriggered = false)
            return
        }
        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _uiState.value = _uiState.value.copy(isReady = true)
                        queryAllProducts()
                        syncActivePurchases(userTriggered = false)
                    } else {
                        _uiState.value = _uiState.value.copy(isReady = false)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _uiState.value = _uiState.value.copy(isReady = false)
                }
            })
        } catch (_: Throwable) {}
    }

    private fun queryAllProducts() {
        val client = billingClient ?: return
        if (!client.isReady) return

        // Query Subscriptions (Monthly + Yearly)
        val subsList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlayPlan.MONTHLY.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlayPlan.YEARLY.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsList)
            .build()

        client.queryProductDetailsAsync(subsParams) { result, detailsResult ->
            val detailsList = detailsResult.productDetailsList
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                var monthly = _uiState.value.monthlyPrice
                var yearly = _uiState.value.yearlyPrice
                synchronized(productMap) {
                    for (pd in detailsList) {
                        productMap[pd.productId] = pd
                        val price = pd.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                            ?.formattedPrice
                        if (!price.isNullOrBlank()) {
                            when (pd.productId) {
                                PlayPlan.MONTHLY.productId -> monthly = price
                                PlayPlan.YEARLY.productId -> yearly = price
                            }
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    monthlyPrice = monthly,
                    yearlyPrice = yearly
                )
            }
        }

        // Query One-Time In-App Purchase (Lifetime)
        val inAppList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlayPlan.LIFETIME.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppList)
            .build()

        client.queryProductDetailsAsync(inAppParams) { result, detailsResult ->
            val detailsList = detailsResult.productDetailsList
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                var lifetime = _uiState.value.lifetimePrice
                synchronized(productMap) {
                    for (pd in detailsList) {
                        productMap[pd.productId] = pd
                        val price = pd.oneTimePurchaseOfferDetails?.formattedPrice
                        if (!price.isNullOrBlank() && pd.productId == PlayPlan.LIFETIME.productId) {
                            lifetime = price
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(lifetimePrice = lifetime)
            }
        }
    }

    fun launchPurchase(activity: Activity, plan: PlayPlan) {
        val client = billingClient
        if (client == null || !client.isReady) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Connecting to Google Play... Please try again in a moment."
            )
            connectAndQuery()
            return
        }

        val details = synchronized(productMap) { productMap[plan.productId] }
        if (details == null) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Loading Google Play pricing... Please try again shortly."
            )
            queryAllProducts()
            return
        }

        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (plan.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Subscription offer not available on Google Play yet."
                )
                return
            }
            paramsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()

        _uiState.value = _uiState.value.copy(isBusy = true, statusMessage = null)
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                statusMessage = result.debugMessage.ifBlank { "Could not open Google Play checkout." }
            )
        }
    }

    fun restorePurchases() {
        val client = billingClient
        if (client == null || !client.isReady) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Connecting to Google Play..."
            )
            connectAndQuery()
            return
        }
        _uiState.value = _uiState.value.copy(isBusy = true, statusMessage = null)
        syncActivePurchases(userTriggered = true)
    }

    private fun syncActivePurchases(userTriggered: Boolean) {
        val client = billingClient ?: return
        val ctx = appContext ?: return
        if (!client.isReady) return

        val lock = Any()
        var subsDone = false
        var inappDone = false
        var subsOk = false
        var inappOk = false
        val activePurchases = mutableListOf<Purchase>()

        fun finalizeSync() {
            if (!subsDone || !inappDone) return
            if (activePurchases.isNotEmpty()) {
                handlePurchases(activePurchases)
                if (userTriggered) {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        statusMessage = "Pro access restored!"
                    )
                }
            } else {
                if (subsOk && inappOk) {
                    LicenseManager.setPro(ctx, false)
                }
                if (userTriggered) {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        statusMessage = "No active Google Play purchases found."
                    )
                }
            }
        }

        val subsQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(subsQuery) { result, list ->
            synchronized(lock) {
                subsDone = true
                subsOk = result.responseCode == BillingClient.BillingResponseCode.OK
                if (subsOk) {
                    activePurchases.addAll(list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED })
                }
                finalizeSync()
            }
        }

        val inappQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(inappQuery) { result, list ->
            synchronized(lock) {
                inappDone = true
                inappOk = result.responseCode == BillingClient.BillingResponseCode.OK
                if (inappOk) {
                    activePurchases.addAll(list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED })
                }
                finalizeSync()
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val ctx = appContext ?: return
        val client = billingClient ?: return
        val validIds = PlayPlan.entries.map { it.productId }.toSet()

        var granted = false
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (purchase.products.none { it in validIds }) continue
            granted = true
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                client.acknowledgePurchase(ackParams) { /* acknowledged */ }
            }
        }
        if (granted) {
            LicenseManager.setPro(ctx, true)
        }
    }
}
