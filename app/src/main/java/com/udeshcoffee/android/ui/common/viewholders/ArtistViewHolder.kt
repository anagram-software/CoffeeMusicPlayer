package com.udeshcoffee.android.ui.common.viewholders

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.utils.DopeUtil



/**
 * Created by Udathari on 10/12/2017.
 */
class ArtistViewHolder(val context: Context, itemView: View, val glide: RequestManager, val listener: OnGridItemClickListener?) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
    // each data item is just a string in this case
    private var artView: ImageView = itemView.findViewById(R.id.grid_art)
    private var titleView: TextView = itemView.findViewById(R.id.grid_title)
    private var subtitleView: TextView = itemView.findViewById(R.id.grid_subtitle)
    private var optionView: ImageButton = itemView.findViewById(R.id.action_optional_play)

    init {
        optionView.setOnClickListener {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener?.onItemOptionClick(position)
            }
        }
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    fun bindData(currentItem: Artist, shouldCollectArt: Boolean) {
        context.let { currentItem.loadArtwork(it, glide, artView, shouldCollectArt) }
        titleView.text = currentItem.name
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