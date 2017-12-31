package com.udeshcoffee.android.recyclerview

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

/**
 * Created by Udathari on 8/27/2017.
 */

class EmptyRecyclerView : FastScrollRecyclerView {

    private var emptyView: View? = null

    private val observer = object : AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    internal fun checkIfEmpty() {
        if (emptyView != null && getAdapter() != null) {
            val emptyViewVisible = adapter.itemCount == 0
            emptyView!!.visibility = if (emptyViewVisible) VISIBLE else GONE
            visibility = if (emptyViewVisible) GONE else VISIBLE
        }
    }

    override fun setAdapter(adapter: Adapter<RecyclerView.ViewHolder>?) {
        val oldAdapter = getAdapter()
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer)
        }
        super.setAdapter(adapter)
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer)
        }

        checkIfEmpty()
    }

    fun setEmptyView(emptyView: View) {
        this.emptyView = emptyView
        checkIfEmpty()
    }
}