package com.udeshcoffee.android.ui.viewholders

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.model.Song

/**
 * Created by Udathari on 9/27/2017.
 */

class SongViewHolder(val context: Context, itemView: View, val listener: OnItemClickListener?, val hasShuffle: Boolean) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

    // each data item is just a string in this case
    var artView: ImageView = itemView.findViewById<ImageView>(R.id.song_art)
    var titleView: TextView = itemView.findViewById<TextView>(R.id.song_title)
    var subtitleView: TextView = itemView.findViewById<TextView>(R.id.song_subtitle)
    var listeningView: TextView = itemView.findViewById<TextView>(R.id.song_listening)

    init {
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    fun bindData(currentItem: Song, isCurrentSong: Boolean) {
        currentItem.loadArtwork(context, artView)
        titleView.text = currentItem.title
        var artist = currentItem.artistName
        val album = currentItem.albumName
        if (album != "<unknown>")
            artist = String.format("%s • %s", artist, album)
        subtitleView.text = artist

        if (isCurrentSong) {
            listeningView.visibility = View.VISIBLE
        } else {
            listeningView.visibility = View.GONE
        }
    }


    override fun onClick(v: View) {
        if (listener != null) {
            var position = adapterPosition
            if (hasShuffle)
                position--
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        if (listener != null) {
            var position = adapterPosition
            if (hasShuffle)
                position--
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemLongClick(position)
            }
        }
        return true
    }
}
