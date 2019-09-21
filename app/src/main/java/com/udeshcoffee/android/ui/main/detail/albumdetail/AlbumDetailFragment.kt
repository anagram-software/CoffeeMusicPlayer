package com.udeshcoffee.android.ui.main.detail.albumdetail

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.fadeIn
import com.udeshcoffee.android.extensions.fadeOut
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.common.adapters.SongAdapter
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.loadAlbumArtwork
import org.koin.android.ext.android.inject
import kotlin.math.abs

/**
 * Created by Udathari on 9/12/2017.
 */
class AlbumDetailFragment : Fragment(), com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener {

    val viewModel: AlbumDetailViewModel by inject()

    private var mIsTheTitleVisible = false
    private var mIsTheTitleContainerVisible = true

    private lateinit var collapsedTitle: TextView
    private lateinit var expandedTitle: TextView
    private lateinit var expandedSubtitle: TextView

    private lateinit var songAdpt: SongAdapter
    private lateinit var detailImage: ImageView
    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_album_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        val name = arguments!!.getString(ARGUMENT_NAME)
        val artistName = arguments!!.getString(ARGUMENT_ARTIST_NAME)

        with(view) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@AlbumDetailFragment.actionBar = supportActionBar
            }

            // Custom Collapsing Toolbar
            val appBar = findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appbar)
            appBar.addOnOffsetChangedListener(this@AlbumDetailFragment)

            expandedTitle = findViewById(R.id.expanded_title)
            expandedTitle.text = name
            expandedSubtitle = findViewById(R.id.expanded_subtitle)
            expandedSubtitle.visibility = android.view.View.VISIBLE
            expandedSubtitle.text = "By $artistName"
            collapsedTitle = findViewById(R.id.collapsed_title)
            collapsedTitle.text = name
            collapsedTitle.fadeOut(0)

            detailImage = findViewById(R.id.detail_image)
            loadAlbumArtwork(context, arguments!!.getLong(ARGUMENT_ID), detailImage, true) {
                startPostponedEnterTransition()
            }

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = ""
            }
            setHasOptionsMenu(true)

            val toolbarTracks = findViewById<Toolbar>(R.id.toolbar_tracks)
            toolbarTracks.inflateMenu(R.menu.song_containing_menu)
            toolbarTracks.setOnMenuItemClickListener {
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

            val songView = findViewById<EmptyRecyclerView>(R.id.album_detail_recycler_view)
            songView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.isNestedScrollingEnabled = false

            // specify an adapter (see also next example)
            songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_ALBUM_ITEM, true)
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
        }
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
        inflater.inflate(R.menu.common_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (viewModel.songSortOrder) {
            SortManager.SongSort.ALBUM_DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
            SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
            SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
            SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
            SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
            SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
            SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
        }
        viewModel.let { menu.findItem(R.id.action_sort_ascending).isChecked = it.songSortAscending }
        super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var sortChanged = true

        if (item.groupId == R.id.sort_group) {
            when (item.itemId) {
                R.id.action_sort_default -> viewModel.songSortOrder = SortManager.SongSort.ALBUM_DEFAULT
                R.id.action_sort_title -> viewModel.songSortOrder = SortManager.SongSort.NAME
                R.id.action_sort_track -> viewModel.songSortOrder = SortManager.SongSort.TRACK_NUMBER
                R.id.action_sort_duration -> viewModel.songSortOrder = SortManager.SongSort.DURATION
                R.id.action_sort_year -> viewModel.songSortOrder = SortManager.SongSort.YEAR
                R.id.action_sort_date -> viewModel.songSortOrder = SortManager.SongSort.DATE
                R.id.action_sort_artist_name -> viewModel.songSortOrder = SortManager.SongSort.ARTIST_NAME
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

        if (sortChanged) {
            viewModel.fetchSongs()
            activity?.invalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            currentSongId.observe(this@AlbumDetailFragment, Observer {
                it?.let { songAdpt.currentId = it }
            })
            songs.observe(this@AlbumDetailFragment, Observer {
                it?.let { songAdpt.accept(it) }
            })

            // Events
            showFavoriteToast.observe(this@AlbumDetailFragment, Observer {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            })
            showAddToPlaylistDialog.observe(this@AlbumDetailFragment, Observer {
                it?.let { openAddToPlaylistDialog(it as ArrayList<Song>) }
            })
            showSongLongDialog.observe(this@AlbumDetailFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.albumId = arguments!!.getLong(ARGUMENT_ID)
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
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
                expandedTitle.fadeOut(ALPHA_ANIMATIONS_DURATION.toLong() / 2)
                expandedSubtitle.fadeOut(ALPHA_ANIMATIONS_DURATION.toLong())
                mIsTheTitleContainerVisible = false
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                expandedTitle.fadeIn(ALPHA_ANIMATIONS_DURATION.toLong() * 2)
                expandedSubtitle.fadeIn(ALPHA_ANIMATIONS_DURATION.toLong())
                mIsTheTitleContainerVisible = true
            }
        }
    }

    private fun handleAlphaOnImage(percentage: Float) {
        detailImage.alpha = 1.0f - 1.0f.coerceAtMost(percentage * 1.8f)
    }


    companion object {
        private const val ARGUMENT_ID = "ARGUMENT_ID"
        private const val ARGUMENT_NAME = "ARGUMENT_NAME"
        private const val ARGUMENT_ARTIST_NAME = "ARGUMENT_ARTIST_NAME"

        private const val PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.9f
        private const val PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f
        private const val ALPHA_ANIMATIONS_DURATION = 200

        fun createBundle(album: Album): Bundle {
            val bundle = Bundle()
            bundle.putLong(ARGUMENT_ID, album.id)
            bundle.putString(ARGUMENT_NAME, album.title)
            bundle.putString(ARGUMENT_ARTIST_NAME, album.artist)
            return bundle
        }

//        private const val TAG = "AlbumDetailFragment"
    }
}