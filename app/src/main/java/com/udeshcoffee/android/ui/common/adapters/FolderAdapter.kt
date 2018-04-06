package com.udeshcoffee.android.ui.common.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.viewholders.SongViewHolder

/**
 * Created by Udathari on 9/26/2017.
 */
class FolderAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var context: Context
    var folderClickListener: OnItemClickListener? = null
    var songClickListener: OnItemClickListener? = null
    var folderSet: ArrayList<Folder> = ArrayList()
    var songSet: ArrayList<Song> = ArrayList()
    var currentId: Long? = null

    private val ITEM_TYPE_SONG = 0
    private val ITEM_TYPE_FOLDER = 1

    fun accept(songs: List<Song>? = null, folders: List<Folder>? = null) {
        songs?.let {
            songSet = it as ArrayList<Song>
            notifyDataSetChanged()
        }
        folders?.let {
            folderSet = it as ArrayList<Folder>
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position >= folderSet.size) {
            ITEM_TYPE_SONG
        } else {
            ITEM_TYPE_FOLDER
        }
    }

    internal inner class FolderViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        private val titleView: TextView = itemView.findViewById(R.id.folder_title)
        private val countView: TextView = itemView.findViewById(R.id.folder_no)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bindView(pos: Int) {
            val current = folderSet[pos]
            titleView.text = current.title
            val count = current.songCount
            if (count == 1) {
                countView.text = String.format("%s song", count.toString())
            } else {
                countView.text = String.format("%s songs", count.toString())
            }
        }

        override fun onClick(view: View) {
            if (folderClickListener != null) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    folderClickListener?.onItemClick(position)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (folderClickListener != null) {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    folderClickListener?.onItemLongClick(position)
                }
                return true
            }
            return false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ITEM_TYPE_SONG) {
            SongViewHolder(context, inflater.inflate(R.layout.song, parent, false), songClickListener, false)
        } else {
            FolderViewHolder(inflater.inflate(R.layout.folder, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemType = getItemViewType(position)

        if (itemType == ITEM_TYPE_SONG) {
            val temp = songSet[position - folderSet.size]
            (holder as SongViewHolder).bindData(temp, this.currentId == temp.id)
        } else if (itemType == ITEM_TYPE_FOLDER) {
            (holder as FolderViewHolder).bindView(position)
        }
    }

    override fun getItemCount(): Int = folderSet.size + songSet.size

}