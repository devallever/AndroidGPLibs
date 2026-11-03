package app.allever.android.lib.basic.core

import android.os.Bundle
import androidx.viewbinding.ViewBinding

abstract class AbstractBindingActivity<VB : ViewBinding> : AbstractActivity() {
    lateinit var mBinding: VB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = inflate()
        setContentView(mBinding.root)
        init()
    }

    abstract fun inflate(): VB
    abstract fun init()
}