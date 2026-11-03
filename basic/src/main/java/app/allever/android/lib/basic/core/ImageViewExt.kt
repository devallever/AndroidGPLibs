package app.allever.android.lib.basic.core

import android.widget.ImageView
import androidx.core.graphics.toColorInt

fun ImageView.load(
    resource: Any,
    loadOrigin: Boolean = true
) {
    CoilLoader.load(resource, this, null, null)
}



fun ImageView.loadCircle(
    any: Any,
    borderWidth: Int = 0,
    borderColor: Int? = "#ffffff".toColorInt(),
    errorResId: Int? = null,
    placeholder: Int? = null
) {
    CoilLoader.loadCircle(any, this, borderWidth, borderColor, errorResId, placeholder)
}

fun ImageView.loadRound(
    any: Any,
    radius: Float = 8f,
    loadOrigin: Boolean = true,
    errorResId: Int? = null,
    placeholder: Int? = null
) {
    CoilLoader.loadRound(any, this, radius, errorResId, placeholder)
}

fun ImageView.loadBlur(any: Any, radius: Float = 10f) {
    CoilLoader.loadBlur(any, this, radius)
}