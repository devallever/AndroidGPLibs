package app.allever.android.lib.basic.update

import androidx.annotation.Keep
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * 版本更新信息
 *
 * version_name / version_code / desc / force 为各项目通用字段，
 * 其余根级字段会原样保存在 [extras] 中，供各项目自行解析扩展
 */
@Keep
data class VersionInfo(
    @SerializedName("version_name") val versionName: String = "",
    @SerializedName("version_code") val versionCode: Long = 0L,
    @SerializedName("desc") val desc: String = "",
    @SerializedName("force") val force: Boolean = false,
    val extras: JsonObject? = null
)
