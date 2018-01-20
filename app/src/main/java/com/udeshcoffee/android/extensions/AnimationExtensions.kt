package com.udeshcoffee.android.extensions

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View

/**
 * Created by Udathari on 9/17/2017.
 */
fun View.fadeOut(duration: Long, completed: (() -> Unit)? = null) {
    val animator = ValueAnimator.ofFloat(1.0f, 0.0f)
    animator.addUpdateListener { animation ->
        this.alpha = animation.animatedValue as Float
    }
    animator.duration = duration
    animator.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) {

        }

        override fun onAnimationEnd(animator: Animator) {
            this@fadeOut.visibility = View.INVISIBLE
            completed?.let { it() }
        }

        override fun onAnimationCancel(animator: Animator) {

        }

        override fun onAnimationRepeat(animator: Animator) {

        }
    })
    animator.start()
}

fun View.fadeIn(duration: Long, completed: (() -> Unit)? = null) {
    val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
    animator.addUpdateListener { animation ->
        this.alpha = animation.animatedValue as Float
    }
    animator.duration = duration
    animator.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) {
            this@fadeIn.visibility = View.VISIBLE
        }

        override fun onAnimationEnd(animator: Animator) {
            completed?.let { it() }
        }

        override fun onAnimationCancel(animator: Animator) {

        }

        override fun onAnimationRepeat(animator: Animator) {

        }
    })
    animator.start()
}