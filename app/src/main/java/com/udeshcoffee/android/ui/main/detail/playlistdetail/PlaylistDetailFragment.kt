package com.udeshcoffee.android.ui.main.detail.playlistdetail

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnDragableItemListener
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.openAddToPlaylistDialog
import com.udeshcoffee.android.openSongLongDialog
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.ItemTouchHelperCallback
import com.udeshcoffee.android.ui.adapters.DragableAdapter
import com.udeshcoffee.android.ui.dialogs.DeletePlaylistDialog
import com.udeshcoffee.android.ui.dialogs.RenamePlaylistDialog
import com.udeshcoffee.android.ui.main.MainActivity

/**
 * Created by Udathari on 9/12/2017.
 */
class PlaylistDetailFragment : Fragment(), PlaylistDetailContract.View {

    override var presenter: PlaylistDetailContract.Presenter? = null

    lateinit var songAdpt: DragableAdapter
    lateinit var songView: EmptyRecyclerView
    var itemHelper: ItemTouchHelper? = null
    var actionBar: ActionBar? = null
    var isAddToPlaylistVisible = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_playlist_detail, container, false)

        val name = arguments!!.getString(MainActivity.DetailFragments.ARGUMENT_NAME)

        with(root) {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@PlaylistDetailFragment.actionBar = supportActionBar
            }

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = name
            }
            setHasOptionsMenu(true)

            songView = findViewById(R.id.list)
            songView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            songView.setEmptyView(findViewById(R.id.empty_view))
            songView.hasFixedSize()
            songView.setItemViewCacheSize(20)
            songView.isDrawingCacheEnabled = true
            songView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            songView.isNestedScrollingEnabled = false

            // specify an adapter (see also next example)
            songAdpt = DragableAdapter(true)

            songAdpt.listener = object : OnDragableItemListener {
                override fun onItemClick(position: Int) {
                    presenter?.itemClicked(position, songAdpt.dataset)
                }

                override fun onItemLongClick(position: Int) {
                    presenter?.itemLongClicked(songAdpt.dataset[position])
                }

                override fun onItemDrag(holder: RecyclerView.ViewHolder) {
                    itemHelper?.startDrag(holder)
                }
            }
            songView.adapter = songAdpt
        }

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.playlist_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.action_add_to_this_playlist)?.isVisible = isAddToPlaylistVisible
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> activity?.onBackPressed()
            R.id.action_play -> presenter?.playClick(songAdpt.dataset)
            R.id.action_play_next -> presenter?.playNextClick(songAdpt.dataset)
            R.id.action_queue -> presenter?.queueClick(songAdpt.dataset)
            R.id.action_add_to_playlist -> presenter?.addToPlaylistClick(songAdpt.dataset)
            R.id.action_add_to_favorites -> presenter?.addToFavoritesClick(songAdpt.dataset)
            R.id.action_rename -> presenter?.rename()
            R.id.action_delete -> presenter?.delete()
            R.id.action_add_to_this_playlist -> presenter?.addToPlaylist()
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

    override fun setPlaylist(isEditable: Boolean) {
        if (isEditable) {
            itemHelper = ItemTouchHelper(ItemTouchHelperCallback(
                    { fromPosition, toPosition -> songAdpt.onItemMove(fromPosition, toPosition) },
                    { fromPosition, toPosition ->
                        if (fromPosition != toPosition) { presenter?.itemMoved(fromPosition, toPosition) }
                    },
                    { presenter?.itemRemoved(songAdpt.dataset[it].playlistSongId, it) })
            )
            itemHelper?.attachToRecyclerView(songView)
        } else {
            isAddToPlaylistVisible = false
            activity?.supportInvalidateOptionsMenu()
            songAdpt.dragable = false
        }
    }

    // Redundent
    override fun showAddToPlaylistDialog(songs: ArrayList<Song>) {}

    override fun showAddToPlaylistDialog(songs: ArrayList<Song>, thisPlaylistId: Long?) {
        openAddToPlaylistDialog(songs, thisPlaylistId)
    }

    override fun showFavoritesToast(isFavorite: Boolean) {
        if (isFavorite)
            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
    }

    override fun showRenameUI(title: String, id: Long) {
        val mDialog = RenamePlaylistDialog()
        val bundle1 = Bundle()
        bundle1.putString(RenamePlaylistDialog.ARGUMENT_TITLE, title)
        bundle1.putLong(RenamePlaylistDialog.ARGUMENT_ID, id)
        mDialog.arguments = bundle1
        mDialog.show(fragmentManager, "RenameDialog")
    }

    override fun showDeleteUI(playlist: Playlist) {
        val mDialog = DeletePlaylistDialog()
        val bundle1 = Bundle()
        bundle1.putParcelable(DeletePlaylistDialog.ARGUMENT_PLAYLIST, playlist)
        mDialog.arguments = bundle1
        mDialog.show(fragmentManager, "DeletePlaylistDialog")
    }

    override fun showSongLongDialog(song: Song) {
        openSongLongDialog(song)
    }

    override fun showAddToPlaylistUI(id: Long, title: String) {
        val intent = Intent(context, PlaylistItemAddActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("id", id)
        startActivity(intent)
    }

}