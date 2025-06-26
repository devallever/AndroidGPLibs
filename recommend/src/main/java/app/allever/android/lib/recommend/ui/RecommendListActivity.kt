package app.allever.android.lib.recommend.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.allever.android.lib.recommend.databinding.ActivityRecommendListBinding
import app.allever.android.lib.recommend.util.RecommendUtils
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
}