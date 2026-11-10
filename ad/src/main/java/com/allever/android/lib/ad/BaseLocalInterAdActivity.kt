package com.allever.android.lib.ad

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private var mCtaPulseAnimator: ObjectAnimator? = null

    /** 是否点击过跳转按钮，用于从商店/目标应用返回后自动关闭 */
    private var mCtaClicked = false

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
     * 为 TextView 设置水平渐变色文字，从左到右使用 vc_gradient_start → vc_gradient_end
     */
    protected fun setGradientText(textView: TextView) {
        val startColor = getColor(R.color.vc_gradient_start)
        val endColor = getColor(R.color.vc_gradient_end)
        textView.post {
            textView.paint.shader = LinearGradient(
                0f, 0f, textView.width.toFloat(), 0f,
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }
    }

    /**
     * 跳转按钮进入页面后持续循环放大缩小，吸引用户注意
     */
    protected fun startCtaPulseAnimation(view: View) {
        mCtaPulseAnimator?.cancel()
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f)
        ).apply {
            duration = 600L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        animator.start()
        mCtaPulseAnimator = animator
    }

    /**
     * CTA：已安装则打开应用，未安装则跳转应用商店
     * 跳转后不立即关闭，避免在商店/目标应用前台时提前触发 next()；
     * 标记已点击，用户返回广告页时再自动关闭
     */
    protected fun handleCtaClick(installed: Boolean) {
        mCtaClicked = true
        if (installed) {
            openApp(mRecommend.pkg)
        } else {
            RecommendUtils.jumpToAppStore(mRecommend.pkg)
        }
    }

    override fun onResume() {
        super.onResume()
        // 点击跳转后从商店/目标应用返回，自动关闭广告页
        if (mCtaClicked) {
            mCtaClicked = false
            finishWithAnimation()
        }
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
        mCtaPulseAnimator?.cancel()
        mCtaPulseAnimator = null
        super.onDestroy()
        if (isFinishing) {
            LocalInterAd.onAdActivityFinished()
            mDismissCallback?.invoke()
            mDismissCallback = null
        }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        log("适配导航栏：${this.javaClass.simpleName}")
        adaptNavigationBar(view)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        log("适配导航栏：${this.javaClass.simpleName}")
        val contentView = window.findViewById<ViewGroup>(android.R.id.content)
        val childView = contentView.getChildAt(0)
        if (childView != null) {
            adaptNavigationBar(childView)
        }
    }


    /**
     * 适配导航栏：确保内容不被导航栏覆盖
     */
    protected fun adaptNavigationBar(view: View?) {
        if (view == null) return
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            log("适配导航栏：导航栏高度 = ${navigationBars.bottom}")
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
            insets
        }
    }

    private fun log(msg: String) {
        Log.d("LocalInterAd", msg)
    }
}
