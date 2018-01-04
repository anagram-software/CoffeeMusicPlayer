package com.udeshcoffee.android.ui.main.detail.genredetail

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.udeshcoffee.android.*
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.ui.detail.albumdetail.GenreDetailContract
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailPresenter
import com.udeshcoffee.android.utils.Injection
import com.udeshcoffee.android.utils.SortManager

/**
* Created by Udathari on 9/12/2017.
*/
class GenreDetailFragment: Fragment(), GenreDetailContract.View {

    override var presenter: GenreDetailContract.Presenter? = null

    lateinit var songAdpt: SongAdapter
    lateinit var albumAdpt: AlbumAdapter

    lateinit var songs: TextView
    lateinit var albums: TextView

    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_genre_detail, container, false)

        val name = arguments!!.getString(MainActivity.DetailFragments.ARGUMENT_NAME)

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

            songs = findViewById(R.id.textView_songs)
            albums = findViewById(R.id.textView_album)

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
                    presenter?.itemClicked(position, songAdpt.songList)
                }

                override fun onItemLongClick(position: Int) {
                    presenter?.itemLongClicked(songAdpt.songList[position])
                }

                override fun onShuffleClick() {
                    presenter?.shuffleClicked(songAdpt.songList)
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
                    presenter?.albumItemClicked(position)
                }

                override fun onItemOptionClick() {
                }

                override fun onItemLongClick(position: Int) {
                    presenter?.albumItemLongClicked(albumAdpt.getItem(position))
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
            when (presenter?.sortOrder) {
                SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
                SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
                SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
                SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
                SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
                SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
                SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.action_sort_album_name).isChecked = true
            }
            when (presenter?.albumSortOrder) {
                SortManager.AlbumSort.DEFAULT -> menu.findItem(R.id.action_sort_album_default).isChecked = true
                SortManager.AlbumSort.NAME -> menu.findItem(R.id.action_sort_album_title).isChecked = true
                SortManager.AlbumSort.YEAR -> menu.findItem(R.id.action_sort_album_year).isChecked = true
            }
            presenter?.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.sortAscending
                menu.findItem(R.id.action_sort_album_ascending).isChecked = it.albumSortAscending }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true
        var albumSortChanged = true

        if (item?.groupId == R.id.sort_album) {
            when (item.itemId) {
                R.id.action_sort_album_default -> presenter?.albumSortOrder = SortManager.AlbumSort.DEFAULT
                R.id.action_sort_album_title -> presenter?.albumSortOrder = SortManager.AlbumSort.NAME
                R.id.action_sort_album_year -> presenter?.albumSortOrder = SortManager.AlbumSort.YEAR
                R.id.action_sort_album_ascending -> presenter?.albumSortAscending = !item.isChecked
            }
        } else albumSortChanged = false

        if (item?.groupId == R.id.sort_song) {
            when (item.itemId) {
                R.id.action_sort_default -> presenter?.sortOrder = SortManager.SongSort.DEFAULT
                R.id.action_sort_title -> presenter?.sortOrder = SortManager.SongSort.NAME
                R.id.action_sort_track -> presenter?.sortOrder = SortManager.SongSort.TRACK_NUMBER
                R.id.action_sort_duration -> presenter?.sortOrder = SortManager.SongSort.DURATION
                R.id.action_sort_year -> presenter?.sortOrder = SortManager.SongSort.YEAR
                R.id.action_sort_date -> presenter?.sortOrder = SortManager.SongSort.DATE
                R.id.action_sort_album_name -> presenter?.sortOrder = SortManager.SongSort.ALBUM_NAME
                R.id.action_sort_ascending -> presenter?.sortAscending = !item.isChecked
            }
        } else sortChanged = false

        when (item?.itemId) {
            android.R.id.home -> activity?.onBackPressed()
            R.id.action_play -> presenter?.playClick(songAdpt.songList)
            R.id.action_play_next -> presenter?.playNextClick(songAdpt.songList)
            R.id.action_queue -> presenter?.queueClick(songAdpt.songList)
            R.id.action_add_to_favorites -> presenter?.addToFavoritesClick(songAdpt.songList)
            R.id.action_add_to_playlist -> presenter?.addToPlaylistClick(songAdpt.songList)
        }

        if (albumSortChanged) {
            presenter?.fetchAlbums()
            activity?.supportInvalidateOptionsMenu()
        }

        if (sortChanged) {
            presenter?.fetchSongs()
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
        songAdpt.accept(items)
        if (items.isEmpty()) {
            albums.visibility = View.GONE
        }
    }

    override fun populateAlbumItems(items: List<Album>) {
        albumAdpt.accept(items)
        if (items.isEmpty()) {
            albums.visibility = View.GONE
        }
    }

    override fun setCurrentSong(id: Long) {
        songAdpt.currentId = id
    }

    override fun showAlbum(position: Int) {
        val detail = albumAdpt.getItem(position)
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.ALBUM_DETAIL)
                as AlbumDetailFragment? ?: AlbumDetailFragment()
        AlbumDetailPresenter(detail, detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.ALBUM_DETAIL, detail)
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

}