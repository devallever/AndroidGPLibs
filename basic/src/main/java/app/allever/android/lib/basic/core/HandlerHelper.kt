package app.allever.android.lib.basic.core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object HandlerHelper {

    val mainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    val threadHandler by lazy {
        val handlerThread = HandlerThread("threadHandler")
        handlerThread.start()
        Handler(handlerThread.looper)
    }
}