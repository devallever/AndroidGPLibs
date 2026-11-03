package app.allever.android.lib.basic.core

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES_FULL.R
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.funnyappptech.android.fake.call.lib.HandlerHelper
import java.lang.ref.WeakReference


abstract class AbstractActivity : AppCompatActivity(){

    protected val mHandler by lazy {
        HandlerHelper.mainHandler
    }

    private var mWeakRefActivity: WeakReference<Activity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {


        if (isFullScreen()) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            StatusBarCompat.translucentStatusBar(this, true)
        }

        // 适配导航栏：根据主题设置不透明颜色
        window.navigationBarColor = if (isDarkMode()) Color.BLACK else Color.WHITE
        // 导航栏按钮图标颜色与背景相反
        val navController = WindowInsetsControllerCompat(window, window.decorView)
        navController.isAppearanceLightNavigationBars = !isDarkMode()


        //状态栏颜色
        if (isDarkMode()) {
            StatusBarCompat.cancelLightStatusBar(this)
        } else {
            StatusBarCompat.changeToLightStatusBar(this)
        }

        super.onCreate(savedInstanceState)
        log(this.javaClass.simpleName)
        mWeakRefActivity = WeakReference(this)
        ActivityHelper.add(mWeakRefActivity)

    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        if (enableAdaptNavigationBar()) {
            log("适配导航栏：${this.javaClass.simpleName}")
            adaptNavigationBar(view)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (enableAdaptNavigationBar()) {
            log("适配导航栏：${this.javaClass.simpleName}")
            val contentView = window.findViewById<ViewGroup>(android.R.id.content)
            val childView = contentView.getChildAt(0)
            if (childView != null) {
                adaptNavigationBar(childView)
            }
        }
    }



    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        ActivityHelper.resumeTop(mWeakRefActivity)
        LifecycleHelper.pullRootOwner(this)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        ActivityHelper.remove(mWeakRefActivity)
        super.onDestroy()
    }

    private var firstPressedBackTime = 0L
    protected fun checkExit(runnable: Runnable? = null) {
        if (System.currentTimeMillis() - firstPressedBackTime < 2000) {
            runnable?.run()
            super.onBackPressed()
        } else {
            toast("再按一次退出")
            firstPressedBackTime = System.currentTimeMillis()
        }
    }

    protected fun setVisibility(view: View, show: Boolean) {
        if (show) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    protected fun postDelay(task: Runnable, delay: Long = 1000) {
        App.mainHandler.postDelayed(task, delay)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    protected open fun enableEnterAnim(): Boolean {
        return true
    }


    protected open fun showTopBar(): Boolean = true

    /**
     * true: 黑夜模式，白色字体
     * false：白光模式，黑色字体
     *
     * @return isDarkMode
     */
    protected open fun isDarkMode(): Boolean {
        return false
    }

    /**
     * 是否全屏
     */
    protected open fun isFullScreen(): Boolean = true

    protected open fun enableAdaptNavigationBar(): Boolean = true

    /**
     * 适配状态栏
     */
    protected fun adaptStatusBar(view: View) {
        ViewHelper.setMarginTop(view, DisplayHelper.getStatusBarHeight(this))
    }

    /**
     * 适配导航栏：确保内容不被导航栏覆盖
     */
    private fun adaptNavigationBar(view: View?) {
        if (view == null) return
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            log("适配导航栏：导航栏高度 = ${navigationBars.bottom}")
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
            insets
        }
    }

    protected open fun hideSystemBar() = false

    protected open fun hideSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    protected open fun fullActivity() {
        val window = window
        val lp = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.addFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES or WindowManager.LayoutParams.FLAG_FULLSCREEN)
            // 如果是 Android 9.0，则需要对刘海屏进行适配，否则也会导致 WindowManager 移动不到刘海屏的位置上面
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            getWindow().decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

}