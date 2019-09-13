package com.udeshcoffee.android.interfaces

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Udathari on 9/17/2017.
 */
interface OnDragableItemListener : OnItemClickListener {

    fun onItemDrag(holder: RecyclerView.ViewHolder)

}