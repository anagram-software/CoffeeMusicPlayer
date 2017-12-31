package com.udeshcoffee.android.ui.main.search

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
import com.udeshcoffee.android.doSharedTransaction
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.openSongLongDialog
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.adapters.ArtistAdapter
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.ui.main.detail.artistdetail.ArtistDetailFragment
import com.udeshcoffee.android.ui.detail.albumdetail.ArtistDetailPresenter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailPresenter
import com.udeshcoffee.android.utils.Injection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

/**
 * Created by Udathari on 9/12/2017.
 */
class SearchFragment : Fragment(), SearchContract.View {
    val TAG = "SearchFragment"

    override var presenter: SearchContract.Presenter? = null

    lateinit var songAdpt: SongAdapter
    lateinit var albumAdpt: AlbumAdapter
    lateinit var artistAdpt: ArtistAdapter

    lateinit var songs: TextView
    lateinit var albums: TextView
    lateinit var artists: TextView


    lateinit var imm: InputMethodManager

    var searchDisposable = CompositeDisposable()

    var actionBar: ActionBar? = null

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
                    .subscribe({ searchViewQueryTextEvent -> presenter?.search(searchViewQueryTextEvent.queryText().toString()) }))

            // Album View
            val albumView = findViewById<RecyclerView>(R.id.album_recycler_view)
            albumView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            albumView.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin), resources.getDimensionPixelSize(R.dimen.mini_grid_spacing)))
            // specify an adapter (see also next example)
            albumAdpt = AlbumAdapter(AlbumAdapter.ITEM_TYPE_MINI)
            albumAdpt.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    presenter?.albumItemClicked(position)
                }

                override fun onItemOptionClick() {
                }

                override fun onItemLongClick(position: Int) {
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
                    presenter?.artistItemClicked(position)
                }

                override fun onItemOptionClick() {
                }

                override fun onItemLongClick(position: Int) {
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
                    val tempList = ArrayList<Song>()
                    tempList.add(songAdpt.getItem(position))
                    presenter?.itemClicked(0, tempList)
                }

                override fun onItemLongClick(position: Int) {
                    presenter?.itemLongClicked(songAdpt.songList[position])
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

    override fun onResume() {
        super.onResume()
        presenter?.start()
    }

    override fun onPause() {
        super.onPause()
        presenter?.stop()
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchDisposable.dispose()
    }

    override fun populateItems(items: List<Song>) {
        songAdpt.accept(items)
        if (items.isEmpty())
            songs.visibility = View.GONE
        else
            songs.visibility = View.VISIBLE
    }

    override fun populateAlbumItems(items: List<Album>) {
        albumAdpt.accept(items)
        if (items.isEmpty())
            albums.visibility = View.GONE
        else
            albums.visibility = View.VISIBLE
    }

    override fun populateArtistItems(items: List<Artist>) {
        artistAdpt.accept(items)
        if (items.isEmpty())
            artists.visibility = View.GONE
        else
            artists.visibility = View.VISIBLE
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

    override fun showArtist(position: Int) {
        val detail = artistAdpt.getItem(position)
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.ARTIST_DETAIL)
                as ArtistDetailFragment? ?: ArtistDetailFragment()
        ArtistDetailPresenter(detail, detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.ARTIST_DETAIL, detail)
    }

    override fun showAddToPlaylistDialog(songs: ArrayList<Song>) {}

    override fun showFavoritesToast(isFavorite: Boolean) {}

    override fun showSongLongDialog(song: Song) {
        openSongLongDialog(song)
    }
}