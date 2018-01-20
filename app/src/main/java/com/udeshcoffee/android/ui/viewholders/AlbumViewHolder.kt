package com.udeshcoffee.android.ui.viewholders

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.utils.DopeUtil

/**
 * Created by Udathari on 9/27/2017.
 */

class AlbumViewHolder(val context: Context, itemView: View, val listener: OnGridItemClickListener?) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
    // each data item is just a string in this case
    var artView: ImageView = itemView.findViewById<View>(R.id.grid_art) as ImageView
    var titleView: TextView = itemView.findViewById<View>(R.id.grid_title) as TextView
    var subtitleView: TextView = itemView.findViewById<View>(R.id.grid_subtitle) as TextView

    init {

        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    fun bindData(currentItem: Album) {
        currentItem.loadArtwork(context, artView)
        ViewCompat.setTransitionName(artView, "$position")
        titleView.text = currentItem.title

        subtitleView.text = DopeUtil.countToSongCount(currentItem.songCount)
    }

    override fun onClick(view: View) {
        val position = adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            listener?.onItemClick(position, artView)
        }
    }

    override fun onLongClick(view: View): Boolean {
        val position = adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            listener?.onItemLongClick(position)
        }
        return true
    }
}
