package app.allever.android.lib.basic.core

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class AbstractFragment : Fragment() {

    protected val mHandler by lazy {
        HandlerHelper.mainHandler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log(this::class.java.simpleName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        log(this.javaClass.simpleName)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    protected fun setVisibility(view: View, show: Boolean) {
        if (show) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    open fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    open fun onBackPressed() {
        finish()
    }

    protected fun finish() {
        requireActivity().finish()
    }

    protected fun adaptStatusBar(view: View) {
        ViewHelper.setMarginTop(view, DisplayHelper.getStatusBarHeight(requireContext()))
    }
}