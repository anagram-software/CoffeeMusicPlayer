package com.udeshcoffee.android.ui.common.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.RequestManager
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.ui.common.viewholders.ArtistViewHolder
import io.reactivex.functions.Consumer

/**
 * Created by Udathari on 5/30/2017.
 */

class ArtistAdapter(private val dataType: Int, val glide: RequestManager, private val shouldCollectArt: Boolean) :
        androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>(), Consumer<List<Artist>> {

    private var mDataset: List<Artist>? = null
    private var context: Context? = null
    var listener: OnGridItemClickListener? = null

    companion object {
        const val ITEM_TYPE_NORMAL = 0
        const val ITEM_TYPE_MINI = 1
    }

    override fun accept(p0: List<Artist>) {
        this.mDataset = p0
        notifyDataSetChanged()
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        this.mDataset = ArrayList()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        context = parent.context

        val inflater = LayoutInflater.from(context)

        return if (dataType == ITEM_TYPE_NORMAL) {
            val itemView = inflater.inflate(R.layout.griditem, parent, false)
            ArtistViewHolder(context!!, itemView, glide, listener)
        } else {
            val itemView = inflater.inflate(R.layout.griditem_mini, parent, false)
            ArtistViewHolder(context!!, itemView, glide, listener)
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        (holder as ArtistViewHolder).bindData(mDataset!![position], shouldCollectArt)
    }

    fun getItem(pos: Int): Artist {
        return mDataset!![pos]
    }

    override fun getItemCount(): Int {
        return mDataset!!.size
    }

}