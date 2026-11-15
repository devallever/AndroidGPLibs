package app.allever.android.lib.basic.update

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import app.allever.android.lib.basic.core.store.StoreCore
import app.allever.android.lib.basic.ui.dialog.UpdateDialog
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 版本更新检查
 *
 * 数据优先级：
 * 1. 本地缓存
 * 2. 本地缓存不存在或解析失败时，读取 assets 兜底数据
 * 3. 网络最新数据，成功后覆盖已有数据并写入本地缓存
 *
 * 接口地址由各项目通过 [init] 传入，模块内不绑定任何项目的接口。
 * assets 兜底默认 version_code = 0，即离线时不提示更新。
 */
object VersionHelper {

    private const val TAG = "VersionHelper"

    /** 网络超时时间 */
    private const val TIME_OUT = 10_000

    private const val DEFAULT_ASSETS_FILE_NAME = "version.json"
    private const val DEFAULT_CACHE_FILE_NAME = "version_cache.json"

    /** 上次提示的版本号，用于「同版本每天提示一次」 */
    private const val KEY_PROMPT_VERSION_CODE = "update_prompt_version_code"

    /** 上次提示的日期，格式 yyyy-MM-dd */
    private const val KEY_PROMPT_DATE = "update_prompt_date"

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var appContext: Context? = null
    private var cacheFileName = DEFAULT_CACHE_FILE_NAME

    /** 当前安装包的 versionCode */
    private var currentVersionCode = 0L

    /** 配置加载完成前发起的展示请求，加载完成后补弹 */
    private var pendingActivity: WeakReference<Activity>? = null

    @Volatile
    private var isInit = false

    @Volatile
    private var isLoaded = false

    /** 当前生效的版本信息，未加载完成前为空数据（不会提示更新） */
    @Volatile
    var versionInfo: VersionInfo = VersionInfo()
        private set

    /** 当前生效配置的原始 json，便于排查问题 */
    @Volatile
    var rawJson: String = ""
        private set

    /**
     * 初始化版本信息
     *
     * @param context 任意 context，内部使用 applicationContext
     * @param url 项目自己的版本配置接口地址，为空时只使用本地缓存和 assets 兜底
     * @param assetsFileName assets 兜底文件名
     * @param cacheFileName 网络数据的本地缓存文件名
     */
    fun init(
        context: Context,
        url: String = "",
        assetsFileName: String = DEFAULT_ASSETS_FILE_NAME,
        cacheFileName: String = DEFAULT_CACHE_FILE_NAME
    ) {
        if (isInit) {
            log("version: already init, skip")
            return
        }
        isInit = true

        appContext = context.applicationContext
        this.cacheFileName = cacheFileName
        currentVersionCode = readCurrentVersionCode()

        CoroutineScope(Dispatchers.IO).launch {
            // 1. 本地缓存优先，缓存不可用时使用 assets 兜底
            if (!applyVersion(readCache(), "cache")) {
                applyVersion(readAssets(assetsFileName), "assets")
            }
            markLoaded()

            // 2. 再从网络获取最新数据，成功才覆盖并缓存
            if (url.isBlank()) {
                log("version: url is empty, skip network")
                return@launch
            }
            val remoteJson = fetchRemote(url)
            if (applyVersion(remoteJson, "network")) {
                writeCache(remoteJson)
            }
        }
    }

    /** 当前生效数据中是否存在新版本 */
    fun hasUpdate(): Boolean {
        return hasUpdate(versionInfo)
    }

