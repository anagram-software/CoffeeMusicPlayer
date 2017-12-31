package com.udeshcoffee.android.interfaces

/**
 * Created by Udathari on 9/17/2017.
 */

interface ItemTouchHelperAdapter {

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean

    fun onItemDismiss(position: Int)
}