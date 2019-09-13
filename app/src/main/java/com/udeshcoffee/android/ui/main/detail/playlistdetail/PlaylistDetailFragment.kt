package com.udeshcoffee.android.ui.main.detail.playlistdetail

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnDragableItemListener
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.ItemTouchHelperCallback
import com.udeshcoffee.android.ui.common.adapters.DragableAdapter
import com.udeshcoffee.android.ui.common.dialogs.DeletePlaylistDialog
import com.udeshcoffee.android.ui.common.dialogs.RenamePlaylistDialog
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 9/12/2017.
*/

class PlaylistDetailFragment : androidx.fragment.app.Fragment() {

    private val viewModel: PlaylistDetailViewModel by inject()

    private lateinit var songAdpt: DragableAdapter
    private lateinit var songView: EmptyRecyclerView
    private var itemHelper: ItemTouchHelper? = null
    private var actionBar: ActionBar? = null
    private var isAddToPlaylistVisible = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_playlist_detail, container, false)

        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@PlaylistDetailFragment.actionBar = supportActionBar
            }

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = arguments!!.getString(ARGUMENT_NAME)
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

            songView = findViewById(R.id.list)
            songView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isNestedScrollingEnabled = false

            // specify an adapter (see also next example)
            songAdpt = DragableAdapter(true)

            songAdpt.listener = object : OnDragableItemListener {
                override fun onItemClick(position: Int) {
                    viewModel.songItemClicked(position)
                }

                override fun onItemLongClick(position: Int) {
                    viewModel.songItemLongClicked(position)
                }

                override fun onItemDrag(holder: RecyclerView.ViewHolder) {
                    itemHelper?.startDrag(holder)
                }
            }
            songView.adapter = songAdpt

            if (arguments!!.getInt(ARGUMENT_TYPE) == Playlist.USER) {
                itemHelper = ItemTouchHelper(ItemTouchHelperCallback(
                        { fromPosition, toPosition -> songAdpt.onItemMove(fromPosition, toPosition) },
                        { fromPosition, toPosition ->
                            if (fromPosition != toPosition) { viewModel.itemMoved(fromPosition, toPosition) }
                        },
                        { viewModel.itemRemoved(songAdpt.dataset[it].playlistSongId) })
                )
                itemHelper?.attachToRecyclerView(songView)
            } else {
                isAddToPlaylistVisible = false
                activity?.invalidateOptionsMenu()
                songAdpt.dragable = false
            }
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.playlist_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_add_to_this_playlist)?.isVisible = isAddToPlaylistVisible
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity?.onBackPressed()
            R.id.action_play -> viewModel.playClick()
            R.id.action_play_next -> viewModel.playNextClick()
            R.id.action_queue -> viewModel.queueClick()
            R.id.action_add_to_playlist -> viewModel.addToPlaylistClick()
            R.id.action_add_to_favorites -> viewModel.addToFavoritesClick()
            R.id.action_rename -> showRenameUI()
            R.id.action_delete -> showDeleteUI()
            R.id.action_add_to_this_playlist -> showAddToPlaylistUI()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            currentSongId.observe(this@PlaylistDetailFragment, Observer {
                it?.let { songAdpt.currentId = it }
            })
            songs.observe(this@PlaylistDetailFragment, Observer {
                it?.let { songAdpt.accept(it) }
            })

            // Events
            showFavoriteToast.observe(this@PlaylistDetailFragment, Observer {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            })
            showAddToPlaylistDialog.observe(this@PlaylistDetailFragment, Observer {
                it?.let { openAddToPlaylistDialog(it as ArrayList<Song>, viewModel.playlistId) }
            })
            showSongLongDialog.observe(this@PlaylistDetailFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.playlistId = arguments!!.getLong(ARGUMENT_ID)
        viewModel.playlistType = arguments!!.getInt(ARGUMENT_TYPE)
        viewModel.playlistTitle = arguments!!.getString(ARGUMENT_NAME, "")
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    private fun showRenameUI() {
        fragmentManager?.let { RenamePlaylistDialog.create(viewModel.playlistId, viewModel.playlistTitle).show(it, "RenameDialog") }
    }

    private fun showDeleteUI() {
        fragmentManager?.let {
            DeletePlaylistDialog.create(viewModel.playlistId, viewModel.playlistTitle, true)
                .show(it, "DeletePlaylistDialog")
        }
    }

    private fun showAddToPlaylistUI() {
        val intent = Intent(context, PlaylistItemAddActivity::class.java)
        intent.putExtra("id", viewModel.playlistId)
        intent.putExtra("title", viewModel.playlistTitle)
        startActivity(intent)
    }

    companion object {
        private const val ARGUMENT_ID = "ARGUMENT_ID"
        private const val ARGUMENT_NAME = "ARGUMENT_NAME"
        private const val ARGUMENT_TYPE = "ARGUMENT_TYPE"

        fun createBundle(playlist: Playlist): Bundle {
            val bundle = Bundle()
            bundle.putLong(ARGUMENT_ID, playlist.id)
            bundle.putInt(ARGUMENT_TYPE, playlist.type)
            bundle.putString(ARGUMENT_NAME, playlist.title)
            return bundle
        }
    }

}