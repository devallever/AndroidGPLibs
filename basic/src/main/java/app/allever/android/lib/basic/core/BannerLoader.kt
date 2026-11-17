package app.allever.android.lib.basic.core

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.allever.android.lib.admob.AdManager

/**
 * Banner 广告加载与生命周期管理工具
 *
 * 内部自动注册生命周期观察者：
 * - ON_RESUME：首次加载 Banner（失败或未加载时重试），并调用 resumeBanner 恢复刷新
 * - ON_PAUSE：调用 pauseBanner 暂停刷新
 * - ON_DESTROY：调用 destroyBanner 销毁并清理容器
 *
 * 用法:
 * ```
 * BannerLoader.Builder(this, mBinding.bannerContainer).start()
 * ```
 */
class BannerLoader private constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val container: ViewGroup
) {

    /** 是否已经发起过加载，避免每次回到前台重复创建 AdView */
    private var hasLoaded = false

    private val observer = LifecycleEventObserver { owner, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                log("生命周期进入 ON_RESUME，加载并恢复 Banner 广告")
                loadIfNeeded()
                AdManager.resumeBanner(container)
            }
            Lifecycle.Event.ON_PAUSE -> {
                log("生命周期进入 ON_PAUSE，暂停 Banner 广告")
                AdManager.pauseBanner(container)
            }
            Lifecycle.Event.ON_DESTROY -> {
                log("生命周期进入 ON_DESTROY，销毁 Banner 广告并清理资源")
                destroy(owner)
            }
            else -> Unit
        }
    }

    private fun loadIfNeeded() {
        if (hasLoaded) {
            log("Banner 广告已加载，无需重复加载")
            return
        }
        hasLoaded = true
        log("开始加载 Banner 广告")
        AdManager.loadBanner(container)
    }

    private fun destroy(owner: LifecycleOwner) {
        runCatching { AdManager.destroyBanner(container) }
        hasLoaded = false
        owner.lifecycle.removeObserver(observer)
        log("BannerLoader 已完全清理")
    }

    fun start() {
        lifecycleOwner.lifecycle.addObserver(observer)
        log("BannerLoader 已注册生命周期观察者")
        // 如果当前已处于 RESUMED 状态，立即加载并恢复
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            log("当前已在前台，立即加载并恢复 Banner 广告")
            loadIfNeeded()
            AdManager.resumeBanner(container)
        } else {
            log("当前不在前台，等待下次 ON_RESUME 再加载")
        }
    }

    private fun stop() {
        log("手动停止 BannerLoader")
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    class Builder(private val lifecycleOwner: LifecycleOwner, private val container: ViewGroup) {

        fun start(): BannerLoader {
            return BannerLoader(lifecycleOwner, container).also { it.start() }
        }
    }
}
