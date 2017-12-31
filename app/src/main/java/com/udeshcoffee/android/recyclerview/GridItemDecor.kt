package com.udeshcoffee.android.recyclerview

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by Udathari on 9/12/2017.
 */
class GridItemDecor(private val space: Int): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        outRect.left = space
        outRect.right = space
        outRect.bottom = space * 2

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0 || parent.getChildLayoutPosition(view) == 1) {
            outRect.top = space * 2
        } else {
            outRect.top = 0
        }
    }
}