package com.allever.android.lib.admob


/**
 *@Description
 *@author: zq
 *@date: 2023/10/30
 */
interface IAdConfig {

    companion object {
        const val OPEN_AD = 0
        const val BANNER_AD = 1
        const val INTER_AD = 2
        const val NATIVE_AD = 3
        const val REWARD_AD = 4
    }

    fun bannerAdId(): String
    fun interAdId(): String
    fun rewardAdId(): String
    fun nativeAdId(): String

    fun openAdId(): String

    /***
     * * //测试广告id
     * 开屏广告	ca-app-pub-3940256099942544/9257395921
     * 自适应横幅广告	ca-app-pub-3940256099942544/9214589741
     * 横幅广告	ca-app-pub-3940256099942544/6300978111
     * 插页式广告	ca-app-pub-3940256099942544/1033173712
     * 插页式视频广告	ca-app-pub-3940256099942544/8691691433
     * 激励广告	ca-app-pub-3940256099942544/5224354917
     * 插页式激励广告	ca-app-pub-3940256099942544/5354046379
     * 原生高级广告	ca-app-pub-3940256099942544/2247696110
     * 原生高级视频广告	ca-app-pub-3940256099942544/1044960115
     */
    fun getAdId(type: Int): String {
        return if (BuildConfig.DEBUG) {
            when (type) {
                OPEN_AD -> "ca-app-pub-3940256099942544/9257395921"
                BANNER_AD -> "ca-app-pub-3940256099942544/6300978111"
                INTER_AD -> "ca-app-pub-3940256099942544/1033173712"
                NATIVE_AD -> "ca-app-pub-3940256099942544/2247696110"
                REWARD_AD -> "ca-app-pub-3940256099942544/5224354917"
                else -> ""
            }
        } else {
            when (type) {
                OPEN_AD -> openAdId()
                BANNER_AD -> bannerAdId()
                INTER_AD -> interAdId()
                NATIVE_AD -> nativeAdId()
                REWARD_AD -> rewardAdId()
                else -> ""
            }
        }
    }
}