package app.allever.android.lib.basic.ui

import android.os.Bundle
import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.core.AbstractActivity
import app.allever.android.lib.basic.core.ActivityHelper
import app.allever.android.lib.basic.core.FeedbackHelper
import app.allever.android.lib.basic.core.toast
import app.allever.android.lib.basic.databinding.ActivityBasicSettingBinding
import app.allever.android.lib.recommend.ui.RecommendDialog
import app.funnyappptech.android.fake.call.util.SystemUtils

class BasicSettingActivity: AbstractActivity(){

    private val rateDialog by lazy {
        RecommendDialog(this,"Cancel")
    }

    //mBinding
    private val mBinding: ActivityBasicSettingBinding by lazy {
        ActivityBasicSettingBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        adaptStatusBar(mBinding.topBar)

        mBinding.ivBack.setOnClickListener {
            finish()
        }

        mBinding.itemAbout.setOnClickListener {
            ActivityHelper.startActivity<BasicAboutActivity>()
        }
        mBinding.itemRateUs.setOnClickListener {
            rateDialog.show()
        }
        mBinding.itemFeedback.setOnClickListener {
            FeedbackHelper.feedback(this)
        }
        mBinding.itemWebside.setOnClickListener {
            SystemUtils.openUrl(this, "https://devallever.github.io/")
        }
    }
}