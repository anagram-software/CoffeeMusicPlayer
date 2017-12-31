package com.udeshcoffee.android.ui.main.library.nested.track

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.openSongLongDialog
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.utils.SortManager

/**
 * Created by Udathari on 8/27/2017.
 */

class TrackFragment() : Fragment(), TrackContract.View{

    val TAG = "TrackFragment"

    override var presenter: TrackContract.Presenter? = null

    internal var songAdpt: SongAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.frag_linear, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val songView = view.findViewById<EmptyRecyclerView>(R.id.linear_list)
        // specify an adapter (see also next example)
        songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, true)
        songView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        songView.setEmptyView(view.findViewById(R.id.empty_view))
        songView.hasFixedSize()
        songView.setItemViewCacheSize(20)
        songView.isDrawingCacheEnabled = true
        songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
        songView.isNestedScrollingEnabled = false

        songAdpt?.listener = object : OnSongItemClickListener {
            override fun onItemClick(position: Int) {
                songAdpt?.let { presenter?.itemClicked(position, it.songList) }
            }

            override fun onItemLongClick(position: Int) {
                songAdpt?.let { presenter?.itemLongClicked(it.songList[position]) }
            }

            override fun onShuffleClick() {
                songAdpt?.let { presenter?.shuffleClicked(it.songList) }
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
            when (presenter?.sortOrder) {
                SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
                SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
                SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
                SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
                SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
                SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
                SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.action_sort_album_name).isChecked = true
                SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
            }
            presenter?.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.sortAscending }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true

        when (item?.itemId) {
            R.id.action_sort_default -> presenter?.sortOrder = SortManager.SongSort.DEFAULT
            R.id.action_sort_title -> presenter?.sortOrder = SortManager.SongSort.NAME
            R.id.action_sort_track -> presenter?.sortOrder = SortManager.SongSort.TRACK_NUMBER
            R.id.action_sort_duration -> presenter?.sortOrder = SortManager.SongSort.DURATION
            R.id.action_sort_year -> presenter?.sortOrder = SortManager.SongSort.YEAR
            R.id.action_sort_date -> presenter?.sortOrder = SortManager.SongSort.DATE
            R.id.action_sort_album_name -> presenter?.sortOrder = SortManager.SongSort.ALBUM_NAME
            R.id.action_sort_artist_name -> presenter?.sortOrder = SortManager.SongSort.ARTIST_NAME
            R.id.action_sort_ascending -> { presenter?.sortAscending = !item.isChecked }
            else -> sortChanged = false
        }

        if (sortChanged) {
            presenter?.fetchData()
            activity?.supportInvalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        presenter?.start()
    }

    override fun onPause() {
        super.onPause()
        presenter?.stop()
    }

    override fun populateItems(items: List<Song>) {
        songAdpt?.accept(items)
    }

    override fun setCurrentSong(id: Long) {
        songAdpt?.currentId = id
    }

    override fun showAddToPlaylistDialog(songs: ArrayList<Song>) {}

    override fun showFavoritesToast(isFavorite: Boolean) {}

    override fun showSongLongDialog(song: Song) {
        openSongLongDialog(song)
    }
}
