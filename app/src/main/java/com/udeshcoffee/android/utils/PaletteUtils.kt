package com.udeshcoffee.android.utils

import android.graphics.Bitmap


/**
 * Created by Udathari on 9/15/2017.
 */
fun getPalette(bitmap: Bitmap, callback: (palette: androidx.palette.graphics.Palette) -> Unit) {
    androidx.palette.graphics.Palette.from(bitmap).generate{
        it?.let { it1 -> callback(it1) }
    }
}