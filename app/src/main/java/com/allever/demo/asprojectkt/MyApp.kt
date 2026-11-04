package com.allever.demo.asprojectkt

import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.config.Config
import app.allever.android.lib.basic.core.App
import app.allever.android.lib.billing.BillingManager
import com.allever.android.lib.admob.AdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApp : App() {

    override fun init() {
        ProjectConfig.init(
            this, Config(
                "google",
                getString(R.string.app_name),
                R.mipmap.ic_launcher,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                MainActivity::class.java,
                AdConfig(),
                false,
                "https://baidu.com",
                { BillingManager.getHasActiveSubscriptionCache() },
            ) {
                BillingManager.init(context, "https://baidu.com") { connected ->
                    // 等待 BillingManager 异步刷新完订阅状态后，推送给 AdManager
                    if (connected) {
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            AdManager.updatePremiumStatus(BillingManager.hasActiveSubscription())
                        }
                    }
                }
            })
    }
}