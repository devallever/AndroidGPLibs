package com.allever.android.lib.ad

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import app.allever.android.lib.ad.R
import app.allever.android.lib.ad.databinding.ActivityLocalAdBinding
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.loadRound

/**
 * 本地插页样式一：图标 + 截图 + 长描述详情页
 */
class LocalAdDetailActivity : BaseLocalInterAdActivity() {

    private lateinit var mBinding: ActivityLocalAdBinding

    override fun onCreateContent() {
        mBinding = ActivityLocalAdBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        RecommendUtils.setMarginTop(mBinding.header, RecommendUtils.getStatusBarHeight(this))
        mBinding.ivClose.setOnClickListener { finishWithAnimation() }
        startCloseCountdown(mBinding.tvCountdown, mBinding.ivClose)

        mBinding.tvName.text = mRecommend.name
        mBinding.tvShortDesc.text = mRecommend.desc
        mBinding.tvLongDesc.text = mRecommend.longDesc?.takeIf { it.isNotBlank() } ?: mRecommend.desc
        loadIcon(mBinding.ivIcon, 18f)
        bindScreenshots(mRecommend.screenUrl ?: emptyList())

        val installed = RecommendUtils.isAppExist(this, mRecommend.pkg)
        mBinding.btnCta.text =
            getString(if (installed) R.string.local_ad_open else R.string.local_ad_get_on_play)
        mBinding.btnCta.setOnClickListener { handleCtaClick(installed) }
        startCtaPulseAnimation(mBinding.btnCta)
    }

    /**
     * 截图横向排列，无截图数据时隐藏整个区域
     */
    private fun bindScreenshots(screenUrl: List<String>) {
        mBinding.screenshotScroll.visibility = if (screenUrl.isEmpty()) View.GONE else View.VISIBLE
        if (screenUrl.isEmpty()) return

        val container = mBinding.screenshotContainer
        container.removeAllViews()
        // 竖屏截图：高度约屏高的 42%，宽度按 9:16 计算
        val imageHeight = (resources.displayMetrics.heightPixels * 0.42f).toInt()
        val imageWidth = (imageHeight * 9 / 16f).toInt()
        val margin = RecommendUtils.dip2px(5)

        screenUrl.forEachIndexed { index, url ->
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(imageWidth, imageHeight).apply {
                    this.marginEnd = if (index == screenUrl.size - 1) RecommendUtils.dip2px(20) else margin
                    this.marginStart = if (index == 0) RecommendUtils.dip2px(20) else margin
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_local_ad_screenshot)
                contentDescription = getString(R.string.local_ad_screenshot)
            }
            imageView.loadRound(url, 12f)
            container.addView(imageView)
        }
    }
}
