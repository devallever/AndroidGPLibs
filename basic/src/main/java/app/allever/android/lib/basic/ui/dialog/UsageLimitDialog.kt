package app.allever.android.lib.basic.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import app.allever.android.lib.basic.R
import app.allever.android.lib.basic.UsageManager
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.databinding.DialogBasicUsageLimitBinding
import com.allever.android.lib.admob.AdCallback
import com.allever.android.lib.admob.AdManager
import kotlin.apply
import kotlin.let

/**
 * 使用次数耗尽弹窗 —— 封装了激励视频广告的加载与展示。
 *
 * 特性：
 * - 完全可配置：奖励次数、标题、消息、按钮文案
 * - 内部自动处理广告加载/展示/奖励回调
 * - 广告加载中按钮自动禁用，防止重复点击
 * - 广告加载失败时友好提示
 *
 * 使用示例：
 * ```
 * UsageLimitDialog.show(activity) { success ->
 *     if (success) {
 *         // 用户看完广告，有次数了，启动模拟来电
 *         startFakeCall()
 *     }
 * }
 *
 * // 自定义配置
 * UsageLimitDialog.show(
 *     activity,
 *     rewardCount = 5,
 *     onDismiss = { success -> ... }
 * )
 * ```
 */
class UsageLimitDialog private constructor(
    private val activity: Activity,
    private val rewardCount: Int,
    private val title: String?,
    private val message: String?,
    private val rewardChipText: String?,
    private val watchAdText: String?,
    private val onDismiss: ((success: Boolean) -> Unit)?
) {

    private val dialog: Dialog
    private val binding: DialogBasicUsageLimitBinding = DialogBasicUsageLimitBinding.inflate(LayoutInflater.from(activity))
    private var isAdLoading = false
    private var isDismissed = false

    init {
        dialog = Dialog(activity).apply {
            setContentView(binding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // 移除 Dialog decor 自带的默认阴影，只保留 CardView 的 elevation
            window?.decorView?.setBackgroundResource(0)
        }

        applyConfig()
        setupListeners()
    }

    private fun applyConfig() {
        title?.let { binding.tvDialogTitle.text = it }
        message?.let { binding.tvDialogMessage.text = it }
        rewardChipText?.let { binding.tvRewardCount.text = it }
        watchAdText?.let { binding.btnWatchAd.text = it }
    }

    private fun setupListeners() {
        binding.btnWatchAd.setOnClickListener {
            if (isAdLoading) return@setOnClickListener
            isAdLoading = true
            showLoading(true)
            loadAndShowRewardAd()
        }

        binding.btnCancel.setOnClickListener {
            dismiss(false)
        }

        dialog.setOnDismissListener {
            if (!isDismissed) {
                isDismissed = true
                onDismiss?.invoke(false)
            }
        }
    }

    /**
     * 加载并展示激励视频广告。
     * 广告看完后自动给用户增加次数并回调 success=true。
     */
    private fun loadAndShowRewardAd() {
        try {
            if (!AdManager.isInitSuccess()) {
                onAdFail()
                return
            }

            AdManager.loadRewardedAd(activity, object : AdCallback {
                override fun onAdShow() {
                    // 广告展示中
                }

                override fun onAdDismiss(rewarded: Boolean) {
                    if (rewarded) {
                        // 用户看完了广告，发放奖励
                        UsageManager.addCount(rewardCount)
                        toast(activity.getString(R.string.bc_usage_reward_success, rewardCount))
                        dismiss(true)
                    } else {
                        // 用户中途关闭，不发奖励
                        dismiss(false)
                    }
                }

                override fun onAdFailLoad() {
                    onAdFail()
                }
            })
        } catch (e: Throwable) {
            e.printStackTrace()
            onAdFail()
        }
    }

    private fun onAdFail() {
        isAdLoading = false
        showLoading(false)
        toast(activity.getString(R.string.bc_usage_ad_fail))
    }

    private fun showLoading(loading: Boolean) {
        binding.btnWatchAd.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun dismiss(success: Boolean) {
        if (isDismissed) return
        isDismissed = true
        onDismiss?.invoke(success)
        dialog.dismiss()
    }

    fun show() {
        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
            // Dialog 默认 wrap_content 会导致被内部 TextView 挤压，这里强制宽度为屏幕宽的 85%
            dialog.window?.let { window ->
                val dm = activity.resources.displayMetrics
                val width = (dm.widthPixels * 0.99f).toInt()
                val lp = window.attributes
                lp.width = width
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = lp
            }
        }
    }

    companion object {

        /**
         * 显示默认配置的弹窗。
         *
         * @param activity 当前 Activity
         * @param rewardCount 观看广告后奖励的次数，默认 3
         * @param onDismiss 弹窗关闭回调，success=true 表示用户看广告获得了次数
         */
        fun show(
            activity: Activity,
            rewardCount: Int = 3,
            onDismiss: ((success: Boolean) -> Unit)? = null
        ) {
            UsageLimitDialog(
                activity = activity,
                rewardCount = rewardCount,
                title = null,
                message = null,
                rewardChipText = null,
                watchAdText = null,
                onDismiss = onDismiss
            ).show()
        }

        /**
         * 显示完全自定义配置的弹窗。
         */
        fun show(
            activity: Activity,
            rewardCount: Int = 3,
            title: String? = null,
            message: String? = null,
            rewardChipText: String? = null,
            watchAdText: String? = null,
            onDismiss: ((success: Boolean) -> Unit)? = null
        ) {
            UsageLimitDialog(
                activity = activity,
                rewardCount = rewardCount,
                title = title,
                message = message,
                rewardChipText = rewardChipText,
                watchAdText = watchAdText,
                onDismiss = onDismiss
            ).show()
        }
    }
}
