package app.allever.android.lib.recommend.data

import android.content.Context
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.removeItemIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object RecommendHelper {

    /** assets 中的兜底数据 */
    private const val ASSETS_FILE_NAME = "recommend.json"

    /** 网络数据缓存文件名（保存在 filesDir 下） */
    private const val CACHE_FILE_NAME = "recommend_cache.json"

    /** 远程推荐数据地址 */
    private const val REMOTE_URL =
        "https://devallever.github.io/business/recommend/recommend.json"

    /** 网络超时时间 */
    private const val TIME_OUT = 10_000

    val list = mutableListOf<Recommend>()

    fun init(context: Context) {
        RecommendUtils.context = context.applicationContext
        initRecommend()
    }

    /**
     * 初始化推荐数据，优先级：
     * 1. 本地缓存
     * 2. 本地缓存不存在或解析失败时，读取 assets 兜底数据
     * 3. 网络最新数据，成功后覆盖已有数据并写入本地缓存
     */
    private fun initRecommend() {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. 先使用本地缓存，缓存不可用时使用 assets 兜底
            val cacheJson = readCache()
            if (!applyRecommend(cacheJson, "cache")) {
                applyRecommend(
                    RecommendUtils.readFile2String(RecommendUtils.context, ASSETS_FILE_NAME),
                    "assets"
                )
            }

            // 2. 再从网络获取最新数据，成功则覆盖并缓存
            val remoteJson = fetchRemote()
            if (applyRecommend(remoteJson, "network")) {
                writeCache(remoteJson)
            }
        }
    }

    /**
     * 解析 json 并应用推荐数据
     *
     * @param json 推荐数据 json
     * @param source 数据来源，仅用于日志
     * @return 是否成功应用数据
     */
    private fun applyRecommend(json: String, source: String): Boolean {
        if (json.isBlank()) {
            RecommendUtils.log("recommend apply failed, source=$source, json is empty")
            return false
        }
        val recommendResponse = RecommendUtils.fromJson(json, RecommendResponse::class.java)
        val data = recommendResponse?.data
        if (data.isNullOrEmpty()) {
            RecommendUtils.log("recommend apply failed, source=$source, data is empty")
            return false
        }

        list.clear()
        list.addAll(data)
        // 过滤自身应用
        list.removeItemIf {
            it.pkg == RecommendUtils.context.packageName
        }
        list.forEach {
            it.isInstall = isAppInstall(it.pkg)
            RecommendUtils.log(RecommendUtils.toJson(it))
        }
        RecommendUtils.log("recommend apply success, source=$source, size=${list.size}")
        return true
    }

    private fun isAppInstall(pkg: String): Boolean {
        return try {
            RecommendUtils.context.packageManager.getPackageInfo(pkg, 0) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从网络下载推荐数据
     *
     * @return 下载的 json，失败时返回空字符串
     */
    private fun fetchRemote(): String {
        var connection: HttpURLConnection? = null
        return try {
            val conn = URL(REMOTE_URL).openConnection() as HttpURLConnection
            connection = conn
            conn.requestMethod = "GET"
            conn.connectTimeout = TIME_OUT
            conn.readTimeout = TIME_OUT
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                RecommendUtils.log("recommend fetch failed, code=$responseCode")
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            connection?.disconnect()
        }
    }

    private fun getCacheFile() = File(RecommendUtils.context.filesDir, CACHE_FILE_NAME)

    /**
     * 读取本地缓存
     *
     * @return 缓存的 json，不存在或读取失败时返回空字符串
     */
    private fun readCache(): String {
        return try {
            val file = getCacheFile()
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 写入本地缓存
     */
    private fun writeCache(json: String) {
        try {
            getCacheFile().writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getRecommendApp(): Recommend {
        return list.first()
    }
}
