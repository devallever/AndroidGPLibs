package app.allever.android.lib.basic.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.R
import app.allever.android.lib.basic.core.DisplayHelper
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.databinding.ActivityBasicSplashBinding
import app.allever.android.lib.basic.ui.dialog.PrivacyDialog
import com.allever.android.lib.admob.AdCallback
import com.allever.android.lib.admob.AdManager
import com.allever.android.lib.admob.GDPRHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

open class BasicSplashActivity : AppCompatActivity() {
    private val mBinding by lazy { ActivityBasicSplashBinding.inflate(layoutInflater) }

    private val jumpTask = Runnable {
        jumpMain()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mBinding.tvAppName.text = ProjectConfig.getAppName()
        mBinding.ivLogo.setImageResource(ProjectConfig.getAppIcon())

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }

        })

        if (!ProjectConfig.isHuawei()) {
            //不是华为，走正常流程
            normalWay()
        } else {
            if (ProjectConfig.isAgreePrivacy()) {
                //走正常流程
                normalWay()
            } else {
                //还没同意，隐私弹窗
                PrivacyDialog.show(
                    this,
                    onAgree = {
                        ProjectConfig.initSdk()
                        ProjectConfig.updateAgreePrivacy()
                        lifecycleScope.launch {
                            delay(1000)
                            normalWay()
                        }
                    },
                    onDisagree = {
                        toast(getString(R.string.vc_privacy_dialog_toast))
                        finish()
                    }
                )
            }
        }



        val duration = 1500L

        val translateAnimator2 = ObjectAnimator.ofFloat(
            mBinding.tvAppName,
            "translationY",
            mBinding.tvAppName.translationY + DisplayHelper.dip2px(25),
            mBinding.tvAppName.translationY
        )
        translateAnimator2.duration = duration
        translateAnimator2.start()

        val alphaAnimator = ObjectAnimator.ofFloat(
            mBinding.ivLogo,
            "alpha",
            0f,
            1f
        )
        alphaAnimator.duration = duration
        alphaAnimator.start()

        val alphaAnimato2 = ObjectAnimator.ofFloat(
            mBinding.tvAppName,
            "alpha",
            0f,
            1f
        )
        alphaAnimato2.duration = duration
        alphaAnimato2.start()
    }

    override fun onBackPressed() {
    }


    open fun jumpMain() {
        lifecycleScope.launch {
            val intent = Intent(this@BasicSplashActivity, ProjectConfig.getMainClz())
            startActivity(intent)
            finish()
        }
    }

    private var showAd = false

    private fun normalWay() {
        lifecycleScope.launch {
            GDPRHelper.requestConsentInfoUpdate(this@BasicSplashActivity) {
                AdManager.init(ProjectConfig.getAdConfig(), application, {
                    if (!isActive || isDestroyed || isFinishing || lifecycle.currentState != Lifecycle.State.RESUMED)return@init
                    if (AdManager.isInitSuccess()) {
                        if (!isActive) return@init
                        if (ProjectConfig.billingEnable() &&  hasActiveSubscription()){
                            showAd = true
                            jumpTask.run()
                            return@init
                        }

                        AdManager.loadOpen(this@BasicSplashActivity, object: AdCallback {
                            override fun onAdFailLoad() {
                                jumpTask.run()
                            }

                            override fun onAdDismiss(rewarded: Boolean) {
                                jumpTask.run()
                            }

                            override fun onAdShow() {
                                showAd = true
                            }
                        })
                    } else {
                        jumpTask.run()
                    }
                })
            }
            delay(20_000.milliseconds)
            if (showAd) {
                return@launch
            }
            jumpTask.run()
        }
    }

    private fun huaweiWay() {

    }

    open fun hasActiveSubscription(): Boolean {
        return false
    }
}