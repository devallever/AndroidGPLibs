package com.allever.android.lib.ad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import app.allever.android.lib.ad.R
import app.allever.android.lib.ad.databinding.ActivityLocalAdBinding
import app.allever.android.lib.recommend.data.Recommend
import app.allever.android.lib.recommend.data.RecommendId
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.StatusBarCompat
import app.allever.android.lib.recommend.util.loadRound

/**
 * 本地插页广告页
 * 当 AdMob 插页广告不可用或展示失败时，使用随机推荐数据填充此页面
 */
class LocalInterAdActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_RECOMMEND = "extra_recommend"

        /** 当前是否已有本地插页在展示，避免重复启动 */
        private var mShowing = false

        /** 广告页关闭后需要执行的业务回调 */
        private var mPendingDismissCallback: (() -> Unit)? = null

        /**
         * 启动本地插页广告页
         *
         * @param activity 当前 Activity
         * @param recommend 推荐数据
         * @param onDismiss 广告页关闭或点击跳转后的回调
         */
        fun start(activity: Activity, recommend: Recommend, onDismiss: () -> Unit = {}) {
            if (activity.isFinishing || activity.isDestroyed || mShowing) {
                onDismiss()
                return
            }
            mShowing = true
            mPendingDismissCallback = onDismiss
            try {
                activity.startActivity(Intent(activity, LocalInterAdActivity::class.java).apply {
                    putExtra(EXTRA_RECOMMEND, RecommendUtils.toJson(recommend))
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mShowing = false
                mPendingDismissCallback = null
                onDismiss()
            }
        }
    }

    private lateinit var mBinding: ActivityLocalAdBinding
    private lateinit var mRecommend: Recommend
    private var mDismissCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityLocalAdBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mDismissCallback = mPendingDismissCallback

        val recommend = RecommendUtils.fromJson(
            intent.getStringExtra(EXTRA_RECOMMEND) ?: "",
            Recommend::class.java
        )
        if (recommend == null) {
            RecommendUtils.log("localInterAd: 推荐数据解析失败，直接关闭")
            finish()
            return
        }
        mRecommend = recommend

        // 状态栏透明并适配内容顶部间距
        StatusBarCompat.translucentStatusBar(this, true)
        StatusBarCompat.changeToLightStatusBar(this)
        RecommendUtils.setMarginTop(
            mBinding.header,
            RecommendUtils.getStatusBarHeight(this)
        )

        initView()
        bindRecommend()
        RecommendUtils.log("localInterAd show, pkg=${recommend.pkg}")
    }

    private fun initView() {
        mBinding.ivClose.setOnClickListener {
            finish()
        }
    }

    private fun bindRecommend() {
        val recommend = mRecommend
        mBinding.tvName.text = recommend.name
        mBinding.tvShortDesc.text = recommend.desc
        mBinding.tvLongDesc.text = recommend.longDesc?.takeIf { it.isNotBlank() } ?: recommend.desc

        // logo 优先使用网络图标，失败或无地址时使用本地兜底图标
        val icon: Any = recommend.iconUrl.takeIf { it.isNotBlank() }
            ?: RecommendId.getIconRes(recommend.id)
        mBinding.ivIcon.loadRound(icon, 18f)

        bindScreenshots(recommend.screenUrl ?: emptyList())

        // CTA：已安装则打开应用，未安装则跳转应用商店
        val installed = RecommendUtils.isAppExist(this, recommend.pkg)
        mBinding.btnCta.text =
            getString(if (installed) R.string.local_ad_open else R.string.local_ad_get_on_play)
        mBinding.btnCta.setOnClickListener {
            if (installed) {
                openApp(recommend.pkg)
            } else {
                RecommendUtils.jumpToAppStore(recommend.pkg)
            }
            finish()
        }
    }

    /**
     * 截图横向排列，无截图数据时隐藏整个区域
     */
    private fun bindScreenshots(screenUrl: List<String>) {
        mBinding.screenshotScroll.visibility = if (screenUrl.isEmpty()) View.GONE else View.VISIBLE
        if (screenUrl.isEmpty()) return

        val container = mBinding.screenshotContainer
        container.removeAllViews()
        // 单张截图宽度约屏宽的 78%，高度按 16:9 计算
        val imageWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        val imageHeight = (imageWidth * 9 / 16f).toInt()
        val marginEnd = RecommendUtils.dip2px(10)

        screenUrl.forEach { url ->
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(imageWidth, imageHeight).apply {
                    this.marginEnd = marginEnd
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_local_ad_screenshot)
                contentDescription = getString(R.string.local_ad_screenshot)
            }
            imageView.loadRound(url, 12f)
            container.addView(imageView)
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
        super.onDestroy()
        if (isFinishing) {
            mShowing = false
            mPendingDismissCallback = null
            mDismissCallback?.invoke()
            mDismissCallback = null
        }
    }
}
