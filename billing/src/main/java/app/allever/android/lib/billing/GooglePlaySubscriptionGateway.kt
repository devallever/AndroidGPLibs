//package com.yue.tan.tweet.module.premium
//
//import android.app.Activity
//import android.content.Context
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import com.android.billingclient.api.AcknowledgePurchaseParams
//import com.android.billingclient.api.BillingClient
//import com.android.billingclient.api.BillingClientStateListener
//import com.android.billingclient.api.BillingFlowParams
//import com.android.billingclient.api.BillingResult
//import com.android.billingclient.api.Purchase
//import com.android.billingclient.api.PurchasesUpdatedListener
//import com.android.billingclient.api.QueryProductDetailsParams
//import com.android.billingclient.api.QueryPurchasesParams
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlin.coroutines.resume
//
//class GooglePlaySubscriptionGateway(
//    private val context: Context
//) : SubscriptionGateway {
//
//    companion object {
//        private const val TAG = "Premium|Billing"
//    }
//
//    private var billingClient: BillingClient? = null
//    private var purchaseListener: SubscriptionGateway.PurchaseListener? = null
//    private val mainHandler = Handler(Looper.getMainLooper())
//
//    override val isConnected: Boolean
//        get() = billingClient?.isReady == true
//
//    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
//        handlePurchasesUpdated(billingResult, purchases)
//    }
//
//    override fun connect(callback: (Boolean) -> Unit) {
//        Log.d(TAG, "connect() called, isReady=${billingClient?.isReady}")
//        if (billingClient?.isReady == true) {
//            Log.d(TAG, "connect: already connected")
//            callback(true)
//            return
//        }
//        billingClient = BillingClient.newBuilder(context.applicationContext)
//            .setListener(purchasesUpdatedListener)
//            .enablePendingPurchases(
//                com.android.billingclient.api.PendingPurchasesParams.newBuilder()
//                    .enableOneTimeProducts()
//                    .build()
//            )
//            .build()
//
//        billingClient?.startConnection(object : BillingClientStateListener {
//            override fun onBillingSetupFinished(result: BillingResult) {
//                val success = result.responseCode == BillingClient.BillingResponseCode.OK
//                Log.d(TAG, "onBillingSetupFinished: code=${result.responseCode}, msg=${result.debugMessage}, success=$success")
//                callback(success)
//            }
//
//            override fun onBillingServiceDisconnected() {
//                Log.w(TAG, "onBillingServiceDisconnected: billing service disconnected, will auto-reconnect")
//                billingClient?.startConnection(this)
//            }
//        })
//    }
//
//    override fun disconnect() {
//        Log.d(TAG, "disconnect()")
//        billingClient?.endConnection()
//        billingClient = null
//    }
//
//    override suspend fun queryPlans(): Result<List<SubscriptionPlan>> =
//        suspendCancellableCoroutine { cont ->
//            Log.d(TAG, "queryPlans: requesting SKUs=${SubscriptionPlan.ALL_SKUS}")
//            val params = QueryProductDetailsParams.newBuilder()
//                .setProductList(
//                    SubscriptionPlan.ALL_SKUS.map { sku ->
//                        QueryProductDetailsParams.Product.newBuilder()
//                            .setProductId(sku)
//                            .setProductType(BillingClient.ProductType.SUBS)
//                            .build()
//                    }
//                ).build()
//
//            billingClient?.queryProductDetailsAsync(params) { billingResult, queryResult ->
//                if (cont.isCompleted) return@queryProductDetailsAsync
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    val productDetailsList = queryResult.productDetailsList
//                    val unfetched = queryResult.unfetchedProductList
//                    Log.d(TAG, "queryPlans: OK, found ${productDetailsList.size} plans, unfetched=${unfetched.size}")
//                    if (unfetched.isNotEmpty()) {
//                        unfetched.forEach { Log.w(TAG, "  unfetched SKU: ${it.productId}, statusCode=${it.statusCode}") }
//                    }
//                    val plans = mutableListOf<SubscriptionPlan>()
//                    productDetailsList.forEach { pd ->
//                        SubscriptionPlan.fromProductDetails(pd, context)?.let { plans.add(it) }
//                    }
//                    Log.d(TAG, "queryPlans: parsed ${plans.size} plans: ${plans.map { it.id }}")
//                    cont.resume(Result.success(plans))
//                } else {
//                    Log.e(TAG, "queryPlans: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
//                    cont.resume(
//                        Result.failure(
//                            BillingException(billingResult.responseCode, "queryPlans: ${billingResult.debugMessage}")
//                        )
//                    )
//                }
//            } ?: run {
//                Log.e(TAG, "queryPlans: BillingClient not connected")
//                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
//            }
//        }
//
//    override fun launchPurchase(activity: Activity, plan: SubscriptionPlan) {
//        Log.d(TAG, "launchPurchase: planId=${plan.id}, badge=${plan.badgeKey}, price=${plan.priceFormatted}")
//        val productDetails = plan.raw as? com.android.billingclient.api.ProductDetails
//            ?: throw IllegalStateException("SubscriptionPlan.raw (ProductDetails) is null, ensure queryPlans was called first")
//
//        val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
//        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
//            .setProductDetails(productDetails)
//        offer?.offerToken?.let {
//            paramsBuilder.setOfferToken(it)
//            Log.d(TAG, "launchPurchase: using offerToken=$it")
//        }
//
//        val productDetailsParamsList = listOf(paramsBuilder.build())
//        val flowParams = BillingFlowParams.newBuilder()
//            .setProductDetailsParamsList(productDetailsParamsList)
//            .build()
//
//        val result = billingClient?.launchBillingFlow(activity, flowParams)
//        Log.d(TAG, "launchBillingFlow returned: code=${result?.responseCode}, msg=${result?.debugMessage}")
//    }
//
//    override suspend fun queryPurchases(): Result<List<SubscriptionPurchase>> =
//        suspendCancellableCoroutine { cont ->
//            Log.d(TAG, "queryPurchases: requesting active subscriptions")
//            val params = QueryPurchasesParams.newBuilder()
//                .setProductType(BillingClient.ProductType.SUBS)
//                .build()
//
//            billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
//                if (cont.isCompleted) return@queryPurchasesAsync
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    val subs = purchases.map(::toSubscriptionPurchase)
//                    Log.d(TAG, "queryPurchases: OK, ${subs.size} purchases active=${subs.filter { it.isActive }.size}")
//                    cont.resume(Result.success(subs))
//                } else {
//                    Log.e(TAG, "queryPurchases: FAILED code=${billingResult.responseCode}")
//                    cont.resume(
//                        Result.failure(
//                            Exception("queryPurchases error: code=${billingResult.responseCode}")
//                        )
//                    )
//                }
//            } ?: run {
//                Log.e(TAG, "queryPurchases: BillingClient not connected")
//                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
//            }
//        }
//
//    override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> =
//        suspendCancellableCoroutine { cont ->
//            Log.d(TAG, "acknowledgePurchase: token=$purchaseToken")
//            //todo服务端验证
//            val params = AcknowledgePurchaseParams.newBuilder()
//                .setPurchaseToken(purchaseToken)
//                .build()
//
//            billingClient?.acknowledgePurchase(params) { billingResult ->
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    Log.d(TAG, "acknowledgePurchase: OK")
//                    cont.resume(Result.success(Unit))
//                } else {
//                    Log.e(TAG, "acknowledgePurchase: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
//                    cont.resume(
//                        Result.failure(
//                            Exception("acknowledgePurchase error: code=${billingResult.responseCode}")
//                        )
//                    )
//                }
//            } ?: run {
//                Log.e(TAG, "acknowledgePurchase: BillingClient not connected")
//                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
//            }
//        }
//
//    override suspend fun restorePurchases(): Result<List<SubscriptionPurchase>> {
//        Log.d(TAG, "restorePurchases: delegate to queryPurchases")
//        return queryPurchases()
//    }
//
//    override fun setPurchaseListener(listener: SubscriptionGateway.PurchaseListener) {
//        Log.d(TAG, "setPurchaseListener")
//        this.purchaseListener = listener
//    }
//
//    /**
//     * 处理 Google Play 推送的购买结果
//     * 主线程回调
//     */
//    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
//        Log.d(TAG, "handlePurchasesUpdated: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}, purchases=${purchases?.size ?: 0}")
//        when (billingResult.responseCode) {
//            BillingClient.BillingResponseCode.OK -> {
//                purchases?.forEach { purchase ->
//                    val subscription = toSubscriptionPurchase(purchase)
//                    Log.d(TAG, "  purchase: planId=${subscription.planId}, active=${subscription.isActive}, state=${purchase.purchaseState}, autoRenew=${purchase.isAutoRenewing}")
//                    mainHandler.post {
//                        purchaseListener?.onPurchaseUpdated(PurchaseResult.Success(subscription))
//                    }
//                }
//            }
//            BillingClient.BillingResponseCode.USER_CANCELED -> {
//                Log.d(TAG, "handlePurchasesUpdated: user canceled")
//                mainHandler.post { purchaseListener?.onPurchaseUpdated(PurchaseResult.Canceled) }
//            }
//            else -> {
//                Log.e(TAG, "handlePurchasesUpdated: ERROR code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
//                mainHandler.post {
//                    purchaseListener?.onPurchaseUpdated(
//                        PurchaseResult.Error(billingResult.responseCode, billingResult.debugMessage)
//                    )
//                }
//            }
//        }
//    }
//
//    private fun toSubscriptionPurchase(purchase: Purchase): SubscriptionPurchase {
//        val isActive = purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
//                purchase.isAutoRenewing || purchase.purchaseState == Purchase.PurchaseState.PENDING
//        return SubscriptionPurchase(
//            planId = purchase.products.firstOrNull() ?: "",
//            purchaseToken = purchase.purchaseToken,
//            source = SubscriptionSource.GOOGLE_PLAY,
//            isActive = isActive,
//            purchaseTimeMs = purchase.purchaseTime,
//            expiryTimeMs = null // 需要调用 Google Play Developer API 查询，客户端暂不支持
//        )
//    }
//}
