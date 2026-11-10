package com.allever.android.lib.ad

import android.view.View
import app.allever.android.lib.ad.R
import app.allever.android.lib.ad.databinding.ActivityLocalAdPlayBinding
import app.allever.android.lib.recommend.util.RecommendUtils

/**
 * 本地插页样式二：Google Play 风格，图标 + 名称 + 描述 + 评分/下载量 + Install
 */
class LocalAdPlayActivity : BaseLocalInterAdActivity() {

    private lateinit var mBinding: ActivityLocalAdPlayBinding

    override fun onCreateContent() {
        mBinding = ActivityLocalAdPlayBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        RecommendUtils.setMarginTop(
            mBinding.tvAdLabel,
            RecommendUtils.getStatusBarHeight(this) + RecommendUtils.dip2px(16)
        )
        RecommendUtils.setMarginTop(
            mBinding.tvCountdown,
            RecommendUtils.getStatusBarHeight(this) + RecommendUtils.dip2px(16)
        )
        mBinding.tvClose.setOnClickListener { finishWithAnimation() }
        startCloseCountdown(mBinding.tvCountdown, mBinding.tvClose)

        mBinding.tvName.text = mRecommend.name
        mBinding.tvShortDesc.text = mRecommend.desc
        loadIcon(mBinding.ivIcon, 32f)
        bindStats()
        setGradientText(mBinding.tvRecommendTitle)

        val installed = RecommendUtils.isAppExist(this, mRecommend.pkg)
        mBinding.btnInstall.text =
            getString(if (installed) R.string.local_ad_open else R.string.local_ad_install)
        mBinding.btnInstall.setOnClickListener { handleCtaClick(installed) }
        startCtaPulseAnimation(mBinding.btnInstall)
    }

    /**
     * 评分/下载量数据缺失时隐藏对应列，全部缺失时隐藏整行
     */
    private fun bindStats() {
        val rating = mRecommend.rating?.takeIf { it.isNotBlank() }
        val downloads = mRecommend.downloads?.takeIf { it.isNotBlank() }

        mBinding.tvRating.text = rating
        mBinding.tvDownloads.text = downloads
        mBinding.ratingColumn.visibility = if (rating != null) View.VISIBLE else View.GONE
        mBinding.downloadsColumn.visibility = if (downloads != null) View.VISIBLE else View.GONE
        mBinding.dividerOne.visibility =
            if (rating != null && downloads != null) View.VISIBLE else View.GONE
        mBinding.dividerTwo.visibility =
            if (rating != null || downloads != null) View.VISIBLE else View.GONE
        mBinding.statsRow.visibility =
            if (rating != null || downloads != null) View.VISIBLE else View.GONE
    }
}
