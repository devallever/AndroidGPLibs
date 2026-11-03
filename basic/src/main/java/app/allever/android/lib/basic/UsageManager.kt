package app.allever.android.lib.basic

import app.allever.android.lib.basic.core.store.StoreCore
import java.util.Calendar
import java.util.TimeZone

/**
 * 剩余使用次数管理器
 *
 * 功能：
 * - 每天默认 [DEFAULT_DAILY_LIMIT] 次免费使用次数
 * - 每天 00:00 自动重置
 * - 支持通过观看广告增加额外次数
 * - 线程安全的 consume/add 操作
 *
 * 使用示例：
 * ```
 * // 检查是否还能使用
 * if (UsageManager.canUse()) {
 *     UsageManager.consume()
 *     startFakeCall()
 * } else {
 *     // 弹窗提示看广告
 *     UsageLimitDialog.show(activity)
 * }
 *
 * // 广告奖励后增加次数
 * UsageManager.addCount(3)
 * ```
 */
object UsageManager {

    /** 每天默认免费次数 */
    private const val DEFAULT_DAILY_LIMIT = 3

    private const val KEY_REMAINING_COUNT = "USAGE_REMAINING_COUNT"
    private const val KEY_DATE_STAMP = "USAGE_DATE_STAMP"

    private val lock = Any()

    /**
     * 获取剩余可用次数。
     * 自动检测日期变更，跨天后重置为每日上限。
     */
    fun getRemaining(): Int {
        synchronized(lock) {
            checkAndResetIfNewDay()
            return StoreCore.getInt(KEY_REMAINING_COUNT, DEFAULT_DAILY_LIMIT)
        }
    }

    /**
     * 是否还有可用次数。
     */
    fun canUse(): Boolean = getRemaining() > 0

    /**
     * 消耗 1 次使用次数。
     * @return true 消耗成功；false 剩余为 0 无法消耗
     */
    fun consume(): Boolean {
        synchronized(lock) {
            checkAndResetIfNewDay()
            val remaining = StoreCore.getInt(KEY_REMAINING_COUNT, DEFAULT_DAILY_LIMIT)
            if (remaining <= 0) return false
            StoreCore.putInt(KEY_REMAINING_COUNT, remaining - 1)
            return true
        }
    }

    /**
     * 增加额外次数（观看广告奖励）。
     * @param count 要增加的次数，必须 > 0
     */
    fun addCount(count: Int) {
        if (count <= 0) return
        synchronized(lock) {
            checkAndResetIfNewDay()
            val remaining = StoreCore.getInt(KEY_REMAINING_COUNT, DEFAULT_DAILY_LIMIT)
            StoreCore.putInt(KEY_REMAINING_COUNT, remaining + count)
        }
    }

    /**
     * 重置为每日默认次数。
     */
    fun reset() {
        synchronized(lock) {
            StoreCore.putInt(KEY_REMAINING_COUNT, DEFAULT_DAILY_LIMIT)
            StoreCore.putLong(KEY_DATE_STAMP, todayStamp())
        }
    }

    /**
     * 跨天检测：如果日期变了，重置剩余次数。
     */
    private fun checkAndResetIfNewDay() {
        val savedStamp = StoreCore.getLong(KEY_DATE_STAMP, 0L)
        val todayStamp = todayStamp()
        if (savedStamp != todayStamp) {
            StoreCore.putInt(KEY_REMAINING_COUNT, DEFAULT_DAILY_LIMIT)
            StoreCore.putLong(KEY_DATE_STAMP, todayStamp)
        }
    }

    /**
     * 获取今日的日期戳（yyyyMMdd）。
     */
    private fun todayStamp(): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        return cal.get(Calendar.YEAR) * 10000L +
                (cal.get(Calendar.MONTH) + 1) * 100L +
                cal.get(Calendar.DAY_OF_MONTH)
    }
}
