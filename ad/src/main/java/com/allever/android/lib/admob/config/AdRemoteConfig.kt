package com.allever.android.lib.admob.config

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * 远程广告配置
 *
 * 模块只认 switch / interval 两个字段，
 * 其余根级字段会原样保存在 [extras] 中，供各项目自行解析扩展，
 * 后续接口在根级新增字段时模块无需改动
 */
data class AdRemoteConfig(
    @SerializedName("switch") val switchConfig: AdSwitchConfig = AdSwitchConfig(),
    @SerializedName("interval") val intervalConfig: AdIntervalConfig = AdIntervalConfig(),
    val extras: JsonObject? = null
)

/** 广告开关，缺省全部开启 */
data class AdSwitchConfig(
    @SerializedName("open") val isOpen: Boolean = true,
    val inter: Boolean = true,
    val reward: Boolean = true,
    val native: Boolean = true,
    val banner: Boolean = true
)

/** 广告展示间隔配置，单位：秒 */
data class AdIntervalConfig(
    val inter: Long = 30,
    val reward: Long = 30,
    val native: Long = 30,
    val banner: Long = 15
)
