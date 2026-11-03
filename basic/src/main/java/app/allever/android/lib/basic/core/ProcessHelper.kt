package app.allever.android.lib.basic.core

import android.app.Application
import android.os.Process
import java.io.File
import java.io.IOException

object ProcessHelper {
    fun executeOnMain(application: Application, task: Runnable) {
        if (isInMainProcess(application)) {
            task.run()
        }
    }

    fun getProcessName(): String {
        return getProcessName(Process.myPid())
    }

    fun isInMainProcess(
        application: Application,
        processName: String = application.packageName
    ): Boolean {
        return processName == getProcessName(Process.myPid())
    }

    private fun getProcessName(pid: Int): String {
        val reader = try {
            val file = File("/proc/$pid/cmdline")
            val bufferReader = file.bufferedReader()

            bufferReader
        } catch (e: IOException) {
            return ""
        }
        return try {
            reader.use { it.readLine().trim().substringBefore('\u0000') }
        } catch (e: IOException) {
            ""
        }
    }
}