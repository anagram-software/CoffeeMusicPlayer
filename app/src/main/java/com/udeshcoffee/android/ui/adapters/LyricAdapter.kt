package com.udeshcoffee.android.ui.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.remote.genius.Result
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.ui.viewholders.LyricViewHolder
import io.reactivex.functions.Consumer

/**
 * Created by Udesh on 2/18/2017.
 */

class LyricAdapter : RecyclerView.Adapter<LyricViewHolder>(), Consumer<List<Result>> {

    val TAG = "LyricAdapter"

    private var mDataset: List<Result>? = ArrayList()
    private var context: Context? = null
    var listener: OnItemClickListener? = null

    override fun accept(items: List<Result>?) {
        this.mDataset = items
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.lyricitem, parent, false)
        return LyricViewHolder(view, listener) // view holder for header items
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: LyricViewHolder, pos: Int) {
        holder.bindData(mDataset!![pos])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mDataset!!.size
    }

    fun getItem(pos: Int): Result {
        return mDataset!![pos]
    }

}