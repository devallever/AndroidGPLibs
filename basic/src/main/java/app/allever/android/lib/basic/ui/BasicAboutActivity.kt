package app.allever.android.lib.basic.ui

import android.os.Bundle
import app.allever.android.lib.basic.ProjectConfig
import app.allever.android.lib.basic.core.AbstractActivity
import app.allever.android.lib.basic.core.loadRound
import app.allever.android.lib.basic.databinding.ActivityBasicSplashBinding
import app.allever.android.lib.basic.databinding.ActivityBasicAboutBinding
import app.funnyappptech.android.fake.call.util.SystemUtils
import kotlin.getValue

class BasicAboutActivity : AbstractActivity() {
    //binding by lazy
    private val mBinding by lazy { ActivityBasicAboutBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        adaptStatusBar(mBinding.topBar)

        mBinding.aboutIvLogo.loadRound(ProjectConfig.getAppIcon(), 12f)
        mBinding.aboutAppName.text = ProjectConfig.getAppName()
        mBinding.aboutAppVersion.text = "v${ProjectConfig.getVersionName()}(${ProjectConfig.getVersionCode()})"
        mBinding.aboutRight

        mBinding.ivBack.setOnClickListener { finish() }
        mBinding.aboutPrivacy.setOnClickListener {
            SystemUtils.openUrl(this, ProjectConfig.getPrivacyUrl())
        }
    }
}