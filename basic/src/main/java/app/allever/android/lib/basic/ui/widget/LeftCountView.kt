package app.allever.android.lib.basic.ui.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import app.allever.android.lib.basic.R
import app.allever.android.lib.basic.UsageManager
import app.allever.android.lib.basic.databinding.BcLeftCountViewBinding
import app.allever.android.lib.basic.ui.dialog.UsageLimitDialog

class LeftCountView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private val mBinding by lazy {
        BcLeftCountViewBinding.inflate(
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
                title = context.getString(R.string.vc_usage_reward_title),
                message = context.getString(R.string.vc_usage_reward_message),
                watchAdText = context.getString(R.string.vc_usage_reward_watch_ad)
            ) { updateLeftCount() }
        }
    }

    /**
     * 将剩余使用次数同步到右下角圆形徽章 tvLeftCount。
     */
    @SuppressLint("SetTextI18n")
    fun updateLeftCount() {
        mBinding.tvLeftCount.text = UsageManager.getRemaining().toString()
    }
}