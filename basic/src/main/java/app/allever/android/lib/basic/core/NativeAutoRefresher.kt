package app.allever.android.lib.basic.core

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.allever.android.lib.ad.R
import app.allever.android.lib.basic.ProjectConfig
import com.allever.android.lib.ad.AdBusiness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.runCatching

/**
 * 原生广告定时自动刷新工具（协程版）
 *
 * 用法:
 * ```
 */
class NativeAutoRefresher private constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val page: String,
    private val nativeLayout: Int,
    private val container: ViewGroup,
    private val skipIntervalCheck: Boolean,
    private val minIntervalSec: Long,
    private val maxIntervalSec: Long
) {

    private var job: Job? = null

    private val observer = LifecycleEventObserver { owner, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                log("生命周期进入 ON_RESUME，启动定时刷新")
                startTimer()
            }
            Lifecycle.Event.ON_PAUSE -> {
                log("生命周期进入 ON_PAUSE，停止定时刷新")
                stopTimer()
            }
            Lifecycle.Event.ON_DESTROY -> {
                log("生命周期进入 ON_DESTROY，销毁原生广告并清理资源")
                destroy(owner)
            }
            else -> Unit
        }
    }

    private fun startTimer() {
        stopTimer()
        log("原生广告定时刷新启动，间隔区间：${minIntervalSec}s ~ ${maxIntervalSec}s")
        job = lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            log("立即执行首次刷新")
            runCatching { AdBusiness.loadNativeAd(container, page, nativeLayout, skipIntervalCheck) }
            while (isActive) {
                val delaySec = Random.nextLong(minIntervalSec, maxIntervalSec + 1)
                log("下一次刷新将在 ${delaySec}s 后执行")
                delay(delaySec * 1000L)
                if (isActive) {
                    log("执行定时刷新")
                    runCatching { AdBusiness.loadNativeAd(container, page, nativeLayout, skipIntervalCheck) }
                }
            }
            log("定时刷新协程已退出")
        }
    }

    private fun stopTimer() {
        job?.cancel()
        job = null
        log("定时刷新已停止")
    }

    private fun destroy(owner: LifecycleOwner) {
        log("执行原生广告销毁任务")
        runCatching { AdBusiness.destroyNativeAd(page) }
        stopTimer()
        owner.lifecycle.removeObserver(observer)
        log("NativeAutoRefresher 已完全清理")
    }

    fun start() {
        lifecycleOwner.lifecycle.addObserver(observer)
        log("NativeAutoRefresher 已注册生命周期观察者")
        // 如果当前已处于 RESUMED 状态，立即启动定时
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            log("当前已在前台，立即启动定时刷新")
            startTimer()
        } else {
            log("当前不在前台，等待下次 ON_RESUME 再启动")
        }
    }

    private fun stop() {
        log("手动停止 NativeAutoRefresher")
        stopTimer()
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    class Builder(private val lifecycleOwner: LifecycleOwner, private val container: ViewGroup) {
        private var page: String = "page:${this.hashCode()}"
        private var nativeLayout = R.layout.ad_native_banner_normal

        private var skipIntervalCheck = false

        private var minIntervalSec: Long = if (ProjectConfig.isPrivateChannel()) 90L else 40L
        private var maxIntervalSec: Long = if (ProjectConfig.isPrivateChannel()) 120L else 60L

        fun page(page: String): Builder {
            this.page = page
            return this
        }

        fun nativeLayout(nativeLayout: Int): Builder {
            this.nativeLayout = nativeLayout
            return this
        }

        fun skipIntervalCheck(skipIntervalCheck: Boolean): Builder {
            this.skipIntervalCheck = skipIntervalCheck
            return this
        }

        /**
         * 设置刷新随机间隔（秒），min 必须 <= max
         */
        fun interval(minSec: Long, maxSec: Long): Builder {
            require(minSec > 0) { "minSec must be > 0" }
            require(maxSec >= minSec) { "maxSec must be >= minSec" }
            this.minIntervalSec = minSec
            this.maxIntervalSec = maxSec
            return this
        }

        fun start(): NativeAutoRefresher {
            return NativeAutoRefresher(
                lifecycleOwner = lifecycleOwner,
                page = page,
                nativeLayout = nativeLayout,
                container,
                skipIntervalCheck = skipIntervalCheck,
                minIntervalSec = minIntervalSec,
                maxIntervalSec = maxIntervalSec
            ).also { it.start() }
        }
    }
}
