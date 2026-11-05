package com.allever.android.lib.ad

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import app.allever.android.lib.ad.R
import app.allever.android.lib.recommend.data.Recommend
import app.allever.android.lib.recommend.data.RecommendId
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.StatusBarCompat
import app.allever.android.lib.recommend.util.loadRound

/**
 * 本地插页广告基类
 * 负责通用的：数据解析、状态栏适配、禁用返回键、进出场动画、关闭回调、CTA 跳转
 * 子类只需实现 [onCreateContent] 完成各自的样式渲染
 */
abstract class BaseLocalInterAdActivity : AppCompatActivity() {

    protected lateinit var mRecommend: Recommend
    private var mDismissCallback: (() -> Unit)? = null
    private var mCountDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTransition()

        mDismissCallback = LocalInterAd.pendingDismissCallback()

        val recommend = RecommendUtils.fromJson(
            intent.getStringExtra(LocalInterAd.EXTRA_RECOMMEND) ?: "",
            Recommend::class.java
        )
        if (recommend == null) {
            RecommendUtils.log("localInterAd: 推荐数据解析失败，直接关闭")
            finish()
            return
        }
        mRecommend = recommend

        // 状态栏透明并适配深色图标
        StatusBarCompat.translucentStatusBar(this, true)
        StatusBarCompat.changeToLightStatusBar(this)

        // 禁用返回键退出，只能通过页面上的关闭按钮关闭
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        onCreateContent()
        RecommendUtils.log(
            "localInterAd show, style=${javaClass.simpleName}, pkg=${recommend.pkg}"
        )
    }

    /**
     * 子类在此 inflate 自己的布局并绑定数据
     */
    protected abstract fun onCreateContent()

    /**
     * 显示关闭倒计时，倒计时结束后隐藏倒计时并显示关闭按钮
     *
     * @param countdownView 倒计时文案
     * @param closeView 关闭按钮（倒计时期间隐藏）
     * @param seconds 倒计时秒数
     */
    protected fun startCloseCountdown(
        countdownView: TextView,
        closeView: View,
        seconds: Int = 5
    ) {
        closeView.visibility = View.INVISIBLE
        countdownView.visibility = View.VISIBLE
        countdownView.text = getString(R.string.local_ad_countdown, seconds)

        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remain = ((millisUntilFinished + 999L) / 1000L)
                    .coerceIn(1L, seconds.toLong())
                countdownView.text = getString(R.string.local_ad_countdown, remain)
            }

            override fun onFinish() {
                countdownView.visibility = View.GONE
                closeView.visibility = View.VISIBLE
            }
        }.start()
    }

    /**
     * 子类布局中的关闭按钮统一调用此方法
     */
    protected fun finishWithAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.ad_hold,
                R.anim.ad_slide_out_bottom
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.ad_hold, R.anim.ad_slide_out_bottom)
        }
        finish()
    }

    /**
     * logo 优先使用网络图标，失败或无地址时使用本地兜底图标
     */
    protected fun loadIcon(imageView: ImageView, radius: Float) {
        val icon: Any = mRecommend.iconUrl.takeIf { it.isNotBlank() }
            ?: RecommendId.getIconRes(mRecommend.id)
        imageView.loadRound(icon, radius)
    }

    /**
     * CTA：已安装则打开应用，未安装则跳转应用商店，随后关闭广告页
     */
    protected fun handleCtaClick(installed: Boolean) {
        if (installed) {
            openApp(mRecommend.pkg)
        } else {
            RecommendUtils.jumpToAppStore(mRecommend.pkg)
        }
        finishWithAnimation()
    }

    private fun setupTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.ad_slide_in_bottom,
                R.anim.ad_hold
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.ad_slide_in_bottom, R.anim.ad_hold)
        }
    }

    private fun openApp(pkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            RecommendUtils.jumpToAppStore(pkg)
            return
        }
        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            RecommendUtils.jumpToAppStore(pkg)
        }
    }

    override fun onDestroy() {
        mCountDownTimer?.cancel()
        mCountDownTimer = null
        super.onDestroy()
        if (isFinishing) {
            LocalInterAd.onAdActivityFinished()
            mDismissCallback?.invoke()
            mDismissCallback = null
        }
    }
}
