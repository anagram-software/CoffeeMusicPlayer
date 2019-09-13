package com.udeshcoffee.android.ui.main.detail.genredetail

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.extensions.showPlayingToast
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.common.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.utils.SortManager
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 9/12/2017.
 */
class GenreDetailFragment : androidx.fragment.app.Fragment() {

    private val viewModel: GenreDetailViewModel by inject()

    private lateinit var songAdpt: SongAdapter
    private lateinit var albumAdpt: AlbumAdapter

    private lateinit var toolbarSongs: Toolbar
    private lateinit var toolbarAlbums: Toolbar

    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_genre_detail, container, false)

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

            val songView = findViewById<EmptyRecyclerView>(R.id.genre_song_recycler_view)
            songView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isNestedScrollingEnabled = false

            // specify an adapter (see also next example)
            songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, true)
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


            // Album View
            val albumView = findViewById<RecyclerView>(R.id.genre_album_recycler_view)
            albumView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            albumView.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.mini_grid_spacing)))
            // specify an adapter (see also next example)
            albumAdpt = AlbumAdapter(AlbumAdapter.ITEM_TYPE_MINI)
            albumAdpt.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    viewModel.albumItemClicked(position)
                }

                override fun onItemOptionClick(position: Int) {
                    albumAdpt.getItem(position).let {
                        showPlayingToast(it)
                        viewModel.albumItemOptionClicked(position)
                    }
                }

                override fun onItemLongClick(position: Int) {
                    viewModel.albumItemLongClicked(position)
                }
            }
            albumView.adapter = albumAdpt
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.genre_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (viewModel.songSortOrder) {
            SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
            SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
            SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
            SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
            SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
            SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
            SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.action_sort_album_name).isChecked = true
        }
        when (viewModel.albumSortOrder) {
            SortManager.AlbumSort.DEFAULT -> menu.findItem(R.id.action_sort_album_default).isChecked = true
            SortManager.AlbumSort.NAME -> menu.findItem(R.id.action_sort_album_title).isChecked = true
            SortManager.AlbumSort.YEAR -> menu.findItem(R.id.action_sort_album_year).isChecked = true
        }
        viewModel.let {
            menu.findItem(R.id.action_sort_ascending).isChecked = it.songSortAscending
            menu.findItem(R.id.action_sort_album_ascending).isChecked = it.albumSortAscending
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var sortChanged = true
        var albumSortChanged = true

        if (item.groupId == R.id.sort_album) {
            when (item.itemId) {
                R.id.action_sort_album_default -> viewModel.albumSortOrder = SortManager.AlbumSort.DEFAULT
                R.id.action_sort_album_title -> viewModel.albumSortOrder = SortManager.AlbumSort.NAME
                R.id.action_sort_album_year -> viewModel.albumSortOrder = SortManager.AlbumSort.YEAR
                R.id.action_sort_album_ascending -> viewModel.albumSortAscending = !item.isChecked
            }
        } else albumSortChanged = false

        if (item.groupId == R.id.sort_song) {
            when (item.itemId) {
                R.id.action_sort_default -> viewModel.songSortOrder = SortManager.SongSort.DEFAULT
                R.id.action_sort_title -> viewModel.songSortOrder = SortManager.SongSort.NAME
                R.id.action_sort_track -> viewModel.songSortOrder = SortManager.SongSort.TRACK_NUMBER
                R.id.action_sort_duration -> viewModel.songSortOrder = SortManager.SongSort.DURATION
                R.id.action_sort_year -> viewModel.songSortOrder = SortManager.SongSort.YEAR
                R.id.action_sort_date -> viewModel.songSortOrder = SortManager.SongSort.DATE
                R.id.action_sort_album_name -> viewModel.songSortOrder = SortManager.SongSort.ALBUM_NAME
                R.id.action_sort_ascending -> viewModel.songSortAscending = !item.isChecked
            }
        } else sortChanged = false

        when (item.itemId) {
            android.R.id.home -> activity?.onBackPressed()
            R.id.action_play -> viewModel.playClick()
            R.id.action_play_next -> viewModel.playNextClick()
            R.id.action_queue -> viewModel.queueClick()
            R.id.action_add_to_favorites -> viewModel.addToFavoritesClick()
            R.id.action_add_to_playlist -> viewModel.addToPlaylistClick()
        }

        if (albumSortChanged) {
            viewModel.fetchAlbums()
            activity?.invalidateOptionsMenu()
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
            currentSongId.observe(this@GenreDetailFragment, Observer {
                it?.let { songAdpt.currentId = it }
            })
            songs.observe(this@GenreDetailFragment, Observer {
                it?.let {
                    songAdpt.accept(it)
                    if (it.isEmpty()) {
                        toolbarSongs.visibility = View.GONE
                    }
                }
            })
            albums.observe(this@GenreDetailFragment, Observer {
                it?.let {
                    albumAdpt.accept(it)
                    if (it.isEmpty()) {
                        toolbarAlbums.visibility = View.GONE
                    }
                }
            })

            // Events
            showFavoriteToast.observe(this@GenreDetailFragment, Observer {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            })
            showAddToPlaylistDialog.observe(this@GenreDetailFragment, Observer {
                it?.let { openAddToPlaylistDialog(it as ArrayList<Song>) }
            })
            showSongLongDialog.observe(this@GenreDetailFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
            showCollectionLongDialog.observe(this@GenreDetailFragment, Observer {
                it?.let { openCollectionLongDialog(it.first, it.second) }
            })
            showAlbum.observe(this@GenreDetailFragment, Observer {
                it?.let { view?.findNavController()?.navigate(R.id.albumDetailFragment, AlbumDetailFragment.createBundle(it)) }
            })
        }
    }


    override fun onResume() {
        super.onResume()
        viewModel.genreId = arguments!!.getLong(ARGUMENT_ID)
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    companion object {
        private const val ARGUMENT_ID = "ARGUMENT_ID"
        private const val ARGUMENT_NAME = "ARGUMENT_NAME"

        fun createBundle(genre: Genre): Bundle {
            val bundle = Bundle()
            bundle.putLong(ARGUMENT_ID, genre.id)
            bundle.putString(ARGUMENT_NAME, genre.name)
            return bundle
        }
    }

}