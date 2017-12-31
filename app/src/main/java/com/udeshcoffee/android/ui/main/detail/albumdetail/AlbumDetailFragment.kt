package com.udeshcoffee.android.ui.main.detail.albumdetail

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.udeshcoffee.android.*
import com.udeshcoffee.android.interfaces.OnSongItemClickListener
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.adapters.SongAdapter
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.loadAlbumArtwork


/**
 * Created by Udathari on 9/12/2017.
 */
class AlbumDetailFragment: Fragment(), AlbumDetailContract.View, AppBarLayout.OnOffsetChangedListener {

    val TAG = "AlbumDetailFragment"

    private var mIsTheTitleVisible = false
    private var mIsTheTitleContainerVisible = true

    private lateinit var collapsedTitle: TextView
    private lateinit var expandedLayout: View
    private lateinit var expandedTitle: TextView
    private lateinit var expandedSubtitle: TextView

    override var presenter: AlbumDetailContract.Presenter? = null

    lateinit var songAdpt: SongAdapter
    lateinit var detailImage: ImageView
    var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_album_detail, container, false)

        val id = arguments!!.getLong(MainActivity.DetailFragments.ARGUMENT_ID)
        val name = arguments!!.getString(MainActivity.DetailFragments.ARGUMENT_NAME)
        val artistName = arguments!!.getString(MainActivity.DetailFragments.ARGUMENT_ARTIST_NAME)
        val transitionName = arguments!!.getString(MainActivity.DetailFragments.ARGUMENT_TRANSITION_NAME)

        // Set up player view
        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@AlbumDetailFragment.actionBar = supportActionBar
            }

            // Custom Collapsing Toolbar
            val appBar = findViewById<AppBarLayout>(R.id.appbar)
            appBar.addOnOffsetChangedListener(this@AlbumDetailFragment)

            expandedLayout = findViewById(R.id.expanded_layout)
            expandedTitle = findViewById(R.id.expanded_title)
            expandedTitle.text = name
            expandedSubtitle = findViewById(R.id.expanded_subtitle)
            expandedSubtitle.visibility = View.VISIBLE
            expandedSubtitle.text = "By $artistName"
            collapsedTitle = findViewById(R.id.collapsed_title)
            collapsedTitle.text = name
            collapsedTitle.fadeOut(0)

            detailImage = findViewById(R.id.detail_image)
            loadAlbumArtwork(context, id, detailImage)
            ViewCompat.setTransitionName(detailImage, transitionName)

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = ""
            }
            setHasOptionsMenu(true)

            val songView = findViewById<EmptyRecyclerView>(R.id.album_detail_recycler_view)
            songView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.isDrawingCacheEnabled = true
            songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            songView.isNestedScrollingEnabled = false

            // specify an adapter (see also next example)
            songAdpt = SongAdapter(SongAdapter.ITEM_TYPE_ALBUM_ITEM, true)
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
        inflater?.inflate(R.menu.common_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        if (menu != null) {
            when (presenter?.sortOrder) {
                SortManager.SongSort.ALBUM_DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
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
                R.id.action_sort_default -> presenter?.sortOrder = SortManager.SongSort.ALBUM_DEFAULT
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
            android.R.id.home -> activity?.onBackPressed()
            R.id.action_play -> presenter?.playClick(songAdpt.songList)
            R.id.action_play_next -> presenter?.playNextClick(songAdpt.songList)
            R.id.action_queue -> presenter?.queueClick(songAdpt.songList)
            R.id.action_add_to_favorites -> presenter?.addToFavoritesClick(songAdpt.songList)
            R.id.action_add_to_playlist -> presenter?.addToPlaylistClick(songAdpt.songList)
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
        songAdpt.accept(items)
    }

    override fun setCurrentSong(id: Long) {
        songAdpt.currentId = id
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
                expandedSubtitle.fadeOut(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong()/2)
                mIsTheTitleContainerVisible = false
            }

        } else {

            if (!mIsTheTitleContainerVisible) {
                expandedTitle.fadeIn(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong())
                expandedSubtitle.fadeIn(MainActivity.DetailFragments.ALPHA_ANIMATIONS_DURATION.toLong()*2)
                mIsTheTitleContainerVisible = true
            }
        }
    }
}