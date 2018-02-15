package com.udeshcoffee.android.ui.main.detail.artistdetail

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Html
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.*
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.loadArtistArtwork
import org.koin.android.ext.android.inject


/**
* Created by Udathari on 9/12/2017.
*/
class ArtistDetailFragment: Fragment(), ArtistDetailContract.View, AppBarLayout.OnOffsetChangedListener {

    val TAG = this.javaClass.simpleName

    override val presenter: ArtistDetailContract.Presenter by inject()

    private var mIsTheTitleVisible = false
    private var mIsTheTitleContainerVisible = true

    private lateinit var collapsedTitle: TextView
    private lateinit var expandedLayout: View
    private lateinit var expandedTitle: TextView
    private var tag: Array<TextView?> = arrayOfNulls(2)
    private lateinit var tagLayout: View
    private lateinit var biotext: TextView

    var artistId: Long = -1
    lateinit var artistName: String

    lateinit var songAdpt: SongAdapter
    lateinit var albumAdpt: AlbumAdapter

    private lateinit var detailImage: ImageView
    private lateinit var toolbarAlbums: Toolbar
    private lateinit var toolbarSongs: Toolbar
    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_artist_detail, container, false)

        artistId = arguments!!.getLong(ARGUMENT_ID)
        artistName = arguments!!.getString(ARGUMENT_NAME)

        // Set up player view
        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@ArtistDetailFragment.actionBar = supportActionBar
            }

            // Custom Collapsing Toolbar
            val appBar = findViewById<AppBarLayout>(R.id.appbar)
            appBar.addOnOffsetChangedListener(this@ArtistDetailFragment)

            expandedLayout = findViewById(R.id.expanded_layout)
            expandedTitle = findViewById(R.id.expanded_title)
            expandedTitle.text = artistName
            tagLayout = findViewById(R.id.expanded_tags)
            tagLayout.visibility = View.VISIBLE
            this@ArtistDetailFragment.tag[0] = findViewById(R.id.expanded_tag1)
            this@ArtistDetailFragment.tag[1] = findViewById(R.id.expanded_tag2)
            collapsedTitle = findViewById(R.id.collapsed_title)
            collapsedTitle.text = artistName
            collapsedTitle.fadeOut(0)

            detailImage = findViewById(R.id.detail_image)
            loadArtistArtwork(context, Glide.with(context), artistId, artistName, detailImage, true)

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                this.title = ""
            }
            setHasOptionsMenu(true)

            // Album View
            toolbarAlbums = findViewById(R.id.toolbar_albums)

            val albumView = findViewById<RecyclerView>(R.id.artist_album_recycler_view)
            albumView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            albumView.addItemDecoration(MiniGridItemDecor(resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.mini_grid_spacing)))
            // specify an adapter (see also next example)
            albumAdpt = AlbumAdapter(AlbumAdapter.ITEM_TYPE_MINI)
            albumAdpt.listener = object : OnGridItemClickListener {
                override fun onItemClick(position: Int, shareElement: View) {
                    presenter.albumItemClicked(position)
                }

                override fun onItemOptionClick(position: Int) {
                    presenter.albumItemOptionClicked(albumAdpt.getItem(position))
                }

                override fun onItemLongClick(position: Int) {
                    presenter.albumItemLongClicked(albumAdpt.getItem(position))
                }
            }
            albumView.adapter = albumAdpt

            // Song View
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

            val songView = findViewById<EmptyRecyclerView>(R.id.artist_song_recycler_view)
            songView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isDrawingCacheEnabled = true
            songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            songView.isNestedScrollingEnabled = false

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

            // Bio
            biotext = findViewById(R.id.info_info)
        }

        return root
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        appBarLayout?.let {
            val maxScroll = appBarLayout.totalScrollRange
            val percentage = Math.abs(verticalOffset) / maxScroll.toFloat()

            handleAlphaOnTitle(percentage)
            handleToolbarTitleVisibility(percentage)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.artist_detail_menu, menu)
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
            menu.findItem(R.id.action_sort_ascending).isChecked = presenter.sortAscending
            menu.findItem(R.id.action_sort_album_ascending).isChecked = presenter.albumSortAscending
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
            R.id.action_select_image -> presenter.selectImage()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult")
        presenter.result(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        presenter.artistId = artistId
        presenter.artistName = artistName
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

    override fun setTags(tags: Array<String>) {
        for (i in 0.. Math.min(1, tags.size - 1)) {
            tag[i]?.text = tags[i]
            tag[i]?.fadeIn(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong())
        }
    }

    override fun setBio(bio: String) {
        if (bio != ""){
            val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.fromHtml(bio, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(bio)
            }
            this.biotext.text = result
        }
    }

    override fun setCurrentSong(id: Long) {
        songAdpt.currentId = id
    }

    override fun showAlbum(position: Int) {
        val detail = albumAdpt.getItem(position)
        fragmentManager?.navigateToDetail(detail)
    }

    override fun artistArtChanged(id: Long, name: String) {
        loadArtistArtwork(context!!, Glide.with(context), id, name, detailImage, shouldCollect = false)
    }

    override fun artistArtDeleted() {
        detailImage.setImageResource(R.drawable.ic_person_white_24dp)
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

    override fun showSelectImageUI() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SELECT_PHOTO)
    }

    private fun handleToolbarTitleVisibility(percentage: Float) {
        if (percentage >= MainActivity.DetailFragments.PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if (!mIsTheTitleVisible) {
                collapsedTitle.fadeIn(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong())
                mIsTheTitleVisible = true
            }

        } else {

            if (mIsTheTitleVisible) {
                collapsedTitle.fadeOut(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong())
                mIsTheTitleVisible = false
            }
        }
    }

    private fun handleAlphaOnTitle(percentage: Float) {
        if (percentage >= MainActivity.DetailFragments.PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if (mIsTheTitleContainerVisible) {
                expandedTitle.fadeOut(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong())
                tagLayout.fadeOut(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong()/2)
                mIsTheTitleContainerVisible = false
            }

        } else {
            if (!mIsTheTitleContainerVisible) {
                expandedTitle.fadeIn(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong())
                tagLayout.fadeIn(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong()*2)
                mIsTheTitleContainerVisible = true
            }
        }
    }

    companion object {
        const val SELECT_PHOTO = 0

        private const val ARGUMENT_ID = "ARGUMENT_ID"
        private const val ARGUMENT_NAME = "ARGUMENT_ARTIST"

        fun create(artist: Artist): ArtistDetailFragment {
            val fragment = ArtistDetailFragment()
            val bundle = Bundle()
            bundle.putLong(ARGUMENT_ID, artist.id)
            bundle.putString(ARGUMENT_NAME, artist.name)
            fragment.arguments = bundle
            return fragment
        }
    }
}