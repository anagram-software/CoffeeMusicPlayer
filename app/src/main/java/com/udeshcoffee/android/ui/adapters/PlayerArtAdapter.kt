package com.udeshcoffee.android.ui.adapters

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.model.Song
import io.reactivex.functions.Consumer

/**
 * Created by Udathari on 9/24/2017.
 */
class PlayerArtAdapter(val context: Context): PagerAdapter(), Consumer<List<Song>> {

    val TAG = "PlayerArtAdapter"

    private var dataSet: ArrayList<Song>? = null
    var isNotifyDatasetChangedCalled = false
        private set

    init {
        dataSet = ArrayList()
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater =  LayoutInflater.from(context)

        val itemView = inflater.inflate(R.layout.player_art_page, container, false)

        val imageView = itemView.findViewById<ImageView>(R.id.player_art)

        dataSet?.get(position)?.loadArtwork(context, imageView)

        container.addView(itemView)

        itemView.tag = position

        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int = dataSet!!.size

    override fun accept(t: List<Song>?) {Log.d(TAG, "accept: ${t?.size}")
        isNotifyDatasetChangedCalled = false
        dataSet = t as ArrayList<Song>
        notifyDataSetChanged()
        if (t.isNotEmpty())
            isNotifyDatasetChangedCalled = true
    }
}