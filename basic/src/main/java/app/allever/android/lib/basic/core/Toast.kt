package app.allever.android.lib.basic.core

import android.widget.Toast

class Toast

fun toast(obj: Any?) {
    toast(obj, false)
}

fun toastLong(obj: Any?) {
    toast(obj, true)
}

fun toastDebug(obj: Any?) {
    if (App.DEBUG) {
        toast(obj)
    }
}

private fun toast(obj: Any?, isLong: Boolean = false) {
    obj ?: return
    val message = when (obj) {
        is String -> {
            obj
        }

        is Int -> {
            App.context.getString(obj)
        }

        else -> {
            obj.toString()
        }
    }
    val duration = if (isLong) {
        Toast.LENGTH_LONG
    } else {
        Toast.LENGTH_SHORT
    }
    App.mainHandler.post {
        Toast.makeText(App.context, message, duration).show()
    }
}