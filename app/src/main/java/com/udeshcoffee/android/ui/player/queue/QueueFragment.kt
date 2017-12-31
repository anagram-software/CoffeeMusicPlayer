package com.udeshcoffee.android.ui.player.queue

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.udeshcoffee.android.R
import com.udeshcoffee.android.interfaces.OnDragableItemListener
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.ItemTouchHelperCallback
import com.udeshcoffee.android.ui.MiniPlayerActivity
import com.udeshcoffee.android.ui.adapters.DragableAdapter
import com.udeshcoffee.android.ui.dialogs.AddToPlaylistDialog
import com.udeshcoffee.android.utils.DopeUtil
import java.util.*

/**
 * Created by Udathari on 9/16/2017.
 */
class QueueFragment: Fragment(), QueueContract.View {

    val TAG = "QueueFragment"

    var adapter: DragableAdapter? = null
    lateinit var recyclerView: RecyclerView

    override var presenter: QueueContract.Presenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postponeEnterTransition()
        setHasOptionsMenu(true)

        postponeEnterTransition()

        val root = inflater.inflate(R.layout.frag_queue, container, false)

        root?.apply {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                val layout = findViewById<RelativeLayout>(R.id.queue_layout)
                layout.setPadding(0, DopeUtil.getStatusHeight(context), 0, 0)
            }

            val toolbar: Toolbar = findViewById(R.id.toolbar)
            toolbar.apply {
                title = getString(R.string.info_upnext)
                inflateMenu(R.menu.queue_menu)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.action_add_to_playlist -> {
                            presenter?.addToPlaylist()
                        }
                        R.id.action_close -> {
                            presenter?.closeQueue()
                        }
                        R.id.action_clear_queue -> {
                            presenter?.clearQueue()
                        }
                    }
                    return@setOnMenuItemClickListener false
                }
            }

            recyclerView = findViewById<RecyclerView>(R.id.queue_recyclerview)
            // specify an adapter (see also next example)
            adapter = DragableAdapter(true)

            // Touch Handler
            val itemHelper = ItemTouchHelper(ItemTouchHelperCallback(
                    { fromPosition, toPosition -> adapter!!.onItemMove(fromPosition, toPosition) },
                    { fromPosition, toPosition ->
                        if (fromPosition != toPosition) { presenter?.itemMoved(fromPosition, toPosition) }
                    },
                    { presenter?.itemRemoved(it) })
            )
            itemHelper.attachToRecyclerView(recyclerView)

            adapter?.listener = object : OnDragableItemListener{
                override fun onItemClick(position: Int) {
                    presenter?.itemClicked(position)
                }

                override fun onItemLongClick(position: Int) {}

                override fun onItemDrag(holder: RecyclerView.ViewHolder) {
                    itemHelper.startDrag(holder)
                }

            }
            recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            recyclerView.hasFixedSize()
            recyclerView.setItemViewCacheSize(20)
            recyclerView.isDrawingCacheEnabled = true
            recyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            recyclerView.adapter = adapter

        }

        return root
    }

    override fun onStart() {
        super.onStart()
        presenter?.start()
    }

    override fun onStop() {
        super.onStop()
        presenter?.stop()
    }

    override fun showQueue(songs: List<Song>) {
        Log.d(TAG, "showQueue: ${songs.size}")
        adapter?.accept(songs)
    }

    override fun showPlayerUI() {
        (activity as MiniPlayerActivity).showPlayer()
    }

    override fun scrollTo(position: Int) {
        recyclerView.scrollToPosition(position)
    }

    override fun setCurrentSong(position: Int, refreshAll: Boolean) {
        adapter?.currentSong = position
        if (refreshAll)
            adapter?.notifyDataSetChanged()
        else {
            adapter?.notifyItemChanged(position)
        }
    }

    override fun hideOnClearQueue() {
        (activity as MiniPlayerActivity).hideNowPlay()
    }

    override fun showAddToPlaylistUI(songs: List<Song>) {
        val addToPlaylistDialog = AddToPlaylistDialog()
        val bundle = Bundle()
        bundle.putParcelableArrayList(AddToPlaylistDialog.ARGUMENT_SONGS, songs as ArrayList<Song>)
        addToPlaylistDialog.arguments = bundle
        addToPlaylistDialog.show(fragmentManager, "AddToPlaylistDialog")
    }


}