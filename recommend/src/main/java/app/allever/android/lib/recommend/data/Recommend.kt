package app.allever.android.lib.recommend.data

import androidx.annotation.Keep

@Keep
data class Recommend(
    val id: Int,
    val pkg: String,
    val category: String,
    val size: String,
    val name: String,
    val desc: String,
    val longDesc: String,
    val iconUrl: String,
    val bannerUrl: String,
    val screenUrl: List<String> = emptyList(),
    val version: String,
    var isInstall: Boolean = false
)