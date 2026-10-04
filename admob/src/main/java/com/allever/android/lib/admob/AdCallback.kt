package com.allever.android.lib.admob

interface AdCallback {
    fun onAdLoaded() {}
    fun onAdFailLoad() {}
    fun onAdShow() {}
    fun onAdDismiss(rewarded: Boolean) {}

    fun onAdClick() {}

}