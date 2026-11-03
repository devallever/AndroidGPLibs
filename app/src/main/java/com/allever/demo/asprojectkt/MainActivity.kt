package com.allever.demo.asprojectkt

import app.allever.android.lib.basic.core.AbstractBindingActivity
import app.allever.android.lib.basic.core.toast
import com.allever.android.lib.admob.AdCallback
import com.allever.android.lib.admob.AdManager
import com.allever.demo.asprojectkt.databinding.ActivityMainBinding

class MainActivity : AbstractBindingActivity<ActivityMainBinding>() {

    override fun inflate(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun init() {
        adaptStatusBar(mBinding.funContainer)
        AdManager.loadBanner(mBinding.bannerContainer)
        mBinding.btnOpen.setOnClickListener {
            AdManager.showOpen(this) {
                toast("open finish")
            }
        }
        mBinding.btnInter.setOnClickListener {
            AdManager.showInter(this) {
                toast("inter finish")
            }
        }
        mBinding.btnReward.setOnClickListener {
            AdManager.loadRewardedAd(this, object : AdCallback {
                override fun onAdDismiss(rewarded: Boolean) {
                    super.onAdDismiss(rewarded)
                    toast("reward finish: $rewarded")
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        AdManager.resumeBanner(mBinding.bannerContainer)
        AdManager.loadNativeAd(mBinding.nativeContainer, "main")
    }

    override fun onPause() {
        super.onPause()
        AdManager.pauseBanner(mBinding.bannerContainer)
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.destroyBanner(mBinding.bannerContainer)
        AdManager.destroyNativeAd("main")
    }
}