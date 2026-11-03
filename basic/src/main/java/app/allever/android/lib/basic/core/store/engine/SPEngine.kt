package app.allever.android.lib.basic.core.store.engine

import android.content.Context
import android.content.SharedPreferences
import app.allever.android.lib.basic.core.App
import app.allever.android.lib.basic.core.store.IStoreEngine

/**
 * SharedPreferences 引擎实现（内置默认）
 *
 * 线程安全：读写均通过 synchronized + apply/commit 保证一致性
 */
class SPEngine : IStoreEngine {

    companion object {
        private const val DEFAULT_NAME = "default_storage"
    }

    @Volatile
    private var sp: SharedPreferences? = null
    private val lock = Any()

    override fun init(context: Context, name: String) {
        if (sp == null) {
            synchronized(lock) {
                if (sp == null) {
                    sp = context.getSharedPreferences(
                        name.ifEmpty { DEFAULT_NAME },
                        Context.MODE_PRIVATE
                    )
                }
            }
        }
    }

    /** 获取 SP 实例，未初始化时使用默认配置自动初始化 */
    private fun requireSp(): SharedPreferences {
        if (sp == null) {
            init(App.context, DEFAULT_NAME)
        }
        return sp!!
    }

    // ==================== 写入 ====================

    override fun putString(key: String, value: String?) {
        edit { if (value != null) putString(key, value) else remove(key) }
    }

    override fun putInt(key: String, value: Int) {
        edit { putInt(key, value) }
    }

    override fun putLong(key: String, value: Long) {
        edit { putLong(key, value) }
    }

    override fun putFloat(key: String, value: Float) {
        edit { putFloat(key, value) }
    }

    override fun putBoolean(key: String, value: Boolean) {
        edit { putBoolean(key, value) }
    }

    override fun putStringSet(key: String, value: Set<String>?) {
        edit { if (value != null) putStringSet(key, value) else remove(key) }
    }

    override fun putAll(map: Map<String, Any?>) {
        edit {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") putStringSet(
                        key,
                        value as Set<String>
                    )

                    null -> remove(key)
                }
            }
        }
    }

    // ==================== 读取 ====================

    override fun getString(key: String, defaultValue: String?): String? =
        requireSp().getString(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Int =
        requireSp().getInt(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Long =
        requireSp().getLong(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Float =
        requireSp().getFloat(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        requireSp().getBoolean(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? =
        requireSp().getStringSet(key, defaultValue)

    // ==================== 对象读写 ====================
    // ==================== 删除 & 清空 ====================

    override fun remove(vararg keys: String) {
        edit { keys.forEach { remove(it) } }
    }

    override fun clear() {
        edit { clear() }
    }

    // ==================== 查询 ====================

    override fun contains(key: String): Boolean = requireSp().contains(key)

    override val allKeys: Set<String>
        get() = requireSp().all.keys

    // ==================== 生命周期 ====================

    override fun destroy() {
        synchronized(lock) {
            sp = null
        }
    }

    // ==================== 内部工具 ====================

    private inline fun edit(action: SharedPreferences.Editor.() -> Unit) {
        synchronized(lock) {
            requireSp().edit().apply(action).apply()
        }
    }
}