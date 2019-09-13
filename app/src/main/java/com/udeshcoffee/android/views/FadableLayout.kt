package com.udeshcoffee.android.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.getColorWithAlpha

/**
 * Created by Udathari on 12/17/2017.
 */
class FadableLayout: ConstraintLayout {

    var backgroundType: Int = 0

    var color: Int = Color.DKGRAY
        private set

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FadableLayout,
                0, 0)

        try {
            backgroundType = a.getInteger(R.styleable.FadableLayout_backgroundType, 0)
        } finally {
            a.recycle()
        }
    }

    private fun setFadeColor(color: Int) {
        this.color = color
        when (backgroundType) {
            0 -> {
                setBackgroundColor(color)
            }
            1 -> {val colors = IntArray(2)

                colors[0] = color.getColorWithAlpha(200)
                colors[1] = color.getColorWithAlpha(75)


                val gradient = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colors)
                background = gradient
            }
        }
    }

    fun fadeToColor(nextColor: Int, duration: Long) {
        val currentColor = this.color

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, nextColor)
        colorAnimation.duration = duration // milliseconds
        colorAnimation.interpolator = FastOutSlowInInterpolator()
        colorAnimation.addUpdateListener { animator -> setFadeColor(animator.animatedValue as Int) }
        colorAnimation.start()
    }

}