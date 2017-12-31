package com.udeshcoffee.android.interfaces

import android.support.v7.widget.RecyclerView

/**
 * Created by Udathari on 9/17/2017.
 */
interface OnDragableItemListener : OnItemClickListener {

    fun onItemDrag(holder: RecyclerView.ViewHolder)

}