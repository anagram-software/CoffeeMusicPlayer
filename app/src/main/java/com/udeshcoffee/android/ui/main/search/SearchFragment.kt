package com.udeshcoffee.android.ui.main.search

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.bumptech.glide.Glide
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.navigateToDetail
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.extensions.showPlayingToast
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.common.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.common.adapters.ArtistAdapter
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

/**
* Created by Udathari on 9/12/2017.
*/
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModelDetail by inject()

    private lateinit var songAdpt: SongAdapter
    private lateinit var albumAdpt: AlbumAdapter
    private lateinit var artistAdpt: ArtistAdapter

    private lateinit var songs: TextView
    private lateinit var albums: TextView
    private lateinit var artists: TextView

    private lateinit var imm: InputMethodManager

    private var searchDisposable = CompositeDisposable()

    private var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_search, container, false)

        imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Set up player view
        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@SearchFragment.actionBar = supportActionBar
            }

            songs = findViewById(R.id.textView_songs)
            songs.visibility = View.GONE
            albums = findViewById(R.id.textView_album)
            albums.visibility = View.GONE
            artists = findViewById(R.id.textView_artist)
            artists.visibility = View.GONE

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                this.title = ""
            }
            setHasOptionsMenu(true)

            val searchView = findViewById<SearchView>(R.id.search)
            searchView?.apply {
                isActivated = true
                onActionViewExpanded()
                isIconified = false
                requestFocus()
            }
            searchDisposable.add(RxSearchView.queryTextChangeEvents(searchView)
                    .skip(1)
                    .debounce(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ searchViewQueryTextEvent -> viewModel.search(searchViewQueryTextEvent.queryText().toString()) }))

            // Album View
            val albumView = findViewById<RecyclerView>(R.id.album_recycler_view)
            albumView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            albumView.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin), resources.getDimensionPixelSize(R.dimen.mini_grid_spacing)))
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

            // Album View
            val artistView = findViewById<RecyclerView>(R.id.artist_recycler_view)
            artistView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            artistView.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin), resources.getDimensionPixelSize(R.dimen.mini_grid_spacing)))
            // specify an adapter (see also next example)
            artistAdpt = ArtistAdapter(ArtistAdapter.ITEM_TYPE_MINI, Glide.with(context), false)
            artistAdpt.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    viewModel.artistItemClicked(position)
                }

                override fun onItemOptionClick(position: Int) {
                    artistAdpt.getItem(position).let {
                        showPlayingToast(it)
                        viewModel.artistItemOptionClicked(position)
                    }
                }

                override fun onItemLongClick(position: Int) {
                    viewModel.artistItemLongClicked(position)
                }
            }
            artistView.adapter = artistAdpt

            // Song View
            val songView = findViewById<EmptyRecyclerView>(R.id.song_recycler_view)
            songView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isDrawingCacheEnabled = true
            songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            songView.isNestedScrollingEnabled = false

            songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, false)
            songAdpt.listener = object : OnSongItemClickListener {
                override fun onItemClick(position: Int) {
                    viewModel.songItemClicked(position)
                }

                override fun onItemLongClick(position: Int) {
                    viewModel.songItemLongClicked(position)
                }

                override fun onShuffleClick() {

                }
            }
            songView.adapter = songAdpt
        }

        return root
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home ) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            currentSongId.observe(this@SearchFragment, Observer {
                it?.let { songAdpt.currentId = it }
            })
            songs.observe(this@SearchFragment, Observer {
                it?.let {
                    songAdpt.accept(it)
                    if (it.isEmpty())
                        this@SearchFragment.songs.visibility = View.GONE
                    else
                        this@SearchFragment.songs.visibility = View.VISIBLE
                }
            })
            albums.observe(this@SearchFragment, Observer {
                it?.let {
                    albumAdpt.accept(it)
                    if (it.isEmpty())
                        this@SearchFragment.albums.visibility = View.GONE
                    else
                        this@SearchFragment.albums.visibility = View.VISIBLE
                }
            })
            artists.observe(this@SearchFragment, Observer {
                it?.let {
                    artistAdpt.accept(it)
                    if (it.isEmpty())
                        this@SearchFragment.artists.visibility = View.GONE
                    else
                        this@SearchFragment.artists.visibility = View.VISIBLE
                }
            })

            // Events
            showSongLongDialog.observe(this@SearchFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
            showCollectionLongDialog.observe(this@SearchFragment, Observer {
                it?.let { openCollectionLongDialog(it.first, it.second) }
            })
            showAlbum.observe(this@SearchFragment, Observer {
                it?.let { fragmentManager?.navigateToDetail(it) }
            })
            showArtist.observe(this@SearchFragment, Observer {
                it?.let { fragmentManager?.navigateToDetail(it) }
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
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchDisposable.dispose()
    }

    companion object {
        fun create() = SearchFragment()
    }
}