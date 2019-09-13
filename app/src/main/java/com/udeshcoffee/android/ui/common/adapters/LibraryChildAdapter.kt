package com.udeshcoffee.android.ui.common.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.udeshcoffee.android.ui.main.library.nested.AlbumFragment
import com.udeshcoffee.android.ui.main.library.nested.ArtistFragment
import com.udeshcoffee.android.ui.main.library.nested.GenreFragment
import com.udeshcoffee.android.ui.main.library.nested.folder.FolderFragment
import com.udeshcoffee.android.ui.main.library.nested.track.TrackFragment
import java.util.*

/**
 * Created by Udathari on 8/26/2017.
 */
class LibraryChildAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> TrackFragment.create()
            1 -> AlbumFragment.create()
            2 -> ArtistFragment.create()
            3 -> FolderFragment.create()
            else -> GenreFragment.create()
        }
    }

    override fun getCount(): Int {
        return 5
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when(position) {
            0 -> "Songs"
            1 -> "Albums"
            2 -> "Artists"
            3 -> "Folders"
            else ->  "Genre"
        }
    }
}
