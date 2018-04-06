package com.udeshcoffee.android.ui.main.library.nested.track

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import com.udeshcoffee.android.utils.SortManager
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 8/27/2017.
*/

class TrackFragment : Fragment() {

    private val viewModel: TrackViewModel by inject()

    private lateinit var songAdpt: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.frag_linear, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val songView = view.findViewById<EmptyRecyclerView>(R.id.linear_list)
        songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, true)
        songView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        songView.setEmptyView(view.findViewById(R.id.empty_view))
        songView.hasFixedSize()
        songView.setItemViewCacheSize(20)
        songView.isDrawingCacheEnabled = true
        songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
        songView.isNestedScrollingEnabled = false

        songAdpt.listener = object : OnSongItemClickListener {
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
        songView.adapter = songAdpt
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.song_sort, menu)
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
                SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.action_sort_album_name).isChecked = true
                SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
            }
            viewModel.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.songSortAscending }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true

        when (item?.itemId) {
            R.id.action_sort_default -> viewModel.songSortOrder = SortManager.SongSort.DEFAULT
            R.id.action_sort_title -> viewModel.songSortOrder = SortManager.SongSort.NAME
            R.id.action_sort_track -> viewModel.songSortOrder = SortManager.SongSort.TRACK_NUMBER
            R.id.action_sort_duration -> viewModel.songSortOrder = SortManager.SongSort.DURATION
            R.id.action_sort_year -> viewModel.songSortOrder = SortManager.SongSort.YEAR
            R.id.action_sort_date -> viewModel.songSortOrder = SortManager.SongSort.DATE
            R.id.action_sort_album_name -> viewModel.songSortOrder = SortManager.SongSort.ALBUM_NAME
            R.id.action_sort_artist_name -> viewModel.songSortOrder = SortManager.SongSort.ARTIST_NAME
            R.id.action_sort_ascending -> { viewModel.songSortAscending = !item.isChecked }
            else -> sortChanged = false
        }

        if (sortChanged) {
            viewModel.fetchSongs()
            activity?.invalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            currentSongId.observe(this@TrackFragment, Observer {
                it?.let { songAdpt.currentId = it }
            })
            songs.observe(this@TrackFragment, Observer {
                it?.let { songAdpt.accept(it) }
            })

            // Events
            showSongLongDialog.observe(this@TrackFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
        }
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
        fun create() = TrackFragment()
    }
}
