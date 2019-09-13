package com.udeshcoffee.android.ui.main.library

import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openDrawer
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.ui.common.adapters.LibraryChildAdapter
import com.udeshcoffee.android.utils.PreferenceUtil

/**
* Created by Udathari on 8/26/2017.
*/
class LibraryFragment: Fragment() {

    var actionBar: ActionBar? = null
    lateinit var adapter: LibraryChildAdapter
    private lateinit var viewPager: androidx.viewpager.widget.ViewPager

    init {
        enterTransition = Fade()
        exitTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        setHasOptionsMenu(true)

        val root = inflater.inflate(R.layout.frag_library, container, false)
        adapter = LibraryChildAdapter(childFragmentManager)

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
                    .getString(PreferenceUtil.PREF_LIB_START, "0")?.toInt() ?: 0

            val tabs: com.google.android.material.tabs.TabLayout = findViewById(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity?.openDrawer()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        fun create() = LibraryFragment()
    }
}