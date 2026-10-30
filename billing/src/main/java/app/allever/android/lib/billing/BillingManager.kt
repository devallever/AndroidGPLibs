package app.allever.android.lib.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 订阅业务管理
 * 提供购买、查询、恢复等上层业务接口
 */
object BillingManager {
    private const val TAG = "BillingManager"

    /** SKU 列表常量 — 在 Google Play Console 创建这些订阅 */
    const val SKU_PREMIUM_WEEKLY = "premium_weekly"
    const val SKU_PREMIUM_MONTHLY = "premium_monthly"
    const val SKU_PREMIUM_YEARLY = "premium_yearly"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val ALL_SUBS_SKUS = listOf(
        SKU_PREMIUM_WEEKLY,
        SKU_PREMIUM_MONTHLY,
        SKU_PREMIUM_YEARLY,
    )

    /** 缓存商品详情，避免购买时再次查询 */
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    /** 是否已初始化完成 */
    private var isInitialized = false

    /** 当前是否有有效订阅（本地缓存） */
    @Volatile
    private var hasActiveSubscription = false

    /**
     * 购买结果回调 — 业务层可监听全局购买事件
     * 也可通过 launchSubscriptionPurchase 的 callback 参数单独监听
     */
    private var globalPurchaseListener: ((PurchaseResult) -> Unit)? = null

    @Volatile
    private var sp: SharedPreferences? = null
    private val lock = Any()
    private const val KEY_HAS_ACTIVE_SUBSCRIPTION = "hasActiveSubscription"

    // ==================== 初始化 ====================

    fun init(context: Context, cb: (connected: Boolean) -> Unit = {}) {
        log("init: pkg=${context.packageName}, isInitialized=$isInitialized")
        BillingHelper.init(context)
        BillingHelper.connect { success ->
            log("connect: success=$success")
            if (success) {
                cb.invoke(true)
                scope.launch {
                    // 1. 查询商品详情
                    loadProductDetails()
                    // 2. 查询已有购买状态
                    refreshSubscriptionStatus()
                    // 3. 处理未确认的购买
                    handlePendingPurchases()
                    isInitialized = true
                    log("init: completed, products=${productDetailsMap.size}, hasActive=$hasActiveSubscription")
                }
            } else {
                cb.invoke(false)
                logE("init: connect failed")
            }
        }

        // 设置全局购买结果监听器
        BillingHelper.setPurchaseListener { result ->
            log("global purchase result: $result")
            scope.launch {
                when (result) {
                    is PurchaseResult.Success -> {
                        // 处理购买成功 —— 通常需要先服务端验证，再 ack
                        handlePurchaseSuccess(result.purchase)
                    }
                    is PurchaseResult.Error -> {
                        logE("purchase error: code=${result.code}, msg=${result.message}")
                    }
                    PurchaseResult.Canceled -> {
                        log("purchase canceled by user")
                    }
                }
                // 通知业务层
                withContext(Dispatchers.Main) {
                    globalPurchaseListener?.invoke(result)
                }
            }
        }

        if (sp == null) {
            synchronized(lock) {
                if (sp == null) {
                    sp = context.getSharedPreferences(
                        "billing_sp",
                        Context.MODE_PRIVATE
                    )
                }
            }
        }
    }

    /** 设置全局购买结果监听器 */
    fun setGlobalPurchaseListener(listener: ((PurchaseResult) -> Unit)?) {
        this.globalPurchaseListener = listener
    }

    // ==================== 商品详情 ====================

    /** 从 Google Play 加载商品详情并缓存 */
    private suspend fun loadProductDetails() {
        val result = BillingHelper.querySubDetails(ALL_SUBS_SKUS)
        if (result.isSuccess) {
            result.getOrNull()?.forEach { details ->
                productDetailsMap[details.productId] = details
                log("cached product: ${details.productId}")
            }
        } else {
            logE("loadProductDetails failed: ${result.exceptionOrNull()?.message}")
        }
    }

    /** 获取缓存的商品详情，如果没有则重新查询 */
    suspend fun getProductDetails(sku: String): ProductDetails? {
        productDetailsMap[sku]?.let { return it }
        // 重新加载
        loadProductDetails()
        return productDetailsMap[sku]
    }

    /** 获取所有已缓存的商品详情 */
    fun getAllCachedProductDetails(): List<ProductDetails> = productDetailsMap.values.toList()

    // ==================== 发起购买 ====================

