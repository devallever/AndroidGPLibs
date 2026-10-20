package app.allever.android.lib.billing

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 封装google内购最原始的功能
 */
@SuppressLint("StaticFieldLeak")
object BillingHelper {
    private val TAG = "BillingHelper|Premium"

    private lateinit var context: Context

    private val purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            log("purchasesUpdated: ${billingResult.responseCode}")
            purchases?.forEach {
                log("purchasesUpdated: purchaseState: ${it.purchaseState}")
                log("purchasesUpdated: ${it.orderId}: ${it.purchaseState} == ${Purchase.PurchaseState.PURCHASED}")
            }
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                log("purchases: ${purchases.size}")
            }
        }

    private val billingClient by lazy {
        BillingClient.newBuilder(context.applicationContext).setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            ).build()
    }

    //初始化
    fun init(context: Context) {
        this.context = context
    }

    //连接
    fun connect(cb: (Boolean) -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                log("onBillingSetupFinished: ${billingResult.responseCode}")
                cb(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                log("onBillingServiceDisconnected")
                cb(false)
            }
        })
    }

    //查询详情
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun queryDetails(skuList: List<String>): Result<List<ProductDetails>> =
        suspendCancellableCoroutine { cont ->
            Log.d(TAG, "queryPlans: requesting SKUs=${skuList}")
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
                    Log.d(TAG, "queryPlans: OK, found ${productDetailsList.size} plans, unfetched=${unfetched.size}")
                    if (unfetched.isNotEmpty()) {
                        unfetched.forEach { Log.w(TAG, "  unfetched SKU: ${it.productId}, statusCode=${it.statusCode}") }
                    }
                    Log.d(TAG, "queryPlans: parsed ${productDetailsList.size} plans: ${productDetailsList.forEach { details -> details.productId }}")
                    cont.resume(Result.success(productDetailsList))
                } else {
                    Log.e(TAG, "queryPlans: FAILED code=${billingResult.responseCode}, msg=${billingResult.debugMessage}")
                    cont.resume(
                        Result.failure(
                            BillingException(billingResult.responseCode, "queryDetails: ${billingResult.debugMessage}")
                        )
                    )
                }
            }
        }

    //购买

    //确认

    //查询状态


    /**
     * 处理 Google Play 推送的购买结果
     * 主线程回调
     */
    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(
            TAG,
            "handlePurchasesUpdated: code=${billingResult.responseCode}, msg=${billingResult.debugMessage}, purchases=${purchases?.size ?: 0}"
        )
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    log("handlePurchasesUpdated: purchase: $purchase")
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                log("handlePurchasesUpdated: user canceled")
            }

            else -> {
                logE(
                    "handlePurchasesUpdated: ERROR code=${billingResult.responseCode}, msg=${billingResult.debugMessage}"
                )
            }
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }

    private fun logE(msg: String) {
        Log.e(TAG, msg)
    }

}


//sealed class PurchaseResult {
//    /** 购买成功（可能已未处理，需要 ack） */
//    data class Success(val purchase: SubscriptionPurchase) : PurchaseResult()
//
//    /** 用户取消或购买失败 */
//    data class Error(val code: Int, val message: String) : PurchaseResult()
//
//    /** 用户主动取消购买弹窗 */
//    data object Canceled : PurchaseResult()
//}


///**
// * 统一购买记录模型
// */
//data class SubscriptionPurchase(
//    /** 对应 SubscriptionPlan.id，如 "premium_blue_monthly" */
//    val planId: String,
//    /** 购买 token：Google Play purchaseToken；Alipay tradeNo */
//    val purchaseToken: String,
//    /** 是否已激活 */
//    val isActive: Boolean,
//    /** 购买时间（毫秒） */
//    val purchaseTimeMs: Long,
//    /** 到期时间（毫秒），可空（自动续期尚未到期） */
//    val expiryTimeMs: Long? = null
//)
//
//fun interface PurchaseListener {
//    fun onPurchaseUpdated(result: PurchaseResult)
//}
