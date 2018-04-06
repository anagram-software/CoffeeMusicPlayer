package com.udeshcoffee.android.ui.common.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.utils.DopeUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 5/30/2017.
 */

class GenreAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter, Consumer<List<Genre>> {

    private var mDataset: List<Genre>? = null
    private var context: Context? = null
    private var listener: OnItemClickListener? = null

    override fun accept(p0: List<Genre>) {
        this.mDataset = p0
        notifyDataSetChanged()
        Observable.fromIterable(mDataset)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    it.setArtsAndSongCount(context!!.contentResolver)
                }, {}, {
                    notifyDataSetChanged()
                })
    }

    override fun getSectionName(position: Int): String {
        return Character.toString(mDataset!![position].name[0]).toUpperCase()
    }

    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        // each data item is just a string in this case
        var nameView: TextView = itemView.findViewById(R.id.genre_name)
        var noView: TextView = itemView.findViewById(R.id.genre_no)
        var clickInterface: FrameLayout = itemView.findViewById(R.id.click_interface)
        val arts: Array<ImageView> = arrayOf(
                itemView.findViewById<ImageView>(R.id.main_art1),
                itemView.findViewById<ImageView>(R.id.main_art2),
                itemView.findViewById<ImageView>(R.id.main_art3),
                itemView.findViewById<ImageView>(R.id.main_art4),
                itemView.findViewById<ImageView>(R.id.main_art5)
        )

        init {
            clickInterface.setOnClickListener(this)
            clickInterface.setOnLongClickListener(this)
        }

        fun bindData(position: Int) {
            val currentItem = mDataset!![position]
            nameView.text = currentItem.name
            noView.text = currentItem.no?.let { DopeUtil.countToSongCount(it) }
            for (imageView in arts) {
                imageView.visibility = View.GONE
            }
            currentItem.arts?.let {
                val imageViews = Array(it.size, {
                    arts[it]
                })
                context?.let { it1 -> currentItem.loadArtwork(it1, imageViews) }
            }
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener!!.onItemClick(position)
            }
        }

        override fun onLongClick(view: View): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener!!.onItemLongClick(position)
            }
            return true
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    init {
        this.mDataset = ArrayList()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context

        val inflater = LayoutInflater.from(context)

        // Inflate the custom layout
        val itemView = inflater.inflate(R.layout.genre, parent, false)

        // Return a new holder instance
        return ViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        (holder as GenreAdapter.ViewHolder).bindData(position)
    }

    fun getItem(pos: Int): Genre {
        return mDataset!![pos]
    }

    val genreList: ArrayList<Genre>
        get() = mDataset as ArrayList<Genre>

    override fun getItemCount(): Int {
        return mDataset!!.size
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

}