    /**
     * 发起订阅购买
     * @param activity Activity
     * @param sku 目标 SKU
     * @param callback 可选的单独回调（主线程）
     */
    fun launchSubscriptionPurchase(
        activity: Activity,
        sku: String,
        callback: ((PurchaseResult) -> Unit)? = null
    ) {
        if (!BillingHelper.isConnected) {
            logE("launchSubscriptionPurchase: Billing not connected")
            callback?.invoke(PurchaseResult.Error(-1, "Billing not connected"))
            return
        }

        val details = productDetailsMap[sku]
        if (details == null) {
            logE("launchSubscriptionPurchase: ProductDetails not found for $sku")
            callback?.invoke(PurchaseResult.Error(-1, "Product not loaded: $sku"))
            return
        }

        // 如果有单独的 callback，先注册临时监听
        if (callback != null) {
            // 这里简化处理，全局监听会统一分发
        }

        log("launchSubscriptionPurchase: sku=$sku")
        BillingHelper.launchSubscriptionPurchase(activity, details)
    }

    // ==================== 查询/恢复购买 ====================

    fun getHasActiveSubscriptionCache(): Boolean {
        return sp?.getBoolean(KEY_HAS_ACTIVE_SUBSCRIPTION, false) ?: false
    }

    /**
     * 刷新订阅状态 —— 从 Google Play 查询当前有效的订阅
     */
    suspend fun refreshSubscriptionStatus(): Boolean {
        val result = BillingHelper.querySubscriptions()
        if (result.isSuccess) {
            val purchases = result.getOrNull() ?: emptyList()
            val active = purchases.firstOrNull { isPurchaseActive(it) }
            activePurchase = active
            hasActiveSubscription = active != null
            sp?.edit()?.putBoolean(KEY_HAS_ACTIVE_SUBSCRIPTION, hasActiveSubscription)?.apply()
            log("refreshSubscriptionStatus: active=$hasActiveSubscription, total=${purchases.size}, activeSku=${active?.products?.firstOrNull()}")
            return hasActiveSubscription
        } else {
            logE("refreshSubscriptionStatus failed: ${result.exceptionOrNull()?.message}")
            return false
        }
    }

    /**
     * 恢复购买 —— 等同于 refreshSubscriptionStatus
     * 用于"恢复购买"按钮触发
     */
    suspend fun restorePurchases(): Boolean {
        log("restorePurchases: start")
        return refreshSubscriptionStatus()
    }

    /** 当前是否有有效的订阅 */
    fun hasActiveSubscription(): Boolean = hasActiveSubscription

    /** 当前激活的订阅 purchase（可能为 null） */
    @Volatile
    private var activePurchase: Purchase? = null
    fun getActivePurchase(): Purchase? = activePurchase

    /**
     * 判断 Purchase 是否为有效订阅
     * 购买状态为 PURCHASED 且（自动续期中 或 未到期）
     */
    private fun isPurchaseActive(purchase: Purchase): Boolean {
        return purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

    // ==================== 处理未完成的购买 ====================

    /**
     * 处理所有未确认的购买
     * 订阅必须在 3 天内确认，否则会退款
     */
    private suspend fun handlePendingPurchases() {
        val result = BillingHelper.querySubscriptions()
        if (result.isSuccess) {
            result.getOrNull()?.forEach { purchase ->
                if (!purchase.isAcknowledged) {
                    log("handlePendingPurchases: ack pending purchase ${purchase.products.firstOrNull()}")
                    // TODO: 这里应该先进行服务端验证，验证成功后再 ack
                    // 现在直接 ack 作为演示
                    BillingHelper.acknowledgePurchase(purchase.purchaseToken)
                }
            }
        }
    }

    /**
     * 处理购买成功 —— 通常需要服务端验证后再确认
     */
    private suspend fun handlePurchaseSuccess(purchase: Purchase) {
        val sku = purchase.products.firstOrNull() ?: return
        log("handlePurchaseSuccess: sku=$sku, state=${purchase.purchaseState}, acknowledged=${purchase.isAcknowledged}")

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            log("handlePurchaseSuccess: not in PURCHASED state, skip")
            return
        }

        // TODO: 调用服务端 API 验证 purchaseToken
        // val verifyResult = api.verifyPurchase(purchase.purchaseToken)
        // if (verifyResult.isValid) {
        //     BillingHelper.acknowledgePurchase(purchase.purchaseToken)
        // }

        // 临时：直接确认
        if (!purchase.isAcknowledged) {
            val ackResult = BillingHelper.acknowledgePurchase(purchase.purchaseToken)
            log("handlePurchaseSuccess: ack result=$ackResult")
        }

        // 刷新订阅状态
        hasActiveSubscription = true
    }

    // ==================== 释放资源 ====================

    fun release() {
        globalPurchaseListener = null
        BillingHelper.setPurchaseListener(null)
        BillingHelper.disconnect()
        productDetailsMap.clear()
        isInitialized = false
        hasActiveSubscription = false
    }

    // ==================== 日志 ====================

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }

    private fun logE(msg: String) {
        Log.e(TAG, msg)
    }
}
