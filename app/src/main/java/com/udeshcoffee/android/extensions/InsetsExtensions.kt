package com.udeshcoffee.android.extensions

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads this view by the system bars and display cutout and consumes those insets.
 *
 * Apps targeting API 35+ are always drawn edge to edge. The View-based UI wasn't designed for
 * that, so this keeps content out from under the status and navigation bars until the Compose UI
 * handles insets per screen.
 */
fun View.padForSystemBars() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}

/** Applies [padForSystemBars] to the activity's root content view. Call after setContentView. */
fun Activity.padContentForSystemBars() {
    (findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return).padForSystemBars()
}
