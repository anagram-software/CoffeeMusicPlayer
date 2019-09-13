package com.udeshcoffee.android.recyclerview

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

/**
 * Created by Udathari on 9/12/2017.
 */
class MiniGridItemDecor(private val initSpace: Int, private val space: Int): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.right = space

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.left = initSpace
        } else {
            outRect.left = 0
        }
    }
}