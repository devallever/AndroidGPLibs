package app.allever.android.lib.basic.config

import com.allever.android.lib.admob.IAdConfig

data class Config(
    val channel: String,
    val appName: String,
    val appIcon: Int,
    val versionName: String,
    val versionCode: Int,
    val mainClz: Class<*>,
    val adConfig: IAdConfig,
    val billingEnable: Boolean,
    val privacyUrl: String,
    /** 广告配置接口地址，各项目传入自己的接口，为空时只使用缓存和 assets 兜底 */
    val adConfigUrl: String = "",
    /** 版本更新接口地址，各项目传入自己的接口，为空时只使用缓存和 assets 兜底 */
    val versionCheckUrl: String = "",
    val hasActiveSubscriptionCache: () -> Boolean,
    val initBillingTask: () -> Unit
) {
}