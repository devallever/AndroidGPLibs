package app.allever.android.lib.basic.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import app.allever.android.lib.basic.BuildConfig
import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.R
import java.util.Locale
import java.util.TimeZone

/**
 * FeedbackHelper — 构建带完整环境信息的反馈邮件。
 *
 * 自动预置以下信息（用户无需手动提供）：
 *   - App 名称 / 版本号（versionName + versionCode）
 *   - 品牌 / 机型 / 产品名
 *   - Android 系统版本（VERSION.RELEASE + SDK_INT）
 *   - 设备硬件指纹（fingerprint / board / cpu ABI）
 *   - 语言 / 时区 / 屏幕密度
 *   - 是否 Debug / Build 类型
 *
 * 所有信息通过 Build 类 + PackageManager 读取，无需任何运行时权限。
 */
object FeedbackHelper {

    private const val FEEDBACK_TO = "funnyappptech@hotmail.com"

    fun feedback(context: Context?) {
        context ?: return

        val appName = ProjectConfig.getAppName()
        val subject = "[$appName] ${context.getString(R.string.bc_feedback)}"
        val body = buildFeedbackBody(context)

        val uri = Uri.parse("mailto:$FEEDBACK_TO")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.bc_feedback_chooser_title))
            )
        } else {
            copyToClipboard(context, body)
        }

        context.startActivity(Intent.createChooser(intent, "Choose mail app"))


    }

    private fun buildFeedbackBody(context: Context): String {
        val divider = "─────────────────────────"
        return buildString {
            appendLine(context.getString(R.string.bc_feedback_user_label))
            appendLine()
            appendLine()
            appendLine(divider)
            appendLine(context.getString(R.string.bc_feedback_env_label))
            appendLine(divider)
            appendLine(buildAppBlock(context))
            appendLine()
            appendLine(buildDeviceBlock())
        }
    }

    private fun buildAppBlock(context: Context): String {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) { "?" }
        val versionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (_: Exception) { -1L }

        val appName = ProjectConfig.getAppName()
        val buildType = BuildConfig.BUILD_TYPE

        return buildString {
            append("• App: ").append(appName).appendLine()
            append("• Version: ").append(versionName)
            if (versionCode >= 0) append(" (").append(versionCode).append(")")
            appendLine()
            append("• Build: ").append(buildType).appendLine()
            append("• Package: ").append(context.packageName).appendLine()
        }
    }

    private fun buildDeviceBlock(): String {
        val osVersion = Build.VERSION.RELEASE ?: "?"
        val sdkInt = Build.VERSION.SDK_INT
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Build.VERSION.SECURITY_PATCH else null
        val brand = Build.BRAND ?: "-"
        val manufacturer = Build.MANUFACTURER ?: "-"
        val model = Build.MODEL ?: "-"
        val product = Build.PRODUCT ?: "-"
        val hardware = Build.HARDWARE ?: "-"
        val board = Build.BOARD ?: "-"
        val abis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.joinToString(", ")
        } else {
            @Suppress("DEPRECATION")
            "${Build.CPU_ABI}, ${Build.CPU_ABI2}"
        }
        val fingerprint = Build.FINGERPRINT?.takeIf { it.isNotEmpty() } ?: "-"
        @Suppress("DEPRECATION")
        val displayId = Build.DISPLAY

        val locale = Locale.getDefault().toString()
        val tz = TimeZone.getDefault().id
        val res = App.context.resources
        val densityDpi = res.displayMetrics.densityDpi
        val screenWidth = res.displayMetrics.widthPixels
        val screenHeight = res.displayMetrics.heightPixels

        return buildString {
            append("• Brand: ").append(brand).appendLine()
            append("• Manufacturer: ").append(manufacturer).appendLine()
            append("• Model: ").append(model).appendLine()
            append("• Product: ").append(product).appendLine()
            append("• Android: ").append(osVersion).append(" (SDK ").append(sdkInt).append(")")
            if (securityPatch != null) append(" · patch ").append(securityPatch)
            appendLine()
            append("• Hardware: ").append(hardware).appendLine()
            append("• Board: ").append(board).appendLine()
            append("• ABI: ").append(abis).appendLine()
            append("• Locale: ").append(locale).appendLine()
            append("• Timezone: ").append(tz).appendLine()
            append("• Screen: ").append(screenWidth).append("x").append(screenHeight)
                .append(" @ ").append(densityDpi).append("dpi").appendLine()
            append("• Fingerprint: ").append(fingerprint).appendLine()
            append("• Display: ").append(displayId).appendLine()
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val label = context.getString(R.string.bc_feedback_clip_label, ProjectConfig.getAppName())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
        } else {
            @Suppress("DEPRECATION")
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
            cm.text = text
        }
        // 无邮件客户端 → 仅复制，不弹 Toast（避免 com.blankj 引入）
    }
}
