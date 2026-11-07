package app.allever.android.lib.recommend.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import app.allever.android.lib.ad.databinding.ActivityRecommendListBinding
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.RecommendUtils.log
import app.allever.android.lib.recommend.util.StatusBarCompat

class RecommendListActivity : AppCompatActivity(){

    private lateinit var  mBinding: ActivityRecommendListBinding
     private val mViewModel by lazy {
        RecommendListViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (true) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            StatusBarCompat.translucentStatusBar(this, true)
        }

        //状态栏颜色
        if (false) {
            StatusBarCompat.cancelLightStatusBar(this)
        } else {
            StatusBarCompat.changeToLightStatusBar(this)
        }


        super.onCreate(savedInstanceState)
        mBinding = ActivityRecommendListBinding.inflate(layoutInflater)
        setContentView(mBinding. root)
        log("适配导航栏：${this.javaClass.simpleName}")
        val contentView = window.findViewById<ViewGroup>(android.R.id.content)
        val childView = contentView.getChildAt(0)
        if (childView != null) {
            adaptNavigationBar(childView)
        }
        init()
    }

    private fun init() {
        RecommendUtils.setMarginTop(
            mBinding.tvTitle,
            RecommendUtils.getStatusBarHeight(this)
        )
        mBinding.apply {
            rvRecommend.layoutManager = LinearLayoutManager(this@RecommendListActivity)
            rvRecommend.adapter = mViewModel.adapter

            ivBack.setOnClickListener {
                finish()
            }
        }
    }


    private fun adaptNavigationBar(view: View?) {
        if (view == null) return
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            log("适配导航栏：导航栏高度 = ${navigationBars.bottom}")
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
            insets
        }
    }

}