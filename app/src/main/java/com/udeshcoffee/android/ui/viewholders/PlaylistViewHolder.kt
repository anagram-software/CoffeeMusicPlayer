package com.udeshcoffee.android.ui.viewholders

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.utils.DopeUtil

/**
 * Created by Udathari on 9/27/2017.
 */

class PlaylistViewHolder(val context: Context, itemView: View, val listener: OnItemClickListener?) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
    // each data item is just a string in this case
    var titleView: TextView = itemView.findViewById<View>(R.id.playlist_title) as TextView
    var subtitleView: TextView = itemView.findViewById<View>(R.id.playlist_song_count) as TextView

    init {
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    fun bindData(currentItem: Playlist) {
        titleView.text = currentItem.title
        if (currentItem.songCount == null)
            subtitleView.text = ""
        else
            subtitleView.text = DopeUtil.countToSongCount(currentItem.songCount!!)
    }

    override fun onClick(view: View) {
        val position = adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            listener?.onItemClick(position)
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