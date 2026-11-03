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
    val initBillingTask: () -> Unit
) {
}