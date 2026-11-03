package app.allever.android.lib.basic.core.store

import app.allever.android.lib.basic.core.App
import app.allever.android.lib.basic.core.store.engine.SPEngine

/**
 * 全局存储门面（单例）
 *
 * 使用方式：
 * ```
 * // 不调用 init() 则默认使用 SPEngine
 * Storage.putString("key", "value")
 * Storage.getString("key")
 *
 * // 切换为 MMKV
 * Storage.init { MMKVEngine() }
 * ```
 */
object StoreCore {

    private const val DEFAULT_NAME = "default_storage"

    internal var engine: IStoreEngine = SPEngine()
    private val lock = Any()

    /**
     * 初始化存储引擎
     *
     * 必须在 Application.onCreate 中尽早调用。
     * 不调用则默认使用 [app.allever.android.lib.store.core.engine.SPEngine]。
     *
     * @param engineFactory 引擎工厂，返回一个已配置好的 [IStoreEngine] 实例
     */
    fun init(engineFactory: () -> IStoreEngine) {
        synchronized(lock) {
            engine.destroy()
            val newEngine = engineFactory()
            newEngine.init(App.context, DEFAULT_NAME)
            engine = newEngine
        }
    }

    /** 获取或创建引擎实例 */
    internal fun getOrCreateEngine(): IStoreEngine {
        return engine
    }

    // ==================== String ====================

    fun putString(key: String, value: String?) = getOrCreateEngine().putString(key, value)

    fun getString(key: String, defaultValue: String? = null): String? =
        getOrCreateEngine().getString(key, defaultValue)

    // ==================== Int ====================

    fun putInt(key: String, value: Int) = getOrCreateEngine().putInt(key, value)

    fun getInt(key: String, defaultValue: Int = 0): Int =
        getOrCreateEngine().getInt(key, defaultValue)

    // ==================== Long ====================

    fun putLong(key: String, value: Long) = getOrCreateEngine().putLong(key, value)

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        getOrCreateEngine().getLong(key, defaultValue)

    // ==================== Float ====================

    fun putFloat(key: String, value: Float) = getOrCreateEngine().putFloat(key, value)

    fun getFloat(key: String, defaultValue: Float = 0f): Float =
        getOrCreateEngine().getFloat(key, defaultValue)

    // ==================== Boolean ====================

    fun putBoolean(key: String, value: Boolean) = getOrCreateEngine().putBoolean(key, value)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        getOrCreateEngine().getBoolean(key, defaultValue)

    // ==================== StringSet ====================

    fun putStringSet(key: String, value: Set<String>?) = getOrCreateEngine().putStringSet(key, value)

    fun getStringSet(key: String, defaultValue: Set<String>? = null): Set<String>? =
        getOrCreateEngine().getStringSet(key, defaultValue)

    // ==================== Object (JSON) ====================
    // ==================== 批量操作 ====================

    fun putAll(map: Map<String, Any?>) = getOrCreateEngine().putAll(map)

    // ==================== 删除 & 清空 ====================

    fun remove(vararg keys: String) = getOrCreateEngine().remove(*keys)

    fun clear() = getOrCreateEngine().clear()

    // ==================== 查询 ====================

    fun contains(key: String): Boolean = getOrCreateEngine().contains(key)

    val allKeys: Set<String>
        get() = getOrCreateEngine().allKeys
}