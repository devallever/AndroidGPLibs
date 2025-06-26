package com.allever.android.lib.admob

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import com.allever.android.lib.admob.databinding.DialogDevBinding

class DevModeDialog(context: Context, val confirm: () -> Unit = {}) :
    Dialog(context, com.google.android.material.R.style.Theme_Design_BottomSheetDialog) {

    private val mBinding: DialogDevBinding

    private val TAG = this::class.java.simpleName

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

        mBinding = DialogDevBinding.inflate(layoutInflater)

        setContentView(mBinding.root)

        initView()
    }

    private fun initView() {
        mBinding.apply {
            btnConfirm.setOnClickListener {
                val pwd = etPwd.text.toString()
                if (pwd == "5400") {
                    confirm.invoke()
                    dismiss()
                    return@setOnClickListener
                }
            }
        }
    }

}