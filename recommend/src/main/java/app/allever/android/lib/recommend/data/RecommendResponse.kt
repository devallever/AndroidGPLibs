package app.allever.android.lib.recommend.data

import androidx.annotation.Keep

@Keep
class RecommendResponse {
    var msg: String? = null
    var code: Int? = null
    var data: List<Recommend>? = null
}
