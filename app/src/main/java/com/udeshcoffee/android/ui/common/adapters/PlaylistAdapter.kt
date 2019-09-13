package com.udeshcoffee.android.ui.common.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.ui.common.viewholders.PlaylistViewHolder
import io.reactivex.functions.Consumer

/**
 * Created by Udathari on 5/30/2017.
 */

class PlaylistAdapter() : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>(), Consumer<List<Playlist>> {

    private var dataSet: List<Playlist>? = null
    private var context: Context? = null
    var listener: OnItemClickListener? = null

    override fun accept(p0: List<Playlist>) {
        this.dataSet = p0
        notifyDataSetChanged()
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        this.dataSet = ArrayList()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        context = parent.context

        val inflater = LayoutInflater.from(context)

        val itemView = inflater.inflate(R.layout.playlist, parent, false)
        return PlaylistViewHolder(context!!, itemView, listener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        dataSet?.get(position)?.let { (holder as PlaylistViewHolder).bindData(it) }
    }

    fun getItem(pos: Int): Playlist {
        return dataSet!![pos]
    }

    val playlist: ArrayList<Playlist>
        get() = dataSet as ArrayList<Playlist>

    override fun getItemCount(): Int {
        return dataSet!!.size
    }

}