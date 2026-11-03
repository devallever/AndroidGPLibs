package app.allever.android.lib.basic.core.store

import app.allever.android.lib.basic.core.App
import app.allever.android.lib.basic.core.store.engine.SPEngine
import java.util.concurrent.ConcurrentHashMap

/**
 * 多实例存储工厂
 *
 * 支持按名称创建多个隔离的存储实例，每个实例使用独立的存储区域。
 *
 * 使用方式：
 * ```
 * // 获取默认引擎的命名实例
 * val userStore = StorageKit.get("user")
 * userStore.putString("name", "Tom")
 *
 * // 指定自定义引擎
 * val cacheStore = StorageKit.get("cache") { DataStoreEngine() }
 *
 * // 释放实例
 * StorageKit.release("user")
 * ```
 */
object StorageKit {

    private val instances = ConcurrentHashMap<String, IStorage>()

    /**
     * 获取或创建命名存储实例
     *
     * @param name 存储名称，用于隔离不同数据域
     * @param engineFactory 自定义引擎工厂。不传则使用全局 [app.allever.android.lib.store.core.StoreCore] 的引擎；
     *                     若 [Storage] 也未初始化则自动使用 [SPEngine]
     * @return 存储实例
     */
    fun get(
        name: String,
        engineFactory: (() -> IStoreEngine)? = null
    ): IStorage {
        return instances.getOrPut(name) {
            val engine = engineFactory?.invoke() ?: createDefaultEngine(name)
            StorageImpl(engine)
        }
    }

    /** 移除并销毁指定名称的存储实例 */
    fun release(name: String) {
        instances.remove(name)?.let { (it as? StorageImpl)?.engine?.destroy() }
    }

    /** 销毁所有实例 */
    fun releaseAll() {
        instances.values.forEach { (it as? StorageImpl)?.engine?.destroy() }
        instances.clear()
    }

    private fun createDefaultEngine(name: String): IStoreEngine {
        return SPEngine().also { it.init(App.context, name) }
    }

    /**
     * 内部实现：持有引擎引用的 IStorage
     */
    internal class StorageImpl(override val engine: IStoreEngine) : IStorage
}

/**
 * 存储接口 — 由 [StorageKit] 返回的实例类型
 *
 * 与 [app.allever.android.lib.store.core.StoreCore] 门面方法一致，但绑定到特定引擎实例，
 * 适用于需要多存储域隔离的场景。
 */
interface IStorage {

    /** 底层引擎引用 */
    val engine: IStoreEngine

    fun putString(key: String, value: String?) = engine.putString(key, value)
    fun getString(key: String, defaultValue: String? = null): String? =
        engine.getString(key, defaultValue)

    fun putInt(key: String, value: Int) = engine.putInt(key, value)
    fun getInt(key: String, defaultValue: Int = 0): Int = engine.getInt(key, defaultValue)

    fun putLong(key: String, value: Long) = engine.putLong(key, value)
    fun getLong(key: String, defaultValue: Long = 0L): Long = engine.getLong(key, defaultValue)

    fun putFloat(key: String, value: Float) = engine.putFloat(key, value)
    fun getFloat(key: String, defaultValue: Float = 0f): Float = engine.getFloat(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) = engine.putBoolean(key, value)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        engine.getBoolean(key, defaultValue)

    fun putStringSet(key: String, value: Set<String>?) = engine.putStringSet(key, value)
    fun getStringSet(key: String, defaultValue: Set<String>? = null): Set<String>? =
        engine.getStringSet(key, defaultValue)

    fun putAll(map: Map<String, Any?>) = engine.putAll(map)

    fun remove(vararg keys: String) = engine.remove(*keys)
    fun clear() = engine.clear()

    fun contains(key: String): Boolean = engine.contains(key)
    val allKeys: Set<String> get() = engine.allKeys
}
