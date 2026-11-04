package com.allever.demo.asprojectkt

import android.annotation.SuppressLint
import app.allever.android.lib.basic.UsageManager
import app.allever.android.lib.basic.core.AbstractBindingActivity
import app.allever.android.lib.basic.core.ActivityHelper
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.ui.BasicSettingActivity
import app.allever.android.lib.basic.ui.dialog.UsageLimitDialog
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
        mBinding.btnSetting.setOnClickListener {
            ActivityHelper.startActivity<BasicSettingActivity>()
        }

        mBinding.btnFloatConsumeCount.setOnClickListener {
        }

        mBinding.tvLeftCount.setOnClickListener {
            UsageLimitDialog.show(
                this,
                title = getString(R.string.vc_usage_reward_title),
                message = getString(R.string.vc_usage_reward_message),
                watchAdText = getString(R.string.vc_usage_reward_watch_ad)
            ) { updateLeftCount() }
        }

    }

    /**
     * 将剩余使用次数同步到右下角圆形徽章 tvLeftCount。
     */
    @SuppressLint("SetTextI18n")
    private fun updateLeftCount() {
        mBinding.tvLeftCount.text = UsageManager.getRemaining().toString()
    }


    override fun onResume() {
        super.onResume()
        AdManager.resumeBanner(mBinding.bannerContainer)
        AdBusiness.loadNativeAd(mBinding.nativeContainer, "main")
        updateLeftCount()
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