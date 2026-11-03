package app.allever.android.lib.recommend.data

import androidx.annotation.Keep

@Keep
data class Recommend(
    val id: Int,
    val pkg: String,
    val name: String,
    val desc: String,
    val iconUrl: String,
    var isInstall: Boolean = false
)