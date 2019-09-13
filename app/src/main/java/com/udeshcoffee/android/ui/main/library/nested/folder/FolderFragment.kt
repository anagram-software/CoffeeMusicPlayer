package com.udeshcoffee.android.ui.main.library.nested.folder

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.common.adapters.FolderAdapter
import com.udeshcoffee.android.utils.SortManager
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 8/27/2017.
 */

class FolderFragment : androidx.fragment.app.Fragment() {

    private val viewModel: FolderViewModel by inject()

    private lateinit var adapter: FolderAdapter
    private lateinit var path: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_folder, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        view.apply {

            path = findViewById(R.id.folder_path)

            val itemView = findViewById<EmptyRecyclerView>(R.id.folder_list)
            // specify an adapter (see also next example)
            adapter = FolderAdapter()
            val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            itemView.layoutManager = layoutManager
            itemView.setEmptyView(view.findViewById(R.id.empty_view))
            itemView.hasFixedSize()
            itemView.setItemViewCacheSize(20)
            itemView.isNestedScrollingEnabled = false

            adapter.songClickListener = object : OnItemClickListener {
                override fun onItemClick(position: Int) {
                    val pos = position - adapter.folderSet.size
                    viewModel.songItemClicked(pos)
                }

                override fun onItemLongClick(position: Int) {
                    val pos = position - adapter.folderSet.size
                    viewModel.songItemLongClicked(pos)
                }
            }

            adapter.folderClickListener = object : OnItemClickListener {
                override fun onItemClick(position: Int) {
                    viewModel.folderItemClicked(position, layoutManager.findLastVisibleItemPosition())
                }

                override fun onItemLongClick(position: Int) {
                    viewModel.folderItemLongClicked(position)
                }
            }

            itemView.adapter = adapter

            // Actions
            val actionUp = findViewById<ImageButton>(R.id.folder_up)
            actionUp.setOnClickListener { viewModel.upClicked() }

            val actionPlay = findViewById<Button>(R.id.folder_play)
            actionPlay.setOnClickListener { viewModel.playClick() }

            val actionQueue = findViewById<Button>(R.id.folder_queue)
            actionQueue.setOnClickListener { viewModel.queueClick() }
        }
        viewModel.start()
    }

    // Sorting
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_menu, menu)
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
            SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
        }
        when (viewModel.folderSortOrder) {
            SortManager.FolderSort.DEFAULT -> menu.findItem(R.id.action_sort_folder_default).isChecked = true
            SortManager.FolderSort.NAME -> menu.findItem(R.id.action_sort_folder_title).isChecked = true
            SortManager.FolderSort.SONG_COUNT -> menu.findItem(R.id.action_sort_folder_song_count).isChecked = true
        }
        menu.findItem(R.id.action_sort_ascending).isChecked = viewModel.songSortAscending
        menu.findItem(R.id.action_sort_folder_ascending).isChecked = viewModel.folderSortAscending
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var sortChanged = true
        var folderSortChanged = true

        if (item.groupId == R.id.sort_folder) {
            when (item.itemId) {
                R.id.action_sort_folder_default -> viewModel.folderSortOrder = SortManager.AlbumSort.DEFAULT
                R.id.action_sort_folder_title -> viewModel.folderSortOrder = SortManager.AlbumSort.NAME
                R.id.action_sort_folder_song_count -> viewModel.folderSortOrder = SortManager.AlbumSort.YEAR
                R.id.action_sort_folder_ascending -> viewModel.folderSortAscending = !item.isChecked
            }
        } else folderSortChanged = false

        when (item.itemId) {
            R.id.action_sort_default -> viewModel.songSortOrder = SortManager.SongSort.DEFAULT
            R.id.action_sort_title -> viewModel.songSortOrder = SortManager.SongSort.NAME
            R.id.action_sort_track -> viewModel.songSortOrder = SortManager.SongSort.TRACK_NUMBER
            R.id.action_sort_duration -> viewModel.songSortOrder = SortManager.SongSort.DURATION
            R.id.action_sort_year -> viewModel.songSortOrder = SortManager.SongSort.YEAR
            R.id.action_sort_date -> viewModel.songSortOrder = SortManager.SongSort.DATE
            R.id.action_sort_album_name -> viewModel.songSortOrder = SortManager.SongSort.ALBUM_NAME
            R.id.action_sort_artist_name -> viewModel.songSortOrder = SortManager.SongSort.ARTIST_NAME
            R.id.action_sort_ascending -> {
                viewModel.songSortAscending = !item.isChecked
            }
            else -> sortChanged = false
        }

        if (folderSortChanged) {
            viewModel.checkSortAndFetchFolders()
            activity?.invalidateOptionsMenu()
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
            currentSongId.observe(this@FolderFragment, Observer {
                it?.let { adapter.currentId = it }
            })
            path.observe(this@FolderFragment, Observer {
                it?.let { this@FolderFragment.path.text = it }
            })
            songs.observe(this@FolderFragment, Observer {
                it?.let {
                    adapter.accept(songs = it)
                }
            })
            folders.observe(this@FolderFragment, Observer {
                it?.let {
                    adapter.accept(folders = it)
                }
            })

            // Events
            showToast.observe(this@FolderFragment, Observer {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            })
            showSongLongDialog.observe(this@FolderFragment, Observer {
                it?.let { openSongLongDialog(it) }
            })
            showCollectionLongDialog.observe(this@FolderFragment, Observer {
                it?.let { openCollectionLongDialog(it.first, it.second) }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stop()
    }

    companion object {
        fun create() = FolderFragment()
    }
}