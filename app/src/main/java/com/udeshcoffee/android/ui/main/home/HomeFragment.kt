package com.udeshcoffee.android.ui.main.home

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.Glide
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.navigateToDetail
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.openDrawer
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.common.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.common.adapters.ArtistAdapter
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.utils.loadCoolCardView
import com.udeshcoffee.android.views.FadableLayout
import com.udeshcoffee.android.views.NonClickableToolbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 12/1/2017.
*/
class HomeFragment: Fragment(), HomeContract.View {

    override val presenter: HomeContract.Presenter by inject()

    private var actionBar: ActionBar? = null
    private lateinit var appBar: AppBarLayout

    // Library Card
    private lateinit var libraryCard: CardView
    private lateinit var librarySongs: TextView
    private lateinit var libraryAlbums: TextView
    private lateinit var libraryArtists: TextView
    private lateinit var libraryPlaylists: TextView

    private lateinit var infoCard: CardView

    // Top Scetion
    private lateinit var topSongsToolbar: Toolbar
    private lateinit var topSongs: RecyclerView
    private lateinit var topSongsAdapter: SongAdapter

    private lateinit var topAlbumsFL: FadableLayout
    private lateinit var topAlbumsCV: CardView
    private lateinit var topAlbums: RecyclerView
    private lateinit var topAlbumsAdapter: AlbumAdapter

    private lateinit var topArtistsFL: FadableLayout
    private lateinit var topArtistsCV: CardView
    private lateinit var topArtists: RecyclerView
    private lateinit var topArtistsAdapter: ArtistAdapter

    private lateinit var recentlyAddedToolbar: Toolbar
    private lateinit var recentlyAdded: RecyclerView
    private lateinit var recentlyAddedAdapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        view.apply {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.apply {
                title = ""
                setHomeButtonEnabled(true)
            }

            appBar = findViewById(R.id.appbar)

            val libraryToolbar = findViewById<NonClickableToolbar>(R.id.library_toolbar)
            libraryToolbar.inflateMenu(R.menu.home_library_menu)

            libraryCard = findViewById(R.id.library_card)
            libraryCard.setOnClickListener{
                (activity as MainActivity).onHomeLibraryClick()
            }

            librarySongs = findViewById(R.id.library_songs)
            libraryAlbums = findViewById(R.id.library_albums)
            libraryArtists = findViewById(R.id.library_artists)
            libraryPlaylists = findViewById(R.id.library_playlists)

            infoCard = findViewById(R.id.info)
            Observable.fromCallable {
                sharedPreferences.getBoolean(PreferenceUtil.HOME_INFO_DISMISSED, false)
            }.first(false)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({ if (it) infoCard.visibility = View.GONE else infoCard.visibility = View.VISIBLE },{})
            val actionInfoDismiss = findViewById<Button>(R.id.action_info_dismiss)
            actionInfoDismiss.setOnClickListener{
                infoCard.visibility = View.GONE
                sharedPreferences.edit().putBoolean(PreferenceUtil.HOME_INFO_DISMISSED, true).apply()
            }

            // Top Songs
            topSongsToolbar = findViewById(R.id.top_songs_toolbar)
            topSongs = findViewById(R.id.top_songs)
            topSongsAdapter = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, false)
            topSongsAdapter.listener = object : OnSongItemClickListener {
                override fun onItemClick(position: Int) {
                    presenter.itemClicked(position, topSongsAdapter.songList)
                }

                override fun onItemLongClick(position: Int) {
                    presenter.itemLongClicked(topSongsAdapter.getItem(position))

                }

                override fun onShuffleClick() {} // Unused

            }
            topSongs.adapter = topSongsAdapter
            topSongs.layoutManager = LinearLayoutManager(context)

