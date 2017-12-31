package com.udeshcoffee.android.ui.main.favorites

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import com.udeshcoffee.android.*
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.utils.SortManager

/**
 * Created by Udathari on 9/29/2017.
 */
class FavoritesFragment: Fragment(), FavoritesContract.View {

    override var presenter: FavoritesContract.Presenter? = null

    lateinit var songAdapter: SongAdapter
    private var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_favorites, container, false)
    }

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

            val songView = findViewById<EmptyRecyclerView>(R.id.list)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songAdapter = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, true)
            songView.adapter = songAdapter
            songView.layoutManager = LinearLayoutManager(context)
            songAdapter.listener = object : OnSongItemClickListener {
                override fun onItemClick(position: Int) {
                    songAdapter.songList.let { presenter?.itemClicked(position, it) }
                }

                override fun onItemLongClick(position: Int) {
                    songAdapter.getItem(position).let { presenter?.itemLongClicked(it) }
                }

                override fun onShuffleClick() {
                    songAdapter.songList.let { presenter?.shuffleClicked(it) }
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
            when (presenter?.sortOrder) {
                SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
                SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
                SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
                SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
                SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
                SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
                SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
            }
            presenter?.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.sortAscending }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true

        if (item?.groupId == R.id.sort_group) {
            when (item.itemId) {
                R.id.action_sort_default -> presenter?.sortOrder = SortManager.SongSort.DEFAULT
                R.id.action_sort_title -> presenter?.sortOrder = SortManager.SongSort.NAME
                R.id.action_sort_track -> presenter?.sortOrder = SortManager.SongSort.TRACK_NUMBER
                R.id.action_sort_duration -> presenter?.sortOrder = SortManager.SongSort.DURATION
                R.id.action_sort_year -> presenter?.sortOrder = SortManager.SongSort.YEAR
                R.id.action_sort_date -> presenter?.sortOrder = SortManager.SongSort.DATE
                R.id.action_sort_artist_name -> presenter?.sortOrder = SortManager.SongSort.ARTIST_NAME
                R.id.action_sort_ascending -> presenter?.sortAscending = !item.isChecked
            }
        } else sortChanged = false

        when (item?.itemId) {
            android.R.id.home -> activity?.openDrawer()
            R.id.action_play -> presenter?.playClick(songAdapter.songList)
            R.id.action_play_next -> presenter?.playNextClick(songAdapter.songList)
            R.id.action_queue -> presenter?.queueClick(songAdapter.songList)
            R.id.action_add_to_playlist -> presenter?.addToPlaylistClick(songAdapter.songList)
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
        songAdapter.accept(items)
    }

    override fun showSongLongDialog(song: Song) {
        openSongLongDialog(song)
    }


    override fun showAddToPlaylistDialog(songs: ArrayList<Song>) {
        openAddToPlaylistDialog(songs)
    }

    override fun showFavoritesToast(isFavorite: Boolean) {}


    override fun setCurrentSong(id: Long) {
        songAdapter.currentId = id
    }

}