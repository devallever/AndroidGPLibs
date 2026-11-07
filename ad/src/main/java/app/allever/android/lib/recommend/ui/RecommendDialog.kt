package app.allever.android.lib.recommend.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import app.allever.android.lib.ad.R
import app.allever.android.lib.recommend.data.Recommend
import app.allever.android.lib.recommend.data.RecommendHelper
import app.allever.android.lib.recommend.data.RecommendId
import app.allever.android.lib.ad.databinding.DialogRecommendBinding
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.ShakeViewContainer
import app.allever.android.lib.recommend.util.loadRound
import app.allever.android.lib.recommend.util.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecommendDialog(context: Context, val cancelText: String = "", val giftedCount: Int = 0, val exit: () -> Unit = {}) :
    Dialog(context, com.google.android.material.R.style.Theme_Design_BottomSheetDialog) {

    private val mBinding: DialogRecommendBinding

    private val TAG = this::class.java.simpleName

    private lateinit var mRecommend: Recommend
    private lateinit var mShakeViewContainer: ShakeViewContainer

    init {
        //宽度占满屏
        val window = window
        window?.setGravity(Gravity.CENTER)
        // 把 DecorView 的默认 padding 取消，同时 DecorView 的默认大小也会取消
        window?.decorView?.setPadding(0, 0, 0, 0)
        val layoutParams = window?.attributes
        // 设置宽度
//        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = layoutParams
        // 给 DecorView 设置背景颜色，很重要，不然导致 Dialog 内容显示不全，有一部分内容会充当 padding，上面例子有举出
        window?.decorView?.setBackgroundColor(Color.TRANSPARENT)

        mBinding = DialogRecommendBinding.inflate(layoutInflater)

        setContentView(mBinding.root)

        initView()

        mShakeViewContainer = ShakeViewContainer(mBinding.recommendView)

    }

    private fun initView() {
        if (cancelText.isNotEmpty()) {
            mBinding.tvExit.text = cancelText
        }

        mBinding.apply {
            tvExit.setOnClickListener {
                dismissAndDestroy()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200L)
                    exit.invoke()
                }
            }

            tvRateUs.setOnClickListener {
                val stars = ratingBar.rating
                if (stars < 5) {
                    toast("Thank you for your support!")
                    dismiss()
                } else {
                    //jump GP
                    goGpForRating(context)
                    dismiss()
                }
            }

            ivRecommendApp.setOnClickListener {
                if (RecommendUtils.isAppExist(context, mRecommend.pkg)) {
                    RecommendUtils.openApp(mRecommend.pkg)
                } else {
                    RecommendUtils.jumpToAppStore(mRecommend.pkg)
                }
            }
        }
    }

    override fun show() {
        super.show()
        mRecommend = RecommendHelper.getRecommendApp()
        mBinding.ivRecommendApp.loadRound(
            RecommendId.getIconRes(mRecommend.id),
            RecommendUtils.dip2px(4).toFloat()
        )
        mBinding.ratingBar.rating = 5f
        mBinding.tvRecommend.text = mRecommend.desc

        // 赠送模式：显示顶部提示，中间改为好评引导
        if (giftedCount > 0) {
            mBinding.tvGiftHint.visibility = android.view.View.VISIBLE
            mBinding.tvGiftHint.text = context.getString(R.string.rc_gift_hint, giftedCount)
            mBinding.tvContent.text = context.getString(R.string.rc_gift_rate_prompt)
        }

        mShakeViewContainer.start()
    }

    private fun dismissAndDestroy() {
        super.dismiss()
    }

    override fun dismiss() {
        super.dismiss()
        mShakeViewContainer.stop()
    }

    private fun goGpForRating(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setPackage("com.android.vending")
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            toast("can not open market")
        }
    }
}