    /**
     * 有更新时展示更新弹窗
     *
     * 非强制更新按「同版本每天提示一次」控制；强制更新每次都会提示。
     *
     * @return 本次是否展示了弹窗
     */
    fun checkAndShow(activity: Activity): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        if (!isLoaded) {
            // 配置还没加载完，等加载完成后补弹
            pendingActivity = WeakReference(activity)
            log("version: config not loaded, pending show")
            return false
        }
        return showIfNeed(activity)
    }

    /**
     * 将配置根级扩展字段（通用字段之外的字段）反序列化为项目自己的配置类
     *
     * @return 无扩展字段或解析失败时返回 null
     */
    fun <T> getProjectConfig(clazz: Class<T>): T? {
        val extras = versionInfo.extras ?: return null
        return try {
            gson.fromJson(extras, clazz)
        } catch (e: Exception) {
            log("version: project config parse failed -> ${e.message}")
            null
        }
    }

    private fun showIfNeed(activity: Activity): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false

        val info = versionInfo
        if (!hasUpdate(info)) return false

        if (!info.force) {
            // 非强制更新：同版本每天只提示一次
            if (!shouldPromptToday(info.versionCode)) {
                log("version: already prompted today, versionCode=${info.versionCode}")
                return false
            }
            savePrompt(info.versionCode)
        }

        log("version: show update dialog, versionCode=${info.versionCode}, force=${info.force}")
        UpdateDialog.show(activity, info)
        return true
    }

    private fun hasUpdate(info: VersionInfo): Boolean {
        if (info.versionCode <= 0) return false
        return info.versionCode > currentVersionCode
    }

    private fun shouldPromptToday(versionCode: Long): Boolean {
        val lastVersionCode = StoreCore.getLong(KEY_PROMPT_VERSION_CODE, 0L)
        val lastDate = StoreCore.getString(KEY_PROMPT_DATE, "")
        return lastVersionCode != versionCode || lastDate != today()
    }

    private fun savePrompt(versionCode: Long) {
        StoreCore.putLong(KEY_PROMPT_VERSION_CODE, versionCode)
        StoreCore.putString(KEY_PROMPT_DATE, today())
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * 配置可用（缓存或 assets 阶段结束）后，补弹加载期间请求过的页面
     */
    private fun markLoaded() {
        if (isLoaded) return
        isLoaded = true

        val activity = pendingActivity?.get()
        pendingActivity = null
        if (activity != null) {
            mainHandler.post { showIfNeed(activity) }
        }
    }

    private fun readCurrentVersionCode(): Long {
        val context = appContext ?: return 0L
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: Exception) {
            log("version: read current version failed -> ${e.message}")
            0L
        }
    }

    /**
     * 解析 json 并应用版本信息
     *
     * @param source 数据来源，仅用于日志
     * @return 是否成功应用
     */
    private fun applyVersion(json: String, source: String): Boolean {
        if (json.isBlank()) {
            log("version: apply failed, source=$source, json is empty")
            return false
        }
        val info = parseVersion(json)
        if (info == null) {
            log("version: apply failed, source=$source, invalid json")
            return false
        }
        versionInfo = info
        rawJson = json
        log(
            "version: apply success, source=$source, " +
                "code=${info.versionCode}, name=${info.versionName}, force=${info.force}"
        )
        return true
    }

    /**
     * 解析版本信息，通用字段走强类型，其余根级字段保留到 extras
     */
    private fun parseVersion(json: String): VersionInfo? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val info = gson.fromJson(json, VersionInfo::class.java) ?: return null
            val extras = root.deepCopy().apply {
                remove("version_name")
                remove("version_code")
                remove("desc")
                remove("force")
            }
            info.copy(extras = if (extras.size() > 0) extras else null)
        } catch (e: Exception) {
            log("version: parse failed -> ${e.message}")
            null
        }
    }

    private fun getCacheFile(): File? {
        val context = appContext ?: return null
        return File(context.filesDir, cacheFileName)
    }

    /**
     * 读取本地缓存
     *
     * @return 缓存的 json，不存在或读取失败时返回空字符串
     */
    private fun readCache(): String {
        return try {
            val file = getCacheFile()
            if (file != null && file.exists()) file.readText() else ""
        } catch (e: Exception) {
            log("version: read cache failed -> ${e.message}")
            ""
        }
    }

    private fun writeCache(json: String) {
        try {
            getCacheFile()?.writeText(json)
        } catch (e: Exception) {
            log("version: write cache failed -> ${e.message}")
        }
    }

    /**
     * 读取 assets 兜底数据
     *
     * @return 兜底 json，不存在或读取失败时返回空字符串
     */
    private fun readAssets(fileName: String): String {
        val context = appContext ?: return ""
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            log("version: read assets failed -> ${e.message}")
            ""
        }
    }

    /**
     * 从网络下载版本信息
     *
     * @return 下载的 json，失败时返回空字符串
     */
    private fun fetchRemote(url: String): String {
        var connection: HttpURLConnection? = null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            connection = conn
            conn.requestMethod = "GET"
            conn.connectTimeout = TIME_OUT
            conn.readTimeout = TIME_OUT
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                log("version: fetch failed, code=$responseCode")
                ""
            }
        } catch (e: Exception) {
            log("version: fetch failed -> ${e.message}")
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }
}