            // Top Albums
            topAlbumsFL = findViewById(R.id.top_albums_fl)
            topAlbumsCV = findViewById(R.id.top_albums_cv)
            topAlbums = findViewById(R.id.top_albums)
            topAlbumsAdapter = AlbumAdapter(AlbumAdapter.ITEM_TYPE_MINI)
            topAlbumsAdapter.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    presenter.albumItemClicked(position)
                }

                override fun onItemLongClick(position: Int) {
                    presenter.albumItemLongClicked(topAlbumsAdapter.getItem(position))
                }

                override fun onItemOptionClick(position: Int) {}

            }
            topAlbums.adapter = topAlbumsAdapter
            topAlbums.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            topAlbums.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.home_grid_spacing),
                    resources.getDimensionPixelSize(R.dimen.home_grid_spacing)))

            // Top Artists
            topArtistsFL = findViewById(R.id.top_artists_fl)
            topArtistsCV = findViewById(R.id.top_artists_cv)
            topArtists = findViewById(R.id.top_artists)
            topArtistsAdapter = ArtistAdapter(AlbumAdapter.ITEM_TYPE_MINI, Glide.with(context), true)
            topArtistsAdapter.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    presenter.artistItemClicked(position)
                }

                override fun onItemLongClick(position: Int) {
                    presenter.artistItemLongClicked(topArtistsAdapter.getItem(position))
                }

                override fun onItemOptionClick(position: Int) {}

            }
            topArtists.adapter = topArtistsAdapter
            topArtists.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            topArtists.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.home_grid_spacing),
                    resources.getDimensionPixelSize(R.dimen.home_grid_spacing)))

            // Recently Added
            recentlyAddedToolbar = findViewById(R.id.recently_added_toolbar)
            recentlyAdded = findViewById(R.id.recently_added)
            recentlyAddedAdapter = SongAdapter(SongAdapter.ITEM_TYPE_NORMAL, false)
            recentlyAddedAdapter.listener = object : OnSongItemClickListener {
                override fun onItemClick(position: Int) {
                    presenter.itemClicked(position, recentlyAddedAdapter.songList)
                }

                override fun onItemLongClick(position: Int) {
                    presenter.itemLongClicked(recentlyAddedAdapter.getItem(position))

                }

                override fun onShuffleClick() {} // Unused

            }
            recentlyAdded.adapter = recentlyAddedAdapter
            recentlyAdded.layoutManager = LinearLayoutManager(context)

        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.home_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {activity?.openDrawer()}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun setLibraryStats(songs: Int?, albums: Int?, artists: Int?, playlists: Int?) {
        songs?.let { librarySongs.text = "$songs" }
        albums?.let { libraryAlbums.text = "$albums" }
        artists?.let { libraryArtists.text = "$artists" }
        playlists?.let { libraryPlaylists.text = "$playlists" }
    }

    override fun showHideInfo(isShow: Boolean) {
        if (isShow)
            infoCard.visibility = View.GONE
        else
            infoCard.visibility = View.VISIBLE
    }

    override fun setTopSongs(items: List<Song>) {
        if (items.isNotEmpty()) {
            topSongsToolbar.visibility = View.VISIBLE
            topSongsAdapter.accept(items)
        } else {
            topSongsToolbar.visibility = View.GONE
        }
    }

    override fun setTopAlbums(items: List<Album>) {
        if (items.isNotEmpty()) {
            topAlbumsCV.visibility = View.VISIBLE
            loadCoolCardView(context!!, albumId = items[0].id, background = topAlbumsFL)
            topAlbumsAdapter.accept(items)
        } else {
            topAlbumsCV.visibility = View.GONE
        }
    }

    override fun setTopArtists(items: List<Artist>) {
        if (items.isNotEmpty()) {
            topArtistsCV.visibility = View.VISIBLE
            loadCoolCardView(context!!, artistId = items[0].id, background = topArtistsFL)
            topArtistsAdapter.accept(items)
        } else {
            topArtistsCV.visibility = View.GONE
        }
    }

    override fun setRecentlyAdded(items: List<Song>) {
        if (items.isNotEmpty()) {
            recentlyAddedToolbar.visibility = View.VISIBLE
            recentlyAddedAdapter.accept(items)
        } else {
            recentlyAddedToolbar.visibility = View.GONE
        }
    }

    override fun showSongLongDialog(song: Song) {
        openSongLongDialog(song)
    }

    override fun showCollectionLongDialog(title: String, songs: List<Song>) {
        openCollectionLongDialog(title, songs)
    }

    override fun showAlbum(position: Int) {
        val detail = topAlbumsAdapter.getItem(position)
        fragmentManager?.navigateToDetail(detail)
    }

    override fun showArtist(position: Int) {
        val detail = topArtistsAdapter.getItem(position)
        fragmentManager?.navigateToDetail(detail)
    }

    companion object {
        fun create(): HomeFragment = HomeFragment()
    }

}