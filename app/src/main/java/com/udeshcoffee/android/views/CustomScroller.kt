package com.udeshcoffee.android.views

import android.content.Context
import android.view.animation.Interpolator
import android.widget.Scroller

/**
 * Created by Udathari on 12/17/2017.
 */
class CustomScroller: Scroller {

    private val mDuration = 500

    constructor(context: Context): super(context)

    constructor(context: Context, interpolator: Interpolator): super(context, interpolator)

    constructor(context: Context, interpolator: Interpolator, flywheel: Boolean): super(context, interpolator, flywheel)

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        super.startScroll(startX, startY, dx, dy, mDuration)
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        super.startScroll(startX, startY, dx, dy, mDuration)
    }
}