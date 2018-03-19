package com.udeshcoffee.android.ui.adapters

import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.ItemTouchHelperAdapter
import com.udeshcoffee.android.interfaces.OnDragableItemListener
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.viewholders.SongViewHolder
import io.reactivex.functions.Consumer
import java.util.*

/**
* Created by Udathari on 9/17/2017.
*/
class DragableAdapter(var dragable: Boolean):
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), Consumer<List<Song>>, ItemTouchHelperAdapter {

    var dataset = ArrayList<Song>()
    var context: Context? = null
    var currentSong: Int = -1
    var currentId: Long = 0
        set(value) {
            field = value
            checkCurrentSong()
        }

    var listener: OnDragableItemListener? = null

    override fun accept(p0: List<Song>) {
        this.dataset = p0 as ArrayList<Song>
        notifyDataSetChanged()
    }

    private fun checkCurrentSong() {
        if (currentId.toInt() != -1) {
            for (e in dataset.indices) {
                if (dataset[e].id == currentId) {
                    val prevSong = currentSong
                    if (prevSong != -1) {
                        notifyItemChanged(prevSong)
                    }
                    currentSong = e
                    notifyItemChanged(e)
                    break
                }
            }
        } else {
            currentSong = -1
            notifyDataSetChanged()
        }
    }

    inner class DragViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener{
        // each data item is just a string in this case
        private var artView: ImageView = itemView.findViewById(R.id.dragable_art)
        private var dragView: ImageView = itemView.findViewById(R.id.dragable_drag)
        private var titleView: TextView = itemView.findViewById(R.id.dragable_title)
        private var subtitleView: TextView = itemView.findViewById(R.id.dragable_subtitle)
        private var listeningView: TextView = itemView.findViewById(R.id.dragable_listening)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bindData(position: Int, holder: DragViewHolder?) {
            val currentItem = dataset[position]
            currentItem.loadArtwork(context!!, artView)
            titleView.text = currentItem.title
            var artist = currentItem.artistName
            val album = currentItem.albumName
            if (album != "<unknown>")
                artist = String.format("%s • %s", artist, album)
            subtitleView.text = artist

            if (currentSong == position) {
                listeningView.visibility = View.VISIBLE
            } else {
                listeningView.visibility = View.GONE
            }
            dragView.setOnTouchListener { _, motionEvent ->
                if (MotionEventCompat.getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                    listener?.onItemDrag(holder as RecyclerView.ViewHolder)
                }
                true
            }
        }

        override fun onClick(v: View) {
            if (listener != null) {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener!!.onItemClick(adapterPosition)
                }
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            if (listener != null) {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener!!.onItemLongClick(adapterPosition)
                    return true
                }
            }
            return false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder{
        context = parent.context
        val inflater = LayoutInflater.from(context)
        return if (dragable) {
            val itemView = inflater.inflate(R.layout.dragable_item, parent, false)
            DragViewHolder(itemView)
        } else {
            val itemView = inflater.inflate(R.layout.song, parent, false)
            SongViewHolder(context!!, itemView, listener, false)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (dragable)
            (holder as DragViewHolder?)?.bindData(position, holder)
        else
            (holder as SongViewHolder?)?.bindData(dataset[position], currentSong == position)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dataset, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dataset, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        dataset.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int = dataset.size
}