package com.allever.demo.asprojectkt

import app.allever.android.lib.basic.UsageManager
import app.allever.android.lib.basic.core.AbstractBindingActivity
import app.allever.android.lib.basic.core.ActivityHelper
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.ui.BasicSettingActivity
import app.allever.android.lib.basic.ui.dialog.UsageLimitDialog
import app.allever.android.lib.billing.BillingActivity
import app.allever.android.lib.recommend.ui.RecommendListActivity
import com.allever.android.lib.ad.AdBusiness
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
            AdBusiness.showInter(this) {
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
        mBinding.btnSetting.setOnClickListener {
            ActivityHelper.startActivity<BasicSettingActivity>()
        }

        mBinding.btnFloatConsumeCount.setOnClickListener {
            // === 使用次数检测（Premium 用户跳过） ===
//            val isPremium = BillingManager.hasActiveSubscription()
            val isPremium = false
            if (!isPremium && !UsageManager.consume()) {
                // 次数耗尽，弹窗提示看广告
                UsageLimitDialog.show(this) { success ->
                    if (success && UsageManager.consume()) {
                        mBinding.leftCountView.updateLeftCount()
                        doFunction()
                    }
                    mBinding.leftCountView.updateLeftCount()
                }
                return@setOnClickListener
            }

            if (!isPremium) mBinding.leftCountView.updateLeftCount()
            doFunction()
        }


        mBinding.btnPremiumContainer.setOnClickListener {
            ActivityHelper.startActivity<BillingActivity>()
        }

        mBinding.btnRecommend.setOnClickListener {
            ActivityHelper.startActivity<RecommendListActivity>()
        }
    }

    private fun doFunction() {
        toast("执行成功，消耗1个次数")
    }


    override fun onResume() {
        super.onResume()
        AdManager.resumeBanner(mBinding.bannerContainer)
        AdBusiness.loadNativeAd(mBinding.nativeContainer, "main")
        mBinding.leftCountView.updateLeftCount()
    }

    override fun onPause() {
        super.onPause()
        AdManager.pauseBanner(mBinding.bannerContainer)
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.destroyBanner(mBinding.bannerContainer)
        AdBusiness.destroyNativeAd("main")
    }
}