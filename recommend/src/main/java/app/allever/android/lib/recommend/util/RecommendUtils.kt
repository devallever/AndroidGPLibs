package app.allever.android.lib.recommend.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import app.allever.android.lib.recommend.BuildConfig
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.WeakHashMap

@SuppressLint("StaticFieldLeak")
object RecommendUtils {

    internal lateinit var context: Context

    //Display///////////////////////////////////////////////////////////////////////////////////////
    private const val STATUS_BAR_HEIGHT = "status_bar_height"
    private const val NAVIGATION_BAR_HEIGHT = "navigation_bar_height"

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context = this.context): Int =
        getXBarHeight(context, STATUS_BAR_HEIGHT)

    /**
     * 获取导航栏栏高度
     */

    fun getNavigationBarHeight(context: Context): Int =
        getXBarHeight(context, NAVIGATION_BAR_HEIGHT)

    private fun getXBarHeight(context: Context, parameterName: String): Int {
        var height = 0
        val resourceId: Int =
            context.resources.getIdentifier(parameterName, "dimen", "android")
        if (resourceId > 0) {
            height = context.resources.getDimensionPixelSize(resourceId)
        }

        return height
    }

    //View//////////////////////////////////////////////////////////////////////////////////
    fun setMarginTop(view: View, marginTop: Int) {
        view.post {
            val lp = view.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = marginTop
            view.layoutParams = lp
        }
    }

    //log//////////////////////////////////////////////////////////////////////////////////
    fun log( msg: String?) {
        msg ?: return
        if (BuildConfig.DEBUG) {
            Log.d("RecommendLog", msg)
        }
    }

    //Gson/////////////////////////////////////////////////////////////////////////////////////////
    private val mGson = Gson()
    fun toJson(obj: Any?): String {
        return try {
            mGson.toJson(obj)
        } catch (e: Exception) {
            e.message ?: ""
        }
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T? {
        return mGson.fromJson(json, clazz)
    }

    //Assets// ///////////////////////////////////////////////////////////////////////////////////////
    private val mAssetsRes = WeakHashMap<String, Any>()
    suspend fun readFile2String(context: Context = this.context, fileName: String) =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(fileName)
                val bufferReader = inputStream.bufferedReader()
                bufferReader.use {
                    return@withContext it.readText()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext ""
        }

    //System/////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 检查是否安装某包
     *
     * @param context
     * @param packageName 包名
     * @return
     */
    fun isAppExist(context: Context, packageName: String): Boolean {
        try {
            context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun openApp(pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("can not open app")
        }
    }

    fun jumpToAppStore(pkg: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=${pkg}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setPackage("com.android.vending")
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            toast("can not open market")
        }
    }

    //ImageLoader/////////////////////////////////////////////////////////////////////////////////////

    fun loadCircle(
        resource: Any,
        imageView: ImageView,
        borderWidthDp: Int?,
        borderColor: Int?,
    ) {
        imageView.load(resource) {
            transformations(
                BorderCircleTransformation(
                    dip2px(borderWidthDp ?: 0),
                    borderColor ?: Color.parseColor("#00000000")
                )
            )
        }
    }

    fun loadRound(
        resource: Any,
        imageView: ImageView,
        radiusDp: Float?
    ) {
        imageView.load(resource) {
            transformations(
                RoundedCornersTransformation(
                    radius = dip2px(radiusDp ?: 0f).toFloat()
                )
            )
        }
    }

    //Display/////////////////////////////////////////////////////////////////////////////////////////
    fun dip2px(dip: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dip * density + 0.5f).toInt()
    }

    fun dip2px(dip: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dip * density + 0.5f).toInt()
    }
}