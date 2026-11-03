package app.allever.android.lib.basic.core

import android.util.Log

class Logger

private const val TAG = "ILogger"

fun log(msg: String?) {
    log(TAG, msg)
}

fun log(tag: String, msg: String?) {
    if (App.DEBUG) {
        logReleaseD(tag, msg)
    }
}

fun logE(msg: String?) {
    logE(TAG, msg)
}

fun logE(tag: String, msg: String?) {
    if (App.DEBUG) {
        logReleaseE(tag, msg)
    }
}

fun logReleaseD(tag: String? = TAG, msg: String?) {
    msg ?: return
    Log.d(tag ?: TAG, msg, null)
}

fun logReleaseE(tag: String? = TAG, msg: String?) {
    msg ?: return
    Log.e(tag ?: TAG, msg, null)
}