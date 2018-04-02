package com.udeshcoffee.android.ui.viewholders

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.remote.genius.Result
import com.udeshcoffee.android.interfaces.OnItemClickListener

/**
 * Created by Udathari on 9/27/2017.
 */

class LyricViewHolder(itemView: View, val listener: OnItemClickListener?) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener{

    // each data item is just a string in this case
    var titleView: TextView = itemView.findViewById(R.id.song_title)
    var subtitleView: TextView = itemView.findViewById(R.id.song_subtitle)

    init {
        itemView.setOnClickListener(this)
    }

    fun bindData(currentItem: Result) {
        titleView.text = currentItem.title_with_featured
        subtitleView.text = currentItem.primary_artist.name
    }


    override fun onClick(v: View) {
        if (listener != null) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }
}