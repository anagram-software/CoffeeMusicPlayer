package com.udeshcoffee.android.ui.main.detail.artistdetail

import androidx.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.text.Html
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.*
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.MiniGridItemDecor
import com.udeshcoffee.android.ui.common.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.loadArtistArtwork
import org.koin.android.ext.android.inject
import kotlin.math.abs


/**
 * Created by Udathari on 9/12/2017.
 */
class ArtistDetailFragment : androidx.fragment.app.Fragment(), com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener {

    private val viewModel: ArtistDetailViewModel by inject()

    private var mIsTheTitleVisible = false
    private var mIsTheTitleContainerVisible = true

    private lateinit var collapsedTitle: TextView
    private lateinit var expandedLayout: View
    private lateinit var expandedTitle: TextView
    private var tag: Array<TextView?> = arrayOfNulls(2)
    private lateinit var tagLayout: View
    private lateinit var biotext: TextView

    private lateinit var songAdpt: SongAdapter
    private lateinit var albumAdpt: AlbumAdapter

    private lateinit var detailImage: ImageView
    private lateinit var blurDetailImage: ImageView
    private lateinit var toolbarAlbums: Toolbar
    private lateinit var toolbarSongs: Toolbar
    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_artist_detail, container, false)

        // Set up player view
        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@ArtistDetailFragment.actionBar = supportActionBar
            }

            // Custom Collapsing Toolbar
            val appBar = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appbar)
            appBar.addOnOffsetChangedListener(this@ArtistDetailFragment)

            expandedLayout = findViewById(R.id.expanded_layout)
            expandedTitle = findViewById(R.id.expanded_title)
            expandedTitle.text = arguments!!.getString(ARGUMENT_NAME)
            tagLayout = findViewById(R.id.expanded_tags)
            tagLayout.visibility = View.VISIBLE
            this@ArtistDetailFragment.tag[0] = findViewById(R.id.expanded_tag1)
            this@ArtistDetailFragment.tag[1] = findViewById(R.id.expanded_tag2)
            collapsedTitle = findViewById(R.id.collapsed_title)
            collapsedTitle.text = arguments!!.getString(ARGUMENT_NAME)
            collapsedTitle.fadeOut(0)

            detailImage = findViewById(R.id.detail_image)
            loadArtistArtwork(context, Glide.with(context), arguments!!.getLong(ARGUMENT_ID),
                    arguments!!.getString(ARGUMENT_NAME, ""), detailImage, true)

            blurDetailImage = findViewById(R.id.blur_detail_image)
            loadArtistArtwork(context, Glide.with(context), arguments!!.getLong(ARGUMENT_ID),
                    arguments!!.getString(ARGUMENT_NAME, ""), blurDetailImage, shouldCollect = true, isBlur = true)

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                this.title = ""
            }
            setHasOptionsMenu(true)

            // Album View
            toolbarAlbums = findViewById(R.id.toolbar_albums)

            val albumView = findViewById<RecyclerView>(R.id.artist_album_recycler_view)
            albumView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
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

            // Song View
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

            val songView = findViewById<EmptyRecyclerView>(R.id.artist_song_recycler_view)
            songView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isNestedScrollingEnabled = false

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

            // Bio
            biotext = findViewById(R.id.info_info)
        }

        return root
    }

    override fun onOffsetChanged(appBarLayout: com.google.android.material.appbar.AppBarLayout?, verticalOffset: Int) {
        appBarLayout?.let {
            val maxScroll = appBarLayout.totalScrollRange
            val percentage = abs(verticalOffset) / maxScroll.toFloat()

            handleAlphaOnTitle(percentage)
            handleToolbarTitleVisibility(percentage)
            handleAlphaOnImage(percentage)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.artist_detail_menu, menu)
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
        menu.findItem(R.id.action_sort_ascending).isChecked = viewModel.songSortAscending
        menu.findItem(R.id.action_sort_album_ascending).isChecked = viewModel.albumSortAscending
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
            R.id.action_select_image -> showSelectImageUI()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult")
        viewModel.result(requestCode, resultCode, data)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            currentSongId.observe(this@ArtistDetailFragment, Observer {
                it?.let { songAdpt.currentId = it }
            })
            songs.observe(this@ArtistDetailFragment, Observer {
                it?.let {
                    songAdpt.accept(it)
                    if (it.isEmpty()) {
                        toolbarSongs.visibility = View.GONE
                    }
                }
            })
            albums.observe(this@ArtistDetailFragment, Observer {
                it?.let {
                    albumAdpt.accept(it)
                    if (it.isEmpty()) {
                        toolbarAlbums.visibility = View.GONE
                    }
                }
            })
            tags.observe(this@ArtistDetailFragment, Observer {
                it?.let {
                    for (i in 0..1.coerceAtMost(it.size - 1)) {
                        tag[i]?.text = it[i]
                        tag[i]?.fadeIn(ALPHA_ANIMATIONS_DURATION.toLong())
                    }
                }
            })
            bio.observe(this@ArtistDetailFragment, Observer {
                it?.let {
                    if (it != "") {
                        val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            @Suppress("DEPRECATION")
                            Html.fromHtml(it)
                        }
                        biotext.text = result
                    }
                }
            })

            // Events
            showFavoriteToast.observe(this@ArtistDetailFragment, Observer {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            })
            showAddToPlaylistDialog.observe(this@ArtistDetailFragment, Observer {
                it?.let { openAddToPlaylistDialog(it as ArrayList<Song>) }
            })
            showSongLongDialog.observe(this@ArtistDetailFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
            showCollectionLongDialog.observe(this@ArtistDetailFragment, Observer {
                it?.let { openCollectionLongDialog(it.first, it.second) }
            })
            showAlbum.observe(this@ArtistDetailFragment, Observer {
                it?.let { view?.findNavController()?.navigate(R.id.albumDetailFragment, AlbumDetailFragment.createBundle(it)) }
            })
            artistArtChanged.observe(this@ArtistDetailFragment, Observer {
                it?.let {
                    loadArtistArtwork(context!!, Glide.with(context!!), it.first, it.second, detailImage,
                            shouldCollect = false)
                }
            })
            artistArtDeleted.observe(this@ArtistDetailFragment, Observer {
                it?.let { detailImage.setImageResource(R.drawable.ic_person_white_24dp) }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.artistId = arguments!!.getLong(ARGUMENT_ID)
        viewModel.artistName = arguments!!.getString(ARGUMENT_NAME, "")
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    private fun showSelectImageUI() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SELECT_PHOTO)
    }

    private fun handleToolbarTitleVisibility(percentage: Float) {
        if (percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {

            if (!mIsTheTitleVisible) {
                collapsedTitle.fadeIn(ALPHA_ANIMATIONS_DURATION.toLong())
                mIsTheTitleVisible = true
            }

        } else {

            if (mIsTheTitleVisible) {
                collapsedTitle.fadeOut(ALPHA_ANIMATIONS_DURATION.toLong())
                mIsTheTitleVisible = false
            }
        }
    }

    private fun handleAlphaOnTitle(percentage: Float) {
        if (percentage >= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
            if (mIsTheTitleContainerVisible) {
                expandedTitle.fadeOut(ALPHA_ANIMATIONS_DURATION.toLong())
                tagLayout.fadeOut(ALPHA_ANIMATIONS_DURATION.toLong() / 2)
                mIsTheTitleContainerVisible = false
            }

        } else {
            if (!mIsTheTitleContainerVisible) {
                expandedTitle.fadeIn(ALPHA_ANIMATIONS_DURATION.toLong())
                tagLayout.fadeIn(ALPHA_ANIMATIONS_DURATION.toLong() * 2)
                mIsTheTitleContainerVisible = true
            }
        }
    }

    private fun handleAlphaOnImage(percentage: Float) {
        detailImage.alpha = 1.0f - 1.0f.coerceAtMost(percentage * 1.2f)
    }

    companion object {
        const val TAG = "ArtistDetailFragment"

        const val SELECT_PHOTO = 0

        private const val ARGUMENT_ID = "ARGUMENT_ID"
        private const val ARGUMENT_NAME = "ARGUMENT_ARTIST"

        private const val PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.9f
        private const val PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f
        private const val ALPHA_ANIMATIONS_DURATION = 200

        fun createBundle(artist: Artist): Bundle {
            val bundle = Bundle()
            bundle.putLong(ARGUMENT_ID, artist.id)
            bundle.putString(ARGUMENT_NAME, artist.name)
            return bundle
        }
    }
}