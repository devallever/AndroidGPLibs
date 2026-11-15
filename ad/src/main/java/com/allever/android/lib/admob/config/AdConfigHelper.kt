package com.allever.android.lib.admob.config

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.allever.android.lib.admob.IAdConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 远程广告配置管理
 *
 * 数据优先级：
 * 1. 本地缓存
 * 2. 本地缓存不存在或解析失败时，读取 assets 兜底数据
 * 3. 网络最新数据，成功后覆盖已有数据并写入本地缓存
 *
 * 接口地址由各项目通过 [init] 传入，模块内不绑定任何项目的接口
 */
object AdConfigHelper {

    private const val TAG = "ILogger"

    /** 网络超时时间 */
    private const val TIME_OUT = 10_000

    private const val DEFAULT_ASSETS_FILE_NAME = "ad_config.json"
    private const val DEFAULT_CACHE_FILE_NAME = "ad_config_cache.json"

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var appContext: Context? = null
    private var cacheFileName = DEFAULT_CACHE_FILE_NAME
    private var onLoaded: ((AdRemoteConfig) -> Unit)? = null
    private var notified = false

    @Volatile
    private var isInit = false

    /**
     * 当前生效的广告配置
     * 未初始化或加载完成前为「全部开启 + 默认间隔」，保证行为与改造前一致
     */
    @Volatile
    var config: AdRemoteConfig = AdRemoteConfig()
        private set

    /** 当前生效配置的原始 json，便于排查问题 */
    @Volatile
    var rawJson: String = ""
        private set

    /**
     * 初始化广告配置
     *
     * @param context 任意 context，内部使用 applicationContext
     * @param url 项目自己的配置接口地址，为空时只使用本地缓存和 assets 兜底
     * @param assetsFileName assets 兜底文件名，项目可替换为自己的兜底文件
     * @param cacheFileName 网络配置的本地缓存文件名
     * @param onLoaded 首次拿到可用配置后的回调（主线程），供项目读取扩展字段
     */
    fun init(
        context: Context,
        url: String = "",
        assetsFileName: String = DEFAULT_ASSETS_FILE_NAME,
        cacheFileName: String = DEFAULT_CACHE_FILE_NAME,
        onLoaded: ((AdRemoteConfig) -> Unit)? = null
    ) {
        if (isInit) {
            log("adConfig: already init, skip")
            return
        }
        isInit = true

        appContext = context.applicationContext
        this.cacheFileName = cacheFileName
        this.onLoaded = onLoaded

        CoroutineScope(Dispatchers.IO).launch {
            // 1. 本地缓存优先，缓存不可用时使用 assets 兜底
            if (!applyConfig(readCache(), "cache")) {
                applyConfig(readAssets(assetsFileName), "assets")
            }

            // 2. 再从网络获取最新数据，成功才覆盖并缓存
            if (url.isBlank()) {
                log("adConfig: url is empty, skip network")
                return@launch
            }
            val remoteJson = fetchRemote(url)
            if (applyConfig(remoteJson, "network")) {
                writeCache(remoteJson)
            }
        }
    }

    /**
     * 判断某类广告开关是否开启
     * @param type 见 [IAdConfig] 中的 OPEN_AD / INTER_AD / REWARD_AD / NATIVE_AD / BANNER_AD
     */
    fun isEnabled(type: Int): Boolean {
        val switchConfig = config.switchConfig
        return when (type) {
            IAdConfig.OPEN_AD -> switchConfig.isOpen
            IAdConfig.INTER_AD -> switchConfig.inter
            IAdConfig.REWARD_AD -> switchConfig.reward
            IAdConfig.NATIVE_AD -> switchConfig.native
            IAdConfig.BANNER_AD -> switchConfig.banner
            else -> true
        }
    }

    /**
     * 获取某类广告的间隔时间
     * @return 毫秒，非法值（0 / 负数）兜底为 1 秒
     */
    fun getIntervalMs(type: Int): Long {
        val intervalConfig = config.intervalConfig
        val seconds = when (type) {
            IAdConfig.INTER_AD -> intervalConfig.inter
            IAdConfig.REWARD_AD -> intervalConfig.reward
            IAdConfig.NATIVE_AD -> intervalConfig.native
            IAdConfig.BANNER_AD -> intervalConfig.banner
            else -> 30L
        }
        return seconds.coerceAtLeast(1L) * 1000L
    }

    /**
     * 将配置根级扩展字段（switch / interval 之外的字段）反序列化为项目自己的配置类
     *
     * @return 无扩展字段或解析失败时返回 null
     */
    fun <T> getProjectConfig(clazz: Class<T>): T? {
        val extras = config.extras ?: return null
        return try {
            gson.fromJson(extras, clazz)
        } catch (e: Exception) {
            log("adConfig: project config parse failed -> ${e.message}")
            null
        }
    }

    /**
     * 解析 json 并应用配置
     *
     * @param source 数据来源，仅用于日志
     * @return 是否成功应用
     */
    private fun applyConfig(json: String, source: String): Boolean {
        if (json.isBlank()) {
            log("adConfig: apply failed, source=$source, json is empty")
            return false
        }
        val parsed = parseConfig(json)
        if (parsed == null) {
            log("adConfig: apply failed, source=$source, invalid json")
            return false
        }
        config = parsed
        rawJson = json

        val switchConfig = parsed.switchConfig
        val intervalConfig = parsed.intervalConfig
        log(
            "adConfig: apply success, source=$source, " +
                "open=${switchConfig.isOpen}, " +
                "inter=${switchConfig.inter}/${intervalConfig.inter}s, " +
                "reward=${switchConfig.reward}/${intervalConfig.reward}s, " +
                "native=${switchConfig.native}/${intervalConfig.native}s, " +
                "banner=${switchConfig.banner}/${intervalConfig.banner}s"
        )
        notifyLoaded()
        return true
    }

    /**
     * 解析配置，switch / interval 走强类型，其余根级字段原样保留到 extras
     */
    private fun parseConfig(json: String): AdRemoteConfig? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val switchConfig = root.get("switch")?.let {
                gson.fromJson(it, AdSwitchConfig::class.java)
            } ?: AdSwitchConfig()
            val intervalConfig = root.get("interval")?.let {
                gson.fromJson(it, AdIntervalConfig::class.java)
            } ?: AdIntervalConfig()
            val extras = root.deepCopy().apply {
                remove("switch")
                remove("interval")
            }
            AdRemoteConfig(
                switchConfig = switchConfig,
                intervalConfig = intervalConfig,
                extras = if (extras.size() > 0) extras else null
            )
        } catch (e: Exception) {
            log("adConfig: parse failed -> ${e.message}")
            null
        }
    }

    private fun notifyLoaded() {
        if (notified) return
        val callback = onLoaded ?: return
        notified = true
        onLoaded = null
        mainHandler.post { callback.invoke(config) }
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
            log("adConfig: read cache failed -> ${e.message}")
            ""
        }
    }

    private fun writeCache(json: String) {
        try {
            getCacheFile()?.writeText(json)
        } catch (e: Exception) {
            log("adConfig: write cache failed -> ${e.message}")
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
            log("adConfig: read assets failed -> ${e.message}")
            ""
        }
    }

    /**
     * 从网络下载配置
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
                log("adConfig: fetch failed, code=$responseCode")
                ""
            }
        } catch (e: Exception) {
            log("adConfig: fetch failed -> ${e.message}")
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
    }
}
