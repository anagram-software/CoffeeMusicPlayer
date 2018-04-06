package com.udeshcoffee.android.ui.main.library.nested

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.bumptech.glide.Glide
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.navigateToDetail
import com.udeshcoffee.android.extensions.openCollectionLongDialog
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.showPlayingToast
import com.udeshcoffee.android.interfaces.OnGridItemClickListener
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.recyclerview.GridItemDecor
import com.udeshcoffee.android.ui.common.adapters.ArtistAdapter
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import java.util.*

/**
 * Created by Udathari on 8/27/2017.
 */

class ArtistFragment : Fragment() {

    val TAG = this.javaClass.simpleName

    private val mediaRepository: MediaRepository by inject()

    private var disposable : Disposable? = null
    private var artistAdpt : ArtistAdapter? = null

    private var sortOrder: Int
        get() = SortManager.artistSortOrder
        set(value) {SortManager.artistSortOrder = value}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.frag_linear, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumView = view.findViewById<EmptyRecyclerView>(R.id.linear_list)
        // specify an adapter (see also next example)
        artistAdpt = ArtistAdapter(ArtistAdapter.ITEM_TYPE_NORMAL, Glide.with(context), true)
        albumView.layoutManager = GridLayoutManager(context,  resources.getInteger(R.integer.grid_columns),
                GridLayoutManager.VERTICAL, false)
        albumView.setEmptyView(view.findViewById(R.id.empty_view))
        albumView.addItemDecoration(GridItemDecor(resources.getDimensionPixelSize(R.dimen.grid_spacing)))
        albumView.hasFixedSize()
        albumView.setItemViewCacheSize(20)
        albumView.isDrawingCacheEnabled = true
        albumView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
        albumView.isNestedScrollingEnabled = false

        artistAdpt?.listener = object : OnGridItemClickListener {
            override fun onItemClick(position: Int, shareElement: View) {
                artistAdpt?.getItem(position)?.let { showDetailUI(it) }
            }

            override fun onItemLongClick(position: Int) {
                artistAdpt?.getItem(position)?.let {
                    mediaRepository.getArtistSongs(it.id)
                            .observeOn(AndroidSchedulers.mainThread())
                            .take(1)
                            .subscribe({ songs ->
                                openCollectionLongDialog(it.name, songs)
                            })
                }
            }

            override fun onItemOptionClick(position: Int) {
                artistAdpt?.getItem(position)?.let {
                    showPlayingToast(it)
                    mediaRepository.getArtistSongs(it.id)
                            .observeOn(AndroidSchedulers.mainThread())
                            .firstOrError()
                            .subscribe({songs ->
                                playSong(0, songs, true)
                            }, {})
                }
            }
        }
        albumView.adapter = artistAdpt
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.artist_sort, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        if (menu != null) {
            when (sortOrder) {
                SortManager.ArtistSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
                SortManager.ArtistSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
            }
            menu.findItem(R.id.action_sort_ascending).isChecked = SortManager.artistsAscending
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        var sortChanged = true

        when (item?.itemId) {
            R.id.action_sort_default -> sortOrder = SortManager.ArtistSort.DEFAULT
            R.id.action_sort_title -> sortOrder = SortManager.ArtistSort.NAME
            R.id.action_sort_ascending -> { SortManager.artistsAscending = !item.isChecked }
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
        disposable = mediaRepository.getArtists()
                .observeOn(AndroidSchedulers.mainThread())
                .map({ artists ->
                    SortManager.sortArtists(artists)

                    if (!SortManager.artistsAscending) {
                        Collections.reverse(artists)
                    }

                    return@map artists
                })
                .subscribe{artistAdpt?.accept(it)}
    }

    fun dispose(){
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun showDetailUI(detail: Artist) {
        activity?.supportFragmentManager?.navigateToDetail(detail)
    }

    companion object {
        fun create(): ArtistFragment = ArtistFragment()
    }

}
