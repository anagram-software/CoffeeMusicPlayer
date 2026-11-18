package com.udeshcoffee.android.ui.main.library.nested

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.showPlayingToast
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.GridItemDecor
import com.udeshcoffee.android.ui.common.adapters.AlbumAdapter
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.utils.SortManager
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 8/27/2017.
 */

class AlbumFragment : Fragment() {

    private val mediaRepository: MediaRepository by inject()

    private var dataJob: Job? = null
    private var albumAdpt: AlbumAdapter? = null

    private var sortOrder: Int
        get() = SortManager.albumsSortOrder
        set(value) {
            SortManager.albumsSortOrder = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.frag_linear, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumView = view.findViewById<EmptyRecyclerView>(R.id.linear_list)
        // specify an adapter (see also next example)
        albumAdpt = AlbumAdapter(AlbumAdapter.ITEM_TYPE_NORMAL)
        albumView.layoutManager = GridLayoutManager(context, resources.getInteger(R.integer.grid_columns),
                RecyclerView.VERTICAL, false)
        albumView.setEmptyView(view.findViewById(R.id.empty_view))
        albumView.addItemDecoration(GridItemDecor(resources.getDimensionPixelSize(R.dimen.grid_spacing)))
        albumView.hasFixedSize()
        albumView.setItemViewCacheSize(20)
        albumView.isNestedScrollingEnabled = false

        albumAdpt?.listener = object : OnGridItemClickListener {
            override fun onItemClick(position: Int, shareElement: View) {
                albumAdpt?.getItem(position)?.let { showDetailUI(it) }
            }

            override fun onItemLongClick(position: Int) {
                albumAdpt?.getItem(position)?.let {
                    viewLifecycleOwner.lifecycleScope.launch {
                                val songs = mediaRepository.loadAlbumSongs(it.id)
                                openCollectionLongDialog(it.title, songs)
                            }
                }
            }

            override fun onItemOptionClick(position: Int) {
                albumAdpt?.getItem(position)?.let {
                    showPlayingToast(it)
                    viewLifecycleOwner.lifecycleScope.launch {
                                val songs = mediaRepository.loadAlbumSongs(it.id)
                                playSong(0, songs, true)
                            }
                }
            }
        }
        albumView.adapter = albumAdpt
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.album_sort, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (sortOrder) {
            SortManager.AlbumSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
            SortManager.AlbumSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
            SortManager.AlbumSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
            SortManager.AlbumSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
        }
        menu.findItem(R.id.action_sort_ascending).isChecked = SortManager.albumsAscending
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var sortChanged = true

        when (item.itemId) {
            R.id.action_sort_default -> sortOrder = SortManager.AlbumSort.DEFAULT
            R.id.action_sort_title -> sortOrder = SortManager.AlbumSort.NAME
            R.id.action_sort_year -> sortOrder = SortManager.AlbumSort.YEAR
            R.id.action_sort_artist_name -> sortOrder = SortManager.AlbumSort.ARTIST_NAME
            R.id.action_sort_ascending -> {
                SortManager.albumsAscending = !item.isChecked
            }
            else -> sortChanged = false
        }

        if (sortChanged) {
            fetchData()
            activity?.invalidateOptionsMenu()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    override fun onPause() {
        super.onPause()
        dispose()
    }

    private fun fetchData() {
        dispose()
        dataJob = viewLifecycleOwner.lifecycleScope.launch {
            mediaRepository.observeAlbums().collect { albums ->
                albumAdpt?.accept(albums.toMutableList().also {
                    SortManager.sortAlbums(it)
                    if (!SortManager.albumsAscending) it.reverse()
                })
            }
        }
    }

    fun dispose() {
        dataJob?.cancel()
    }

    fun showDetailUI(detail: Album) {
        view?.findNavController()?.navigate(R.id.albumDetailFragment, AlbumDetailFragment.createBundle(detail))
    }

    companion object {
        const val TAG = "AlbumFragment"
        fun create(): AlbumFragment = AlbumFragment()
    }
}
