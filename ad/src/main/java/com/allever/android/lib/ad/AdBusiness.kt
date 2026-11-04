package com.allever.android.lib.ad

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import app.allever.android.lib.ad.R
import app.allever.android.lib.ad.databinding.AdLocalNativeBinding
import app.allever.android.lib.recommend.data.RecommendHelper
import app.allever.android.lib.recommend.data.RecommendId
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.loadRound
import com.allever.android.lib.admob.AdCallback
import com.allever.android.lib.admob.AdManager

/**
 * 广告业务
 * 与AdManager的区别
 * AdManager是广告的管理类，负责广告的加载、展示、统计等
 * AdBusiness是广告的业务类，负责广告的业务逻辑，比如加载sdk广告失败，显示本地广告等
 */
object AdBusiness {
    private val localNativeCache by lazy { mutableMapOf<String, ViewGroup>() }

    /**
     * 展示插页广告
     * 优先展示 AdMob 插页广告，不可用（无缓存/展示失败）时展示本地插页广告页
     * @param activity 当前 Activity
     * @param skipAd 是否跳过广告（业务层控制，如订阅用户免广告）
     * @param skipFreqCheck 是否跳过频率控制（默认 false，一般不需要）
     * @param next 广告关闭或跳过后的回调
     */
    fun showInter(
        activity: Activity,
        skipAd: Boolean = false,
        skipFreqCheck: Boolean = false,
        next: () -> Unit
    ) {
        AdManager.showInter(
            activity = activity,
            skipAd = skipAd,
            skipFreqCheck = skipFreqCheck,
            onFallback = {
                val recommend = RecommendHelper.getRandomRecommendApp()
                if (recommend == null) {
                    RecommendUtils.log("localInterAd: 推荐数据为空，直接结束")
                    next()
                } else {
                    LocalInterAdActivity.start(activity, recommend, next)
                }
            },
            next = next
        )
    }

    fun loadNativeAd(
        viewGroup: ViewGroup,
        page: String,
        adLayoutId: Int = R.layout.ad_native_small,
        skipIntervalCheck: Boolean = true
    ) {
        AdManager.loadNativeAd(
            viewGroup,
            page,
            adLayoutId,
            skipIntervalCheck,
            adCallback = object : AdCallback {
                override fun onAdFailLoad() {
                    //加载失败，显示本地推荐广告
                    val recommend = RecommendHelper.getRandomRecommendApp()
                    recommend.iconUrl
                    val binding = AdLocalNativeBinding.inflate(
                        LayoutInflater.from(viewGroup.context),
                        viewGroup,
                        true
                    )
                    binding.adIcon.loadRound(RecommendId.getIconRes(recommend.id), 12f)
                    binding.adHeadline.text = recommend.name
                    binding.adBody.text = recommend.desc
                    binding.root.setOnClickListener {
                        RecommendUtils.jumpToAppStore(recommend.pkg)
                    }
                    viewGroup.removeAllViews()
                    viewGroup.addView(binding.root)
                    localNativeCache[page] = viewGroup
                }

            })
    }

    fun destroyNativeAd(page: String) {
        AdManager.destroyNativeAd(page)
        localNativeCache.remove(page)
    }
}