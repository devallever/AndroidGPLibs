package app.allever.android.lib.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private const val TAG = "BillingHelper|Premium"

    private lateinit var context: Context

    private val mainHandler = Handler(Looper.getMainLooper())

    private var purchaseListener: PurchaseListener? = null

    private val purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            handlePurchasesUpdated(billingResult, purchases)
        }

    private val billingClient by lazy {
        BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
    }

    /** 是否已连接 */
    val isConnected: Boolean
        get() = billingClient.isReady

    // ==================== 生命周期 ====================

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    /** 连接 BillingService */
    fun connect(cb: (Boolean) -> Unit) {
        log("connect() called, isReady=${billingClient.isReady}")
        if (billingClient.isReady) {
            log("connect: already connected")
            cb(true)
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val success = result.responseCode == BillingClient.BillingResponseCode.OK
                log("onBillingSetupFinished: code=${result.responseCode}, msg=${result.debugMessage}, success=$success")
                cb(success)
            }

            override fun onBillingServiceDisconnected() {
                logW("onBillingServiceDisconnected: billing service disconnected, will auto-reconnect")
                // 断线自动重连
                billingClient.startConnection(this)
            }
        })
    }

    /** 断开连接 */
    fun disconnect() {
        log("disconnect()")
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
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun querySubDetails(skuList: List<String>): Result<List<ProductDetails>> =
        suspendCancellableCoroutine { cont ->
            log("querySubDetails: requesting SKUs=$skuList")
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
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetailsList = queryResult.productDetailsList
                    val unfetched = queryResult.unfetchedProductList
                    log("querySubDetails: OK, found ${productDetailsList.size} plans, unfetched=${unfetched.size}")
                    if (unfetched.isNotEmpty()) {
                        unfetched.forEach { logW("  unfetched SKU: ${it.productId}, statusCode=${it.statusCode}") }
                    }
                    cont.resume(Result.success(productDetailsList))
                } else {
                    logE("querySubDetails: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "querySubDetails: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    /** 查询内购商品详情（一次性购买） */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun queryInAppDetails(skuList: List<String>): Result<List<ProductDetails>> =
        suspendCancellableCoroutine { cont ->
            log("queryInAppDetails: requesting SKUs=$skuList")
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
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(Result.success(queryResult.productDetailsList))
                } else {
                    logE("queryInAppDetails: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "queryInAppDetails: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    // ==================== 发起购买 ====================

    /** 发起订阅购买 */
    fun launchSubscriptionPurchase(activity: Activity, productDetails: ProductDetails) {
        log("launchSubscriptionPurchase: productId=${productDetails.productId}")

        val offer = productDetails.subscriptionOfferDetails?.firstOrNull()
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
        log("launchBillingFlow returned: code=${result.responseCode}, msg=${result.debugMessage}")
    }

    /** 发起一次性购买 */
    fun launchInAppPurchase(activity: Activity, productDetails: ProductDetails) {
        log("launchInAppPurchase: productId=${productDetails.productId}")

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
        log("launchBillingFlow returned: code=${result.responseCode}, msg=${result.debugMessage}")
    }

    // ==================== 查询购买状态 ====================

    /** 查询已订阅的商品 */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun querySubscriptions(): Result<List<Purchase>> =
        suspendCancellableCoroutine { cont ->
            log("querySubscriptions: requesting active subscriptions")
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (cont.isCompleted) return@queryPurchasesAsync
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    log("querySubscriptions: OK, ${purchases.size} purchases")
                    cont.resume(Result.success(purchases))
                } else {
                    logE("querySubscriptions: FAILED code=${billingResult.responseCode}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "querySubscriptions error: code=${billingResult.responseCode}")
                        )
                    )
                }
            }
        }

    /** 查询已购买的一次性商品 */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun queryInAppPurchases(): Result<List<Purchase>> =
        suspendCancellableCoroutine { cont ->
            log("queryInAppPurchases: requesting in-app purchases")
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (cont.isCompleted) return@queryPurchasesAsync
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(Result.success(purchases))
                } else {
                    logE("queryInAppPurchases: FAILED code=${billingResult.responseCode}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "queryInAppPurchases error: code=${billingResult.responseCode}")
                        )
                    )
                }
            }
        }

    // ==================== 确认/消耗购买 ====================

    /** 确认订阅购买（订阅必须确认） */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            log("acknowledgePurchase: token=$purchaseToken")
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { billingResult ->
                if (cont.isCompleted) return@acknowledgePurchase
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    log("acknowledgePurchase: OK")
                    cont.resume(Result.success(Unit))
                } else {
                    logE("acknowledgePurchase: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "acknowledgePurchase error: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    /** 消耗一次性购买 */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun consumePurchase(purchaseToken: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            log("consumePurchase: token=$purchaseToken")
            val params = com.android.billingclient.api.ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            billingClient.consumeAsync(params) { billingResult, _ ->
                if (cont.isCompleted) return@consumeAsync
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    log("consumePurchase: OK")
                    cont.resume(Result.success(Unit))
                } else {
                    logE("consumePurchase: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "consumePurchase error: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    // ==================== 内部处理 ====================

    /**
     * 处理 Google Play 推送的购买结果
     * 主线程回调
     */
    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        log("handlePurchasesUpdated: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}, purchases=${purchases?.size ?: 0}")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    log("  purchase: product=${purchase.products.firstOrNull()}, state=${purchase.purchaseState}, autoRenew=${purchase.isAutoRenewing}")
                    mainHandler.post {
                        purchaseListener?.onPurchaseUpdated(PurchaseResult.Success(purchase))
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                log("handlePurchasesUpdated: user canceled")
                mainHandler.post { purchaseListener?.onPurchaseUpdated(PurchaseResult.Canceled) }
            }

            else -> {
                logE("handlePurchasesUpdated: ERROR code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                mainHandler.post {
                    purchaseListener?.onPurchaseUpdated(
                        PurchaseResult.Error(billingResult.responseCode, billingResult.debugMessage)
                    )
                }
            }
        }
    }

    // ==================== 日志工具 ====================

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }

    private fun logW(msg: String) {
        Log.w(TAG, msg)
    }

    private fun logE(msg: String) {
        Log.e(TAG, msg)
    }
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
