package app.allever.android.lib.basic.core

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
 * ```kotlin
 * private val nativeRefresher = NativeAutoRefresher.Builder(this)
 *     .interval(30, 120)
 *     .onRefresh { AdBusiness.loadNativeAd(mBinding.nativeContainer, "main") }
 *     .onDestroy { AdBusiness.destroyNativeAd("main") }
 *     .start()
 * ```
 */
class NativeAutoRefresher private constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val minIntervalSec: Long,
    private val maxIntervalSec: Long,
    private val refreshTask: () -> Unit,
    private val destroyTask: (() -> Unit)?
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
            runCatching { refreshTask() }
            while (isActive) {
                val delaySec = Random.nextLong(minIntervalSec, maxIntervalSec + 1)
                log("下一次刷新将在 ${delaySec}s 后执行")
                delay(delaySec * 1000L)
                if (isActive) {
                    log("执行定时刷新")
                    runCatching { refreshTask() }
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
        runCatching { destroyTask?.invoke() }
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

    fun stop() {
        log("手动停止 NativeAutoRefresher")
        stopTimer()
        lifecycleOwner.lifecycle.removeObserver(observer)
    }

    class Builder(private val lifecycleOwner: LifecycleOwner) {

        private var minIntervalSec: Long = 30L
        private var maxIntervalSec: Long = 120L
        private var refreshTask: (() -> Unit)? = null
        private var destroyTask: (() -> Unit)? = null

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

        /**
         * 刷新任务（必传）
         */
        fun onRefresh(task: () -> Unit): Builder {
            this.refreshTask = task
            return this
        }

        /**
         * 销毁任务（可选，在 ON_DESTROY 时执行）
         */
        fun onDestroy(task: () -> Unit): Builder {
            this.destroyTask = task
            return this
        }

        fun start(): NativeAutoRefresher {
            val task = refreshTask
                ?: throw IllegalStateException("onRefresh must be set before start()")
            return NativeAutoRefresher(
                lifecycleOwner = lifecycleOwner,
                minIntervalSec = minIntervalSec,
                maxIntervalSec = maxIntervalSec,
                refreshTask = task,
                destroyTask = destroyTask
            ).also { it.start() }
        }
    }
}
