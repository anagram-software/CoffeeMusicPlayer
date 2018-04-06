package com.udeshcoffee.android.ui.player.queue

import android.arch.lifecycle.Observer
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnDragableItemListener
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.ItemTouchHelperCallback
import com.udeshcoffee.android.ui.MiniPlayerActivity
import com.udeshcoffee.android.ui.common.adapters.DragableAdapter
import com.udeshcoffee.android.utils.DopeUtil
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 9/16/2017.
*/
class QueueFragment: Fragment() {

    private lateinit var adapter: DragableAdapter
    private lateinit var recyclerView: RecyclerView

    private val viewModel: QueueViewModel by inject()

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
                            viewModel.addToPlaylist()
                        }
                        R.id.action_close -> {
                            viewModel.closeQueue()
                        }
                        R.id.action_clear_queue -> {
                            viewModel.clearQueue()
                        }
                    }
                    return@setOnMenuItemClickListener false
                }
            }

            recyclerView = findViewById(R.id.queue_recyclerview)
            adapter = DragableAdapter(true)

            // Touch Handler
            val itemHelper = ItemTouchHelper(ItemTouchHelperCallback(
                    { fromPosition, toPosition -> adapter.onItemMove(fromPosition, toPosition) },
                    { fromPosition, toPosition ->
                        if (fromPosition != toPosition) {
                            viewModel.itemMoved(fromPosition, toPosition)
                        }
                    },
                    { viewModel.itemRemoved(it) })
            )
            itemHelper.attachToRecyclerView(recyclerView)

            adapter.listener = object : OnDragableItemListener{
                override fun onItemClick(position: Int) {
                    viewModel.queueItemClicked(position)
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            currentSongId.observe(this@QueueFragment, Observer {
                it?.let { adapter.currentId = it }
            })
            songs.observe(this@QueueFragment, Observer {
                it?.let { adapter.accept(it) }
            })

            // Events
            showSongLongDialog.observe(this@QueueFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
            showPlayerUI.observe(this@QueueFragment, Observer {
                (activity as MiniPlayerActivity).showPlayer()
            })
            scrollTo.observe(this@QueueFragment, Observer {
                it?.let { it1 -> recyclerView.scrollToPosition(it1) }
            })
            hideNowPlay.observe(this@QueueFragment, Observer {
                (activity as MiniPlayerActivity).hideNowPlay()
            })
            showAddToPlaylist.observe(this@QueueFragment, Observer {
                it?.let { openAddToPlaylistDialog(it as ArrayList<Song>) }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.start()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
    }
}