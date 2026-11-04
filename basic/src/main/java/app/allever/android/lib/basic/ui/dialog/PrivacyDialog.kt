package app.allever.android.lib.basic.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.R
import app.allever.android.lib.basic.databinding.DialogBasicPrivacyBinding
import app.funnyappptech.android.fake.call.util.SystemUtils

/**
 * 隐私政策同意弹窗 —— 华为渠道首次启动时弹出。
 *
 * 特性：
 * - 必须做出选择（不可取消、不可点击外部关闭）
 * - 说明文案中"隐私政策"关键词带下划线、可点击跳转
 * - 与项目现有 Dialog 风格统一（CardView + 圆角 + 透明窗口）
 *
 * 使用示例：
 * ```
 * PrivacyDialog.show(
 *     activity,
 *     onAgree = { FakeCall.updateAgreePrivacy(); normalWay() },
 *     onDisagree = { toast("..."); finish() }
 * )
 * ```
 */
class PrivacyDialog private constructor(
    private val activity: Activity,
    private val onAgree: () -> Unit,
    private val onDisagree: () -> Unit
) {

    private val dialog: Dialog
    private val binding: DialogBasicPrivacyBinding = DialogBasicPrivacyBinding.inflate(LayoutInflater.from(activity))
    private var isDismissed = false

    init {
        dialog = Dialog(activity).apply {
            setContentView(binding.root)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.decorView?.setBackgroundResource(0)
        }

        setupClickableMessage()
        setupListeners()
    }

    /**
     * 为说明文案中的"隐私政策"关键词添加下划线和点击跳转。
     * 使用 SpannableString + ClickableSpan 实现局部可点击。
     *
     * 安全检查：indexOf < 0 时降级为普通文本，避免 setSpan 崩溃。
     */
    private fun setupClickableMessage() {
        val rawMessage = activity.getString(R.string.bc_privacy_dialog_message)
        val span = SpannableString(rawMessage)

        // 同时匹配中英文关键词，兼容两种语言环境
        val keywordCn = "隐私政策"
        val keywordEn = "Privacy Policy"

        val startCn = rawMessage.indexOf(keywordCn)
        val startEn = rawMessage.indexOf(keywordEn)

        val (keyword, start, end) = when {
            startCn >= 0 -> Triple(keywordCn, startCn, startCn + keywordCn.length)
            startEn >= 0 -> Triple(keywordEn, startEn, startEn + keywordEn.length)
            else -> Triple("", -1, -1)
        }

        if (start >= 0) {
            // 可点击 + 下划线
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    SystemUtils.startWebView(activity, ProjectConfig.getPrivacyUrl())
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = activity.getColor(R.color.bc_gradient_start)
                    ds.isFakeBoldText = true
                }
            }
            span.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 加粗
            span.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.tvDialogMessage.text = span
        binding.tvDialogMessage.movementMethod = LinkMovementMethod.getInstance()
        // 消除点击时的系统高亮背景
        binding.tvDialogMessage.highlightColor = Color.TRANSPARENT
    }

    private fun setupListeners() {
        binding.btnAgree.setOnClickListener {
            dismiss { onAgree() }
        }

        binding.btnDisagree.setOnClickListener {
            dismiss { onDisagree() }
        }
    }

    private fun dismiss(callback: () -> Unit) {
        if (isDismissed) return
        isDismissed = true
        callback()
        dialog.dismiss()
    }

    fun show() {
        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
            dialog.window?.let { window ->
                val dm = activity.resources.displayMetrics
                val width = (dm.widthPixels * 0.92f).toInt()
                val lp = window.attributes
                lp.width = width
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = lp
            }
        }
    }

    companion object {

        /**
         * 显示隐私政策同意弹窗。
         *
         * @param activity 当前 Activity
         * @param onAgree 用户点击"同意并继续"回调
         * @param onDisagree 用户点击"不同意"回调
         */
        fun show(
            activity: Activity,
            onAgree: () -> Unit,
            onDisagree: () -> Unit
        ) {
            PrivacyDialog(
                activity = activity,
                onAgree = onAgree,
                onDisagree = onDisagree
            ).show()
        }
    }
}
