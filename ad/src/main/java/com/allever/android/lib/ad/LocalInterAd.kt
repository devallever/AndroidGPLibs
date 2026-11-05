package com.allever.android.lib.ad

import android.app.Activity
import android.content.Intent
import app.allever.android.lib.recommend.data.Recommend
import app.allever.android.lib.recommend.util.RecommendUtils

/**
 * 本地插页广告启动器
 *
 * 新增样式步骤：
 * 1. 新建 Activity 并继承 [BaseLocalInterAdActivity]；
 * 2. 在 AndroidManifest.xml 中注册该 Activity；
 * 3. 将 Activity 的 Class 添加到 [styleList]。
 */
object LocalInterAd {
    const val EXTRA_RECOMMEND = "extra_recommend"

    /** 可用的插页样式，新增后自动参与随机选择 */
    private val styleList = listOf(
        LocalAdDetailActivity::class.java,
        LocalAdPlayActivity::class.java
    )

    /** 当前是否已有本地插页在展示，避免重复启动 */
    private var mShowing = false

    /** 广告页关闭后需要执行的业务回调 */
    private var mPendingDismissCallback: (() -> Unit)? = null

    /**
     * 随机选择一种样式并启动本地插页广告页
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
        val style = styleList.random()
        try {
            activity.startActivity(Intent(activity, style).apply {
                putExtra(EXTRA_RECOMMEND, RecommendUtils.toJson(recommend))
            })
        } catch (e: Exception) {
            e.printStackTrace()
            mShowing = false
            mPendingDismissCallback = null
            onDismiss()
        }
    }

    internal fun pendingDismissCallback(): (() -> Unit)? = mPendingDismissCallback

    internal fun onAdActivityFinished() {
        mShowing = false
        mPendingDismissCallback = null
    }
}
