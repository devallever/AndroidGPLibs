package app.allever.android.lib.recommend.util

import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast
import app.allever.android.lib.recommend.BuildConfig
import app.allever.android.lib.recommend.util.RecommendUtils.dip2px

internal fun <T> MutableList<T>.removeItemIf(needRemove: (item: T) -> Boolean) {
    val iterator: MutableIterator<T> = this.iterator()
    while (iterator.hasNext()) {
        val bean = iterator.next()
        if (needRemove(bean)) {
            iterator.remove()
        }
    }
}

internal fun ImageView.loadCircle(
    any: Any,
    borderWidth: Int = 0,
    borderColor: Int? = Color.parseColor("#00000000"),
) {
    RecommendUtils.loadCircle(any, this, borderWidth, borderColor)
}

internal fun ImageView.loadRound(
    any: Any,
    radius: Float? = dip2px(10).toFloat(),
) {
    RecommendUtils.loadRound(any, this, radius)
}

internal fun toast(message: String?) {
    if (BuildConfig.DEBUG) {
        Toast.makeText(RecommendUtils.context, message, Toast.LENGTH_SHORT).show()
    }
}



