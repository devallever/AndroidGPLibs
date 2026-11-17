package com.allever.demo.asprojectkt

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.UsageManager
import app.allever.android.lib.basic.core.AbstractBindingActivity
import app.allever.android.lib.basic.core.ActivityHelper
import app.allever.android.lib.basic.core.BannerLoader
import app.allever.android.lib.basic.core.NativeAutoRefresher
import app.allever.android.lib.basic.core.log
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.ui.BasicSettingActivity
import app.allever.android.lib.basic.ui.dialog.UsageLimitDialog
import app.allever.android.lib.basic.update.VersionHelper
import app.allever.android.lib.billing.BillingActivity
import app.allever.android.lib.recommend.ui.RecommendDialog
import app.allever.android.lib.recommend.ui.RecommendListActivity
import com.allever.android.lib.ad.AdBusiness
import com.allever.android.lib.admob.AdCallback
import com.allever.android.lib.admob.AdManager
import com.allever.demo.asprojectkt.databinding.ActivityMainBinding
import com.allever.demo.asprojectkt.BuildConfig

class MainActivity : AbstractBindingActivity<ActivityMainBinding>() {

    private val mRecommendDialog by lazy {
        RecommendDialog(this) {
            finish()
        }
    }

    override fun inflate(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ProjectConfig.isPrivateChannel()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        super.onCreate(savedInstanceState)
    }

    override fun init() {
        adaptStatusBar(mBinding.funContainer)

        NativeAutoRefresher.Builder(this, mBinding.nativeContainer)
            .page("main")
            .interval(5, 10)
            .start()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                log("execute handleOnBackPressed")
                handleBackPress()
            }
        })

        BannerLoader.Builder(this, mBinding.bannerContainer).start()
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

    override fun onBackPressed() {
        log("execute onBackPressed")
        handleBackPress()
    }

    private fun handleBackPress() {
        if (mRecommendDialog.isShowing) {
            mRecommendDialog.dismiss()
        } else  {
            mRecommendDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        mBinding.leftCountView.updateLeftCount()
        VersionHelper.checkAndShow(this)
    }
}