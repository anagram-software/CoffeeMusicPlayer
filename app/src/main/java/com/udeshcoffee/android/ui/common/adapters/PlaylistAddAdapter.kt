package com.udeshcoffee.android.ui.common.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.interfaces.OnItemSelectListener
import com.udeshcoffee.android.model.Song
import io.reactivex.functions.Consumer
import java.util.ArrayList

/**
 * Created by Udathari on 5/30/2017.
 */

class PlaylistAddAdapter// Provide a suitable constructor (depends on the kind of dataset)
(private val count: TextView) : RecyclerView.Adapter<PlaylistAddAdapter.ViewHolder>(), Consumer<List<Song>> {
    private var mDataset: List<Song>? = null
    private val selected: MutableList<Int>
    private var context: Context? = null
    var listener: OnItemSelectListener? = null

    override fun accept(songs: List<Song>) {
        this.mDataset = songs
        notifyDataSetChanged()
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // each data item is just a string in this case
        var artView: ImageView = itemView.findViewById(R.id.song_art)
        var titleView: TextView = itemView.findViewById(R.id.song_title)
        var artistView: TextView = itemView.findViewById(R.id.song_subtitle)

        init {

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                var isSelected = false
                for (i in selected.indices) {
                    if (position == selected[i]) {
                        isSelected = true
                    }
                }
                if (isSelected) {
                    listener!!.onDeselectItem(position)
                    selected.remove(position)
                    notifyItemChanged(position)
                } else {
                    listener!!.onSelectItem(position)
                    selected.add(position)
                    notifyItemChanged(position)
                }
            }
        }

    }

    init {
        mDataset = ArrayList()
        selected = ArrayList()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistAddAdapter.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val songView = inflater.inflate(R.layout.song, parent, false)

        // Return a new holder instance
        return ViewHolder(songView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: PlaylistAddAdapter.ViewHolder, position: Int) {
        // - get element from your dataset at this position

        val currentItem = mDataset!![position]

        context?.let { currentItem.loadArtwork(it, holder.artView, null) }
        holder.titleView.text = currentItem.title
        holder.artistView.text = currentItem.artistName

        var isSelected = false
        for (i in selected.indices) {
            if (position == selected[i]) {
                isSelected = true
            }
        }
        if (isSelected) {
            val typedValue = TypedValue()
            if (context!!.theme.resolveAttribute(R.attr.artBackground, typedValue, true))
                holder.itemView.setBackgroundColor(typedValue.data)
        } else {
            val typedValue = TypedValue()
            if (context!!.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true))
                holder.itemView.setBackgroundColor(typedValue.data)
        }
        count.text = String.format("%d selected", selected.size)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mDataset!!.size
    }

    fun getItem(pos: Int): Song {
        return mDataset!![pos]
    }

}