package com.udeshcoffee.android.ui.main.favorites

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openDrawer
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import com.udeshcoffee.android.utils.SortManager
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 9/29/2017.
*/
class FavoritesFragment: Fragment() {

    val viewModel: FavoritesViewModel by inject()

    private lateinit var songAdapter: SongAdapter
    private lateinit var toolbarSongs: Toolbar
    private var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_favorites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        view.apply {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.apply {
                title = ""
                setHomeButtonEnabled(true)
            }

            val titleView = findViewById<TextView>(R.id.title_view)
            titleView.text = getString(R.string.info_favorites).toUpperCase()
            titleView.setRoundColor(R.color.favAccent)

            toolbarSongs = findViewById(R.id.toolbar_songs)
            toolbarSongs.inflateMenu(R.menu.song_containing_menu)
            toolbarSongs.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_play -> {
                        viewModel.playClick()
                        true
                    }
                    R.id.action_queue -> {
                        viewModel.queueClick()
                        true
                    }
                    else -> false
                }
            }

            val songView = findViewById<EmptyRecyclerView>(R.id.list)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songAdapter = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, true)
            songView.adapter = songAdapter
            songView.layoutManager = LinearLayoutManager(context)
            songAdapter.listener = object : OnSongItemClickListener {
                override fun onItemClick(position: Int) {
                    viewModel.songItemClicked(position)
                }

                override fun onItemLongClick(position: Int) {
                    viewModel.songItemLongClicked(position)
                }

                override fun onShuffleClick() {
                    viewModel.shuffleClicked()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.favorites_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        if (menu != null) {
            when (viewModel.songSortOrder) {
                SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
                SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
                SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
                SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
                SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
                SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
                SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
            }
            viewModel.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.songSortAscending }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true

        if (item?.groupId == R.id.sort_group) {
            when (item.itemId) {
                R.id.action_sort_default -> viewModel.songSortOrder = SortManager.SongSort.DEFAULT
                R.id.action_sort_title -> viewModel.songSortOrder = SortManager.SongSort.NAME
                R.id.action_sort_track -> viewModel.songSortOrder = SortManager.SongSort.TRACK_NUMBER
                R.id.action_sort_duration -> viewModel.songSortOrder = SortManager.SongSort.DURATION
                R.id.action_sort_year -> viewModel.songSortOrder = SortManager.SongSort.YEAR
                R.id.action_sort_date -> viewModel.songSortOrder = SortManager.SongSort.DATE
                R.id.action_sort_artist_name -> viewModel.songSortOrder = SortManager.SongSort.ARTIST_NAME
                R.id.action_sort_ascending -> viewModel.songSortAscending = !item.isChecked
            }
        } else sortChanged = false

        when (item?.itemId) {
            android.R.id.home -> activity?.openDrawer()
            R.id.action_play -> viewModel.playClick()
            R.id.action_play_next -> viewModel.playNextClick()
            R.id.action_queue -> viewModel.queueClick()
            R.id.action_add_to_playlist -> viewModel.addToPlaylistClick()
        }

        if (sortChanged) {
            viewModel.fetchSongs()
            activity?.invalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    companion object {
        fun create(): FavoritesFragment = FavoritesFragment()
    }

}