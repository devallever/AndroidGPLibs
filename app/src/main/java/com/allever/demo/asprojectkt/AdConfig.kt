package com.allever.demo.asprojectkt

import com.allever.android.lib.admob.IAdConfig

class AdConfig: IAdConfig {
    override fun bannerAdId(): String {
        return "ca-app-pub-3940256099942544/6300978100"
    }

    override fun interAdId(): String {
        return "ca-app-pub-3940256099942544/1033173700"
    }

    override fun rewardAdId(): String {
        return "ca-app-pub-3940256099942544/5224354900"
    }

    override fun nativeAdId(): String {
        return "ca-app-pub-3940256099942544/2247696100"
    }

    override fun openAdId(): String {
        return "ca-app-pub-3940256099942544/3419835200"
    }

    override fun getAdId(type: Int): String {
        return when(type) {
            IAdConfig.OPEN_AD -> openAdId()
            IAdConfig.BANNER_AD -> bannerAdId()
            IAdConfig.INTER_AD -> interAdId()
            IAdConfig.REWARD_AD -> rewardAdId()
            IAdConfig.NATIVE_AD -> nativeAdId()
            else -> ""
        }
    }
}