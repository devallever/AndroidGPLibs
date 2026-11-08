package app.allever.android.lib.basic.core

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import app.allever.android.lib.basic.BuildConfig
import app.allever.android.lib.basic.core.ProcessHelper.isInMainProcess
import app.allever.android.lib.basic.core.crash.Cockroach
import app.allever.android.lib.basic.core.crash.ExceptionHandler

abstract class App : Application() {
    private var startTime = System.currentTimeMillis()
    override fun attachBaseContext(base: Context?) {
        startTime = System.currentTimeMillis()
        super.attachBaseContext(base)
    }
    override fun onCreate() {
        super.onCreate()

        init(this)

        init()

        initCrashHandler()

        registerActivityLifecycleCallback()

        val endTime = System.currentTimeMillis()
        log(
            "init time: ${endTime - startTime} , isMain = ${
                isInMainProcess(
                    this
                )
            }"
        )
    }

    abstract fun init()

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: Application

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        val mainHandler by lazy { Handler(Looper.getMainLooper()) }

        val DEBUG by lazy {
            BuildConfig.DEBUG
        }

        private var activityCount = 0
        var alreadyInBackground = false

        fun init(context: Context) {
            Companion.context = context.applicationContext
            app = context as Application
        }
    }

    private val mExceptionHandler: ExceptionHandler by lazy {
        object : ExceptionHandler() {
            override fun onUncaughtExceptionHappened(thread: Thread?, throwable: Throwable?) {
                logE("CrashHandler onUncaughtExceptionHappened")
                logE("CrashHandler", "--->onUncaughtExceptionHappened:$thread<---")
                toastDebug(throwable?.message)
            }

            override fun onBandageExceptionHappened(throwable: Throwable?) {
                toastDebug(throwable?.message)
                logE("CrashHandler onBandageExceptionHappened")
//                throwable.printStackTrace() //打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
            }

            override fun onEnterSafeMode() {
                toastDebug("onEnterSafeMode")
                logE("CrashHandler onEnterSafeMode")
            }

            override fun onMayBeBlackScreen(e: Throwable?) {
                toastDebug("onMayBeBlackScreen")
                logE("CrashHandler onMayBeBlackScreen")
//                val thread: Thread = Looper.getMainLooper().getThread()
//                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:$thread<---")
//                //黑屏时建议直接杀死app
//                sysExcepHandler.uncaughtException(thread, RuntimeException("black screen"))
            }
        }
    }

    protected open fun crashHandler(): ExceptionHandler? = mExceptionHandler

    private fun initCrashHandler() {
        if (App.DEBUG) {
            return
        }

        Cockroach.install(this, crashHandler())

    }

    private fun registerActivityLifecycleCallback() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(p0: Activity) {
                //remove from onResume to onStart
                //reason: some case just invoke onStart, suddenly onStop,so onStop invoke many times
                activityCount++
                log("onActivityStarted: ${p0.javaClass.simpleName}, count = ${activityCount}")
            }

            override fun onActivityResumed(activity: Activity) {
                log("onActivityResumed: ${activity::class.java.simpleName}")
                ActivityHelper.onResume(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                log("onActivityPaused: ${activity::class.java.simpleName}")
            }

            override fun onActivityStopped(p0: Activity) {
                activityCount--
                log("onActivityStopped: ${p0.javaClass.simpleName}, count = ${activityCount}")
                alreadyInBackground = activityCount == 0
                ActivityHelper.onStoped()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                log("onActivityDestroyed: ${activity.javaClass.simpleName}, count = ${activityCount}")
            }
        })
    }
}