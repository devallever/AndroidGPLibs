package app.allever.android.lib.basic

import android.annotation.SuppressLint
import android.content.Context
import app.allever.android.lib.basic.config.Config
import app.allever.android.lib.basic.core.App
import app.allever.android.lib.basic.core.CoilLoader
import app.allever.android.lib.basic.core.store.StoreCore
import app.allever.android.lib.recommend.data.RecommendHelper
import com.allever.android.lib.admob.AdManager
import com.allever.android.lib.admob.GDPRHelper
import com.allever.android.lib.admob.IAdConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object ProjectConfig {
    lateinit var context: Context
    private lateinit var config: Config
    fun init(context: Context, config: Config, executeInit: Boolean = true) {
        this.context = context
        this.config = config

        if (executeInit) {
            CoilLoader.init(context)

            if (!isHuawei()) {
                initSdk()
            } else {
                if (isAgreePrivacy()) {
                    initSdk()
                }
            }
        }
    }

    fun initSdk() {
        GDPRHelper.init(context)
        if (GDPRHelper.canRequestAds()) {
            AdManager.init(getAdConfig(), App.app)
        } else {
            AdManager.init(App.app)
        }
        RecommendHelper.init(context)

        getInitBillingTask().invoke()
    }

    fun isAgreePrivacy(): Boolean {
        return StoreCore.getBoolean("agree_privacy", false)
    }

    fun updateAgreePrivacy() {
        StoreCore.putBoolean("agree_privacy", true)
    }

    fun getConfig(): Config {
        return config
    }

    fun getAppName(): String {
        return config.appName
    }

    fun getAppIcon(): Int {
        return config.appIcon
    }

    //versionName
    fun getVersionName(): String {
        return config.versionName
    }

    fun getVersionCode(): Int {
        return config.versionCode
    }

    fun getMainClz(): Class<*> {
        return config.mainClz
    }

    fun getChannel(): String {
        return config.channel
    }

    fun isHuawei(): Boolean {
        return config.channel == "huawei"
    }

    fun getAdConfig(): IAdConfig {
        return config.adConfig
    }

    fun billingEnable(): Boolean {
        return config.billingEnable
    }

    fun getPrivacyUrl(): String {
        return config.privacyUrl
    }

    fun getInitBillingTask(): () -> Unit {
        return config.initBillingTask
    }

    fun hasActiveSubscription(): Boolean {
        return config.hasActiveSubscriptionCache.invoke()
    }
}