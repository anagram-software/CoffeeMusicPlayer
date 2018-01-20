package com.udeshcoffee.android.ui.main.detail.genredetail

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.navigateToDetail
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.utils.SortManager
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 9/12/2017.
*/
class GenreDetailFragment: Fragment(), GenreDetailContract.View {

    override val presenter: GenreDetailContract.Presenter by inject()

    var genreId: Long = -1

    lateinit var songAdpt: SongAdapter
    lateinit var albumAdpt: AlbumAdapter

    private lateinit var toolbarSongs: Toolbar
    private lateinit var toolbarAlbums: Toolbar

    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_genre_detail, container, false)

        genreId = arguments!!.getLong(ARGUMENT_ID)
        val name = arguments!!.getString(ARGUMENT_NAME)

        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@GenreDetailFragment.actionBar = supportActionBar
            }

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = name
            }
            setHasOptionsMenu(true)

            toolbarAlbums = findViewById(R.id.toolbar_albums)

            toolbarSongs = findViewById(R.id.toolbar_songs)
            toolbarSongs.inflateMenu(R.menu.song_containing_menu)
            toolbarSongs.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_play -> {
                        presenter.playClick(songAdpt.songList)
                        true
                    }
                    R.id.action_queue -> {
                        presenter.queueClick(songAdpt.songList)
                        true
                    }
                    else -> false
                }
            }

            val songView = findViewById<EmptyRecyclerView>(R.id.genre_song_recycler_view)
            songView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isDrawingCacheEnabled = true
            songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            songView.isNestedScrollingEnabled = false

            // specify an adapter (see also next example)
            songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, true)
            songAdpt.listener = object : OnSongItemClickListener {
                override fun onItemClick(position: Int) {
                    presenter.itemClicked(position, songAdpt.songList)
                }

                override fun onItemLongClick(position: Int) {
                    presenter.itemLongClicked(songAdpt.songList[position])
                }

                override fun onShuffleClick() {
                    presenter.shuffleClicked(songAdpt.songList)
                }
            }
            songView.adapter = songAdpt


            // Album View
            val albumView = findViewById<RecyclerView>(R.id.genre_album_recycler_view)
            albumView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            albumView.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.mini_grid_spacing)))
            // specify an adapter (see also next example)
            albumAdpt = AlbumAdapter(AlbumAdapter.ITEM_TYPE_MINI)
            albumAdpt.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    presenter.albumItemClicked(position)
                }

                override fun onItemOptionClick() {
                }

                override fun onItemLongClick(position: Int) {
                    presenter.albumItemLongClicked(albumAdpt.getItem(position))
                }
            }
            albumView.adapter = albumAdpt
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.genre_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        if (menu != null) {
            when (presenter.sortOrder) {
                SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
                SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
                SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
                SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
                SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
                SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
                SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.action_sort_album_name).isChecked = true
            }
            when (presenter.albumSortOrder) {
                SortManager.AlbumSort.DEFAULT -> menu.findItem(R.id.action_sort_album_default).isChecked = true
                SortManager.AlbumSort.NAME -> menu.findItem(R.id.action_sort_album_title).isChecked = true
                SortManager.AlbumSort.YEAR -> menu.findItem(R.id.action_sort_album_year).isChecked = true
            }
            presenter.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.sortAscending
                menu.findItem(R.id.action_sort_album_ascending).isChecked = it.albumSortAscending }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true
        var albumSortChanged = true

        if (item?.groupId == R.id.sort_album) {
            when (item.itemId) {
                R.id.action_sort_album_default -> presenter.albumSortOrder = SortManager.AlbumSort.DEFAULT
                R.id.action_sort_album_title -> presenter.albumSortOrder = SortManager.AlbumSort.NAME
                R.id.action_sort_album_year -> presenter.albumSortOrder = SortManager.AlbumSort.YEAR
                R.id.action_sort_album_ascending -> presenter.albumSortAscending = !item.isChecked
            }
        } else albumSortChanged = false

        if (item?.groupId == R.id.sort_song) {
            when (item.itemId) {
                R.id.action_sort_default -> presenter.sortOrder = SortManager.SongSort.DEFAULT
                R.id.action_sort_title -> presenter.sortOrder = SortManager.SongSort.NAME
                R.id.action_sort_track -> presenter.sortOrder = SortManager.SongSort.TRACK_NUMBER
                R.id.action_sort_duration -> presenter.sortOrder = SortManager.SongSort.DURATION
                R.id.action_sort_year -> presenter.sortOrder = SortManager.SongSort.YEAR
                R.id.action_sort_date -> presenter.sortOrder = SortManager.SongSort.DATE
                R.id.action_sort_album_name -> presenter.sortOrder = SortManager.SongSort.ALBUM_NAME
                R.id.action_sort_ascending -> presenter.sortAscending = !item.isChecked
            }
        } else sortChanged = false

        when (item?.itemId) {
            android.R.id.home -> activity?.onBackPressed()
            R.id.action_play -> presenter.playClick(songAdpt.songList)
            R.id.action_play_next -> presenter.playNextClick(songAdpt.songList)
            R.id.action_queue -> presenter.queueClick(songAdpt.songList)
            R.id.action_add_to_favorites -> presenter.addToFavoritesClick(songAdpt.songList)
            R.id.action_add_to_playlist -> presenter.addToPlaylistClick(songAdpt.songList)
        }

        if (albumSortChanged) {
            presenter.fetchAlbums()
            activity?.invalidateOptionsMenu()
        }

        if (sortChanged) {
            presenter.fetchSongs()
            activity?.invalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        presenter.genreId = genreId
        presenter.view = this
        presenter.start()
    }

    override fun onPause() {
        super.onPause()
        presenter.stop()
    }

    override fun populateItems(items: List<Song>) {
        songAdpt.accept(items)
        if (items.isEmpty()) {
            toolbarSongs.visibility = View.GONE
        }
    }

    override fun populateAlbumItems(items: List<Album>) {
        albumAdpt.accept(items)
        if (items.isEmpty()) {
            toolbarAlbums.visibility = View.GONE
        }
    }

    override fun setCurrentSong(id: Long) {
        songAdpt.currentId = id
    }

    override fun showAlbum(position: Int) {
        val detail = albumAdpt.getItem(position)
        fragmentManager?.navigateToDetail(detail)
    }

    override fun showSongLongDialog(song: Song) {
        openSongLongDialog(song)
    }

    override fun showAddToPlaylistDialog(songs: ArrayList<Song>) {
        openAddToPlaylistDialog(songs)
    }

    override fun showFavoritesToast(isFavorite: Boolean) {
        if (isFavorite)
            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
    }

    override fun showCollectionLongDialog(title: String, songs: List<Song>) {
       openCollectionLongDialog(title, songs)
    }

    companion object {
        private val ARGUMENT_ID = "ARGUMENT_ID"
        private val ARGUMENT_NAME = "ARGUMENT_NAME"

        fun create(genre: Genre): GenreDetailFragment {
            val fragment = GenreDetailFragment()
            val bundle = Bundle()
            bundle.putLong(ARGUMENT_ID, genre.id)
            bundle.putString(ARGUMENT_NAME, genre.name)
            fragment.arguments = bundle
            return fragment
        }
    }

}