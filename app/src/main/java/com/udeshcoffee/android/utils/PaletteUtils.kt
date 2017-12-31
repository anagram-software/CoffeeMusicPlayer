package com.udeshcoffee.android.utils

import android.graphics.Bitmap
import android.support.v7.graphics.Palette


/**
 * Created by Udathari on 9/15/2017.
 */
fun getPalette(bitmap: Bitmap, callback: (palette: Palette) -> Unit) {
    Palette.from(bitmap).generate{
        callback(it)
    }
}