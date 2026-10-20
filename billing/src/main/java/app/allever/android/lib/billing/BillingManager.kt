package app.allever.android.lib.billing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 订阅业务
 */
object BillingManager {
    private val TAG = "BillingManager"



    /** SKU 列表常量 — 在 Google Play Console 创建这 4 个订阅 */
    const val SKU_PREMIUM_WEEKLY = "premium_weekly"
    const val SKU_PREMIUM_MONTHLY = "premium_monthly"
    const val SKU_PREMIUM_YEARLY = "premium_yearly"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val ALL_SKUS = listOf(
        SKU_PREMIUM_WEEKLY,
        SKU_PREMIUM_MONTHLY,
        SKU_PREMIUM_YEARLY,
    )


    fun init(context: Context) {
        BillingHelper.init(context)
        BillingHelper.connect {
            log("connect: $it")
            scope.launch {
                val result = BillingHelper.queryDetails(ALL_SKUS)
                if (result.isSuccess) {
                    result.getOrNull()?.let { skuDetails ->
                        skuDetails.forEach {
                            log("skuDetails: ${it.title} ${it.description} ${it.name}")
                        }
                    }
                } else {
                    log("skuDetails: ${result.exceptionOrNull()?.message}")
                }
            }
        }

    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }
}

/**
 * 带 Billing 错误码的异常 —— PremiumActivity 可据此区分 BILLING_UNAVAILABLE 等场景
 *
 * Google Play BillingResponseCode:
 *   3 = BILLING_UNAVAILABLE（设备不支持或未配置）
 *   4 = ITEM_UNAVAILABLE（SKU 未激活）
 *   5 = DEVELOPER_ERROR
 */
class BillingException(val responseCode: Int, msg: String) : Exception(msg)