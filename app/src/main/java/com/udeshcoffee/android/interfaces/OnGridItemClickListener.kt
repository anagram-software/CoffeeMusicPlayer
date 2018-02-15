package com.udeshcoffee.android.interfaces

import android.view.View

/**
* Created by Udathari on 8/26/2017.
*/
interface OnGridItemClickListener {
    fun onItemClick(position: Int, shareElement: View)
    fun onItemLongClick(position: Int)
    fun onItemOptionClick(position: Int)
}