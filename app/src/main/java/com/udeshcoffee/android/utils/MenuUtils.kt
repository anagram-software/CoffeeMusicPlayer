package com.udeshcoffee.android.utils

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import com.udeshcoffee.android.R

/**
 * Created by Udathari on 9/9/2017.
 */
fun showSortPopup(context: Context, v: View, type: Int, checkedItem: Int, listener: PopupMenu.OnMenuItemClickListener) {
    val popup = PopupMenu(context, v)

    // This com.udeshcoffee.android.activity implements OnMenuItemClickListener
    popup.setOnMenuItemClickListener(listener)
    when (type) {
        SortManager.SortType.Song -> popup.inflate(R.menu.song_sort)
        SortManager.SortType.Album -> popup.inflate(R.menu.album_sort)
        SortManager.SortType.Artist -> popup.inflate(R.menu.artist_sort)
    }
    popup.menu.getItem(checkedItem).isChecked = true

    popup.show()
}
