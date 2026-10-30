package app.allever.android.lib.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 购买结果回调
 */
sealed class PurchaseResult {
    /** 购买成功 */
    data class Success(val purchase: Purchase) : PurchaseResult()

    /** 用户取消或购买失败 */
    data class Error(val code: Int, val message: String) : PurchaseResult()

    /** 用户主动取消购买弹窗 */
    data object Canceled : PurchaseResult()
}

/**
 * 购买结果监听器
 */
fun interface PurchaseListener {
    fun onPurchaseUpdated(result: PurchaseResult)
}

/**
 * 封装 Google Play Billing 最原始的功能
 * 参考：GooglePlaySubscriptionGateway
 */
@SuppressLint("StaticFieldLeak")
object BillingHelper {
    private const val TAG = "BillingHelper"

    private lateinit var context: Context

    private var purchaseListener: PurchaseListener? = null

    private val purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            handlePurchasesUpdated(billingResult, purchases)
        }

    private val billingClient: BillingClient by lazy {
        log("creating BillingClient instance")
        BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }

    /** 是否已连接 */
    val isConnected: Boolean
        get() = runCatching { billingClient.isReady }.getOrDefault(false)

    // ==================== 生命周期 ====================

    fun init(context: Context) {
        this.context = context.applicationContext
        log("init: context=${this.context.packageName}")
    }

    /** 连接 BillingService */
    fun connect(cb: (Boolean) -> Unit) {
        if (!this::context.isInitialized) {
            logE("connect: NOT initialized, call init() first!")
            cb(false)
            return
        }
        log("connect: isReady=${billingClient.isReady}")
        if (billingClient.isReady) {
            log("connect: already connected, skip")
            cb(true)
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val code = result.responseCode
                val success = code == BillingClient.BillingResponseCode.OK
                val responseName = responseCodeName(code)
                log("onBillingSetupFinished: code=$code($responseName), msg=${result.debugMessage}, success=$success")
                cb(success)
            }

            override fun onBillingServiceDisconnected() {
                logW("onBillingServiceDisconnected: billing service disconnected, will auto-reconnect")
                billingClient.startConnection(this)
            }
        })
    }

    /** 断开连接 */
    fun disconnect() {
        log("disconnect: isReady=${billingClient.isReady}")
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    /** 设置购买结果监听器 */
    fun setPurchaseListener(listener: PurchaseListener?) {
        log("setPurchaseListener: ${if (listener != null) "set" else "clear"}")
        this.purchaseListener = listener
    }

    // ==================== 查询商品 ====================

    /** 查询商品详情（订阅） */
    suspend fun querySubDetails(skuList: List<String>): Result<List<ProductDetails>> =
        suspendCancellableCoroutine { cont ->
            log("querySubDetails: requesting SKUs=$skuList")

            if (!billingClient.isReady) {
                logE("querySubDetails: BillingClient NOT connected")
                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
                return@suspendCancellableCoroutine
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    skuList.map { sku ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(sku)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    }
                ).build()

            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (cont.isCompleted) return@queryProductDetailsAsync
                val code = billingResult.responseCode
                val responseName = responseCodeName(code)
                if (code == BillingClient.BillingResponseCode.OK) {
                    val productDetailsList = queryResult.productDetailsList
                    val unfetched = queryResult.unfetchedProductList
                    log("querySubDetails: OK(${productDetailsList.size}/${skuList.size} fetched, ${unfetched.size} unfetched)")
                    productDetailsList.forEach { pd ->
                        log("  product: id=${pd.productId}, title=${pd.title}, price=${pd.extractPrice()}")
                    }
                    if (unfetched.isNotEmpty()) {
                        unfetched.forEach {
                            logW("  unfetched: id=${it.productId}, statusCode=${it.statusCode}")
                        }
                    }
                    cont.resume(Result.success(productDetailsList))
                } else {
                    logE("querySubDetails: FAILED code=$code($responseName), msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(code, "querySubDetails: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    /** 查询内购商品详情（一次性购买） */
    suspend fun queryInAppDetails(skuList: List<String>): Result<List<ProductDetails>> =
        suspendCancellableCoroutine { cont ->
            log("queryInAppDetails: requesting SKUs=$skuList")

            if (!billingClient.isReady) {
                logE("queryInAppDetails: BillingClient NOT connected")
                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
                return@suspendCancellableCoroutine
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    skuList.map { sku ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(sku)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    }
                ).build()

            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (cont.isCompleted) return@queryProductDetailsAsync
                val code = billingResult.responseCode
                val responseName = responseCodeName(code)
                if (code == BillingClient.BillingResponseCode.OK) {
                    val list = queryResult.productDetailsList
                    log("queryInAppDetails: OK, found ${list.size} items")
                    list.forEach { log("  product: id=${it.productId}, title=${it.title}") }
                    cont.resume(Result.success(list))
                } else {
                    logE("queryInAppDetails: FAILED code=$code($responseName), msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(code, "queryInAppDetails: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    // ==================== 发起购买 ====================

    /** 发起订阅购买 */
    fun launchSubscriptionPurchase(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            logE("launchSubscriptionPurchase: BillingClient NOT connected")
            return
        }
        val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
        log("launchSubscriptionPurchase: productId=${productDetails.productId}, title=${productDetails.title}, price=${productDetails.extractPrice()}, hasOffer=${offer != null}")

        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        offer?.offerToken?.let {
            paramsBuilder.setOfferToken(it)
            log("launchSubscriptionPurchase: using offerToken=$it")
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(paramsBuilder.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        log("launchBillingFlow returned: code=${result.responseCode}(${responseCodeName(result.responseCode)}), msg=${result.debugMessage}")
    }

    /** 发起一次性购买 */
    fun launchInAppPurchase(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            logE("launchInAppPurchase: BillingClient NOT connected")
            return
        }
        log("launchInAppPurchase: productId=${productDetails.productId}, title=${productDetails.title}")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        log("launchBillingFlow returned: code=${result.responseCode}(${responseCodeName(result.responseCode)}), msg=${result.debugMessage}")
    }

    // ==================== 查询购买状态 ====================

    /** 查询已订阅的商品 */
    suspend fun querySubscriptions(): Result<List<Purchase>> =
        suspendCancellableCoroutine { cont ->
            log("querySubscriptions: requesting active subscriptions")

            if (!billingClient.isReady) {
                logE("querySubscriptions: BillingClient NOT connected")
                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
                return@suspendCancellableCoroutine
            }

            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (cont.isCompleted) return@queryPurchasesAsync
                val code = billingResult.responseCode
                val responseName = responseCodeName(code)
                if (code == BillingClient.BillingResponseCode.OK) {
                    log("querySubscriptions: OK, ${purchases.size} purchases")
                    purchases.forEach { p ->
                        log("  purchase: sku=${p.products.firstOrNull()}, token=${p.purchaseToken.take(16)}..., state=${purchaseStateName(p.purchaseState)}, autoRenew=${p.isAutoRenewing}, acknowledged=${p.isAcknowledged}, time=${p.purchaseTime}")
                    }
                    cont.resume(Result.success(purchases))
                } else {
                    logE("querySubscriptions: FAILED code=$code($responseName), msg=${billingResult.debugMessage}")
                    cont.resume(Result.failure(BillingException(code, "querySubscriptions: ${billingResult.debugMessage}")))
                }
            }
        }

    /** 查询已购买的一次性商品 */
    suspend fun queryInAppPurchases(): Result<List<Purchase>> =
        suspendCancellableCoroutine { cont ->
            log("queryInAppPurchases: requesting in-app purchases")

            if (!billingClient.isReady) {
                logE("queryInAppPurchases: BillingClient NOT connected")
                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
                return@suspendCancellableCoroutine
            }

            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (cont.isCompleted) return@queryPurchasesAsync
                val code = billingResult.responseCode
                if (code == BillingClient.BillingResponseCode.OK) {
                    log("queryInAppPurchases: OK, ${purchases.size} items")
                    purchases.forEach { p ->
                        log("  purchase: sku=${p.products.firstOrNull()}, state=${purchaseStateName(p.purchaseState)}, acknowledged=${p.isAcknowledged}")
                    }
                    cont.resume(Result.success(purchases))
                } else {
                    logE("queryInAppPurchases: FAILED code=$code(${responseCodeName(code)})")
                    cont.resume(Result.failure(BillingException(code, "queryInAppPurchases: code=$code")))
                }
            }
        }

    // ==================== 确认/消耗购买 ====================

    /** 确认订阅购买（订阅必须确认） */
    suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            log("acknowledgePurchase: token=${purchaseToken.take(16)}...")

            if (!billingClient.isReady) {
                logE("acknowledgePurchase: BillingClient NOT connected")
                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
                return@suspendCancellableCoroutine
            }

            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { billingResult ->
                if (cont.isCompleted) return@acknowledgePurchase
                val code = billingResult.responseCode
                if (code == BillingClient.BillingResponseCode.OK) {
                    log("acknowledgePurchase: OK")
                    cont.resume(Result.success(Unit))
                } else {
                    logE("acknowledgePurchase: FAILED code=$code(${responseCodeName(code)}), msg=${billingResult.debugMessage}")
                    cont.resume(Result.failure(BillingException(code, "acknowledgePurchase: ${billingResult.debugMessage}")))
                }
            }
        }

    /** 消耗一次性购买 */
    suspend fun consumePurchase(purchaseToken: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            log("consumePurchase: token=${purchaseToken.take(16)}...")

            if (!billingClient.isReady) {
                logE("consumePurchase: BillingClient NOT connected")
                cont.resume(Result.failure(IllegalStateException("BillingClient not connected")))
                return@suspendCancellableCoroutine
            }

            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.consumeAsync(params) { billingResult, _ ->
                if (cont.isCompleted) return@consumeAsync
                val code = billingResult.responseCode
                if (code == BillingClient.BillingResponseCode.OK) {
                    log("consumePurchase: OK")
                    cont.resume(Result.success(Unit))
                } else {
                    logE("consumePurchase: FAILED code=$code(${responseCodeName(code)}), msg=${billingResult.debugMessage}")
                    cont.resume(Result.failure(BillingException(code, "consumePurchase: ${billingResult.debugMessage}")))
                }
            }
        }

    // ==================== 内部处理 ====================

    /**
     * 处理 Google Play 推送的购买结果
     * 主线程回调
     */
    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val code = billingResult.responseCode
        log("handlePurchasesUpdated: code=$code(${responseCodeName(code)}), msg=${billingResult.debugMessage}, purchases=${purchases?.size ?: 0}")
        when (code) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    log("  purchase: sku=${purchase.products.firstOrNull()}, token=${purchase.purchaseToken.take(16)}..., state=${purchaseStateName(purchase.purchaseState)}, autoRenew=${purchase.isAutoRenewing}, acknowledged=${purchase.isAcknowledged}")
                }
                purchases?.forEach { purchase ->
                    purchaseListener?.onPurchaseUpdated(PurchaseResult.Success(purchase))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                log("handlePurchasesUpdated: user canceled")
                purchaseListener?.onPurchaseUpdated(PurchaseResult.Canceled)
            }

            else -> {
                logE("handlePurchasesUpdated: ERROR code=$code(${responseCodeName(code)}), msg=${billingResult.debugMessage}")
                purchaseListener?.onPurchaseUpdated(PurchaseResult.Error(code, billingResult.debugMessage))
            }
        }
    }

    // ==================== 工具 ====================

    /** 提取订阅价格（兼容 Billing 9.x） */
    private fun ProductDetails.extractPrice(): String {
        return subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: oneTimePurchaseOfferDetails?.formattedPrice
            ?: ""
    }

    private fun responseCodeName(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.OK -> "OK"
        BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        BillingClient.BillingResponseCode.ERROR -> "ERROR"
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        else -> "UNKNOWN($code)"
    }

    private fun purchaseStateName(state: Int): String = when (state) {
        Purchase.PurchaseState.PURCHASED -> "PURCHASED"
        Purchase.PurchaseState.PENDING -> "PENDING"
        Purchase.PurchaseState.UNSPECIFIED_STATE -> "UNSPECIFIED"
        else -> "UNKNOWN($state)"
    }

    private fun log(msg: String) { Log.d(TAG, msg) }
    private fun logW(msg: String) { Log.w(TAG, msg) }
    private fun logE(msg: String) { Log.e(TAG, msg) }
}

/**
 * 带 Billing 错误码的异常
 *
 * Google Play BillingResponseCode:
 *   3 = BILLING_UNAVAILABLE（设备不支持或未配置）
 *   4 = ITEM_UNAVAILABLE（SKU 未激活）
 *   5 = DEVELOPER_ERROR
 */
class BillingException(val responseCode: Int, msg: String) : Exception(msg)
