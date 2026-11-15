package app.allever.android.lib.basic.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import app.allever.android.lib.basic.R
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.databinding.DialogBasicUpdateBinding
import app.allever.android.lib.basic.update.VersionInfo

/**
 * 版本更新弹窗
 *
 * 样式参考使用次数耗尽弹窗（激励视频弹窗）：
 * - 顶部渐变圆形图标 + 标题 + 版本号 + 更新说明
 * - 主按钮「立即更新」跳转 Google Play
 * - 强制更新时隐藏「稍后」，且不可取消、拦截返回键
 */
class UpdateDialog private constructor(
    private val activity: Activity,
    private val info: VersionInfo
) {

    private val dialog: Dialog
    private val binding: DialogBasicUpdateBinding =
        DialogBasicUpdateBinding.inflate(LayoutInflater.from(activity))

    init {
        dialog = Dialog(activity).apply {
            setContentView(binding.root)
            setCancelable(!info.force)
            setCanceledOnTouchOutside(!info.force)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // 移除 Dialog decor 自带的默认阴影，只保留 CardView 的 elevation
            window?.decorView?.setBackgroundResource(0)
        }

        bind()
    }

    private fun bind() {
        val versionText = info.versionName.ifBlank { info.versionCode.toString() }
        binding.tvVersionName.text = activity.getString(R.string.bc_update_version, versionText)
        binding.tvUpdateDesc.text = info.desc

        // 强制更新时按钮文案为「退出」，点击直接退出应用
        binding.btnLater.setText(if (info.force) R.string.bc_update_exit else R.string.bc_update_later)

        binding.btnUpdate.setOnClickListener {
            jumpToPlay()
        }
        binding.btnLater.setOnClickListener {
            if (info.force) {
                exitApp()
            } else {
                dialog.dismiss()
            }
        }
    }

    /**
     * 退出应用：关闭当前任务栈内所有页面并结束进程
     */
    private fun exitApp() {
        dialog.dismiss()
        activity.finishAffinity()
        Process.killProcess(Process.myPid())
    }

    /**
     * 跳转 Google Play 本应用详情页，未安装商店时回退到网页版
     */
    private fun jumpToPlay() {
        val pkg = activity.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            setPackage("com.android.vending")
        }
        try {
            activity.startActivity(marketIntent)
            return
        } catch (e: Exception) {
            Log.e("ILogger", "update: open market failed -> ${e.message}")
        }

        try {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                )
            )
        } catch (e: Exception) {
            Log.e("ILogger", "update: open play web failed -> ${e.message}")
            toast(activity.getString(R.string.bc_update_jump_failed))
        }
    }

    fun show() {
        if (activity.isFinishing || activity.isDestroyed) return
        if (dialog.isShowing) return
        dialog.show()
        // Dialog 默认 wrap_content 会导致宽度过窄，这里强制宽度为屏幕的 99%，与激励弹窗一致
        dialog.window?.let { window ->
            val dm = activity.resources.displayMetrics
            val width = (dm.widthPixels * 0.99f).toInt()
            val lp = window.attributes
            lp.width = width
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = lp
        }
    }

    companion object {

        /**
         * 展示版本更新弹窗
         *
         * @param activity 当前 Activity
         * @param info 版本信息
         */
        fun show(activity: Activity, info: VersionInfo) {
            UpdateDialog(activity, info).show()
        }
    }
}
