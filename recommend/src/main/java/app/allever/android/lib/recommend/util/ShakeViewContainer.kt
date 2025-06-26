package app.allever.android.lib.recommend.util

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator

class ShakeViewContainer(private val view: View) {

    private var shakeAnimation: ObjectAnimator? = null

    init {
        shakeAnimation =
            ObjectAnimator.ofFloat(view, "rotation", 0f, 10f, 0f, -10f,  0f)
        shakeAnimation?.repeatCount = ValueAnimator.INFINITE
        shakeAnimation?.duration = 600
        shakeAnimation?.interpolator = LinearInterpolator()
    }

    fun start() {
        shakeAnimation?.start()
    }

    fun stop() {
        shakeAnimation?.cancel()
    }
}