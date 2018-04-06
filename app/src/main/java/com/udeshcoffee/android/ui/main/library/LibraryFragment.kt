package com.udeshcoffee.android.ui.main.library

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openDrawer
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.ui.common.adapters.FragAdapter
import com.udeshcoffee.android.ui.main.library.nested.AlbumFragment
import com.udeshcoffee.android.ui.main.library.nested.ArtistFragment
import com.udeshcoffee.android.ui.main.library.nested.GenreFragment
import com.udeshcoffee.android.ui.main.library.nested.folder.FolderFragment
import com.udeshcoffee.android.ui.main.library.nested.track.TrackFragment
import com.udeshcoffee.android.utils.PreferenceUtil


/**
* Created by Udathari on 8/26/2017.
*/
class LibraryFragment: Fragment() {

    var actionBar: ActionBar? = null
    lateinit var adapter: FragAdapter
    lateinit var viewPager: ViewPager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        setHasOptionsMenu(true)

        val root = inflater.inflate(R.layout.frag_library, container, false)
        adapter = FragAdapter(childFragmentManager)

        val trackFragment = TrackFragment.create()
        adapter.addFragment(trackFragment, "Songs")

        val albumFragment = AlbumFragment.create()
        adapter.addFragment(albumFragment, "Albums")

        val artistFragment = ArtistFragment.create()
        adapter.addFragment(artistFragment, "Artists")

        val folderFragment = FolderFragment.create()
        adapter.addFragment(folderFragment, "Folders")

        val genreFragment = GenreFragment.create()
        adapter.addFragment(genreFragment, "Genres")

        // Set up player view
        with(root) {
            val toolbar: Toolbar = findViewById(R.id.toolbar)
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.apply {
                title = ""
                setHomeButtonEnabled(true)
            }

            val titleView = findViewById<TextView>(R.id.title_view)
            titleView.setRoundColor(R.color.libAccent)

            viewPager = findViewById(R.id.view_pager)
            viewPager.adapter = adapter
            viewPager.currentItem = PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getString(PreferenceUtil.PREF_LIB_START, "0").toInt()

            val tabs: TabLayout = findViewById(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.library_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> activity?.openDrawer()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun create() = LibraryFragment()
    }
}