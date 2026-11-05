package app.allever.android.lib.basic.ui.widget

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import app.allever.android.lib.basic.R
import app.allever.android.lib.basic.UsageManager
import app.allever.android.lib.basic.databinding.BasicLeftCountViewBinding
import app.allever.android.lib.basic.ui.dialog.UsageLimitDialog

class LeftCountView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private val mBinding by lazy {
        BasicLeftCountViewBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mBinding.tvLeftCount.setOnClickListener {
            UsageLimitDialog.show(
                context as Activity,
                title = context.getString(R.string.bc_usage_reward_title),
                message = context.getString(R.string.bc_usage_reward_message),
                watchAdText = context.getString(R.string.bc_usage_reward_watch_ad)
            ) { updateLeftCount() }
        }
    }

    /**
     * 将剩余使用次数同步到右下角圆形徽章 tvLeftCount。
     */
    @SuppressLint("SetTextI18n")
    fun updateLeftCount() {
        val count = UsageManager.getRemaining()
        mBinding.tvLeftCount.text = count.toString()
        if (count > 2) {
            stopLimitShakeAnim()
        } else {
            startLimitShakeAnim()
        }
    }

    private var mLimitShakeAnim: AnimatorSet? = null

    /**
     * 次数不足时循环抖动放大缩小动画
     */
    private fun startLimitShakeAnim() {
        if (mLimitShakeAnim?.isRunning == true) return
        val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 0.85f, 1.15f).apply {
            duration = 700
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0.85f, 1.15f).apply {
            duration = 700
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        mLimitShakeAnim = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    /**
     * 停止抖动动画
     */
    private fun stopLimitShakeAnim() {
        mLimitShakeAnim?.cancel()
        mLimitShakeAnim = null
        this.scaleX = 1f
        this.scaleY = 1f
    }
}