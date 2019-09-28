package com.udeshcoffee.android.ui.player.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnDragableItemListener
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.ItemTouchHelperCallback
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.MainActivity
import com.udeshcoffee.android.ui.common.adapters.DragableAdapter
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 9/16/2017.
 */
class QueueFragment : androidx.fragment.app.Fragment() {

    private lateinit var repeat: ImageButton
    private lateinit var shuffle: ImageButton
    private lateinit var queueSize: TextView

    private lateinit var adapter: DragableAdapter
    private lateinit var recyclerView: RecyclerView

    private val viewModel: QueueViewModel by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postponeEnterTransition()
        setHasOptionsMenu(true)

        postponeEnterTransition()

        val root = inflater.inflate(R.layout.frag_queue, container, false)

        root?.apply {

            val toolbar: Toolbar = findViewById(R.id.toolbar)
            toolbar.apply {
                title = getString(R.string.info_upnext)
                inflateMenu(R.menu.queue_menu)
                setOnMenuItemClickListener {
                    when (it.itemId) {
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

            repeat = root.findViewById(R.id.repeat)
            repeat.setOnClickListener { viewModel.changeRepeatMode() }

            shuffle = root.findViewById(R.id.shuffle)
            shuffle.setOnClickListener { viewModel.shuffle() }
            queueSize = findViewById(R.id.queue_size)

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

            adapter.listener = object : OnDragableItemListener {
                override fun onItemClick(position: Int) {
                    viewModel.queueItemClicked(position)
                }

                override fun onItemLongClick(position: Int) {}

                override fun onItemDrag(holder: RecyclerView.ViewHolder) {
                    itemHelper.startDrag(holder)
                }

            }
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            recyclerView.hasFixedSize()
            recyclerView.setItemViewCacheSize(20)
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
            playPosition.observe(this@QueueFragment, Observer {
                it?.let {
                    this@QueueFragment.queueSize.text = String.format("%d song of %d", it + 1, total)
                }
            })
            repeatMode.observe(this@QueueFragment, Observer {
                it?.let {
                    when (it) {
                        MusicService.RepeatMode.NONE -> {
                            repeat.setImageResource(R.drawable.ic_repeat_white_24dp)
                            repeat.alpha = 0.3f
                        }
                        MusicService.RepeatMode.ONE -> {
                            repeat.setImageResource(R.drawable.ic_repeat_one_white_24dp)
                            repeat.alpha = 1f
                        }
                        MusicService.RepeatMode.ALL -> {
                            repeat.setImageResource(R.drawable.ic_repeat_white_24dp)
                            repeat.alpha = 1f
                        }
                    }
                }
            })
            isShuffle.observe(this@QueueFragment, Observer {
                it?.let {
                    if (!it) {
                        shuffle.alpha = 0.3f
                    } else {
                        shuffle.alpha = 1.0f
                    }
                }
            })

            // Events
            showSongLongDialog.observe(this@QueueFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
            showPlayerUI.observe(this@QueueFragment, Observer {
                (activity as MainActivity).showPlayer()
            })
            scrollTo.observe(this@QueueFragment, Observer {
                it?.let { it1 -> recyclerView.scrollToPosition(it1) }
            })
            hideNowPlay.observe(this@QueueFragment, Observer {
                (activity as MainActivity).hideNowPlay()
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