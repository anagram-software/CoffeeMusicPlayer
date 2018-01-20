package com.udeshcoffee.android.ui.main.library.nested.folder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openAddToPlaylistDialog
import com.udeshcoffee.android.extensions.openSongLongDialog
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Folder
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.adapters.FolderAdapter
import com.udeshcoffee.android.ui.dialogs.CollectionLongDialog
import io.reactivex.Observable
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 8/27/2017.
*/

class FolderFragment : Fragment(), FolderContract.View {

    val TAG = "FolderFragment"

    override val presenter: FolderContract.Presenter by inject()

    internal var adapter: FolderAdapter? = null
    lateinit var path: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_folder, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.apply {

            path = findViewById(R.id.folder_path)

            val itemView = findViewById<EmptyRecyclerView>(R.id.folder_list)
            // specify an adapter (see also next example)
            adapter = FolderAdapter()
            val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            itemView.layoutManager = layoutManager
            itemView.setEmptyView(view.findViewById(R.id.empty_view))
            itemView.hasFixedSize()
            itemView.setItemViewCacheSize(20)
            itemView.isDrawingCacheEnabled = true
            itemView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_AUTO
            itemView.isNestedScrollingEnabled = false

            adapter?.songClickListener = object : OnItemClickListener {
                override fun onItemClick(position: Int) {
                    // Change position to match songsets position
                    val pos = position - adapter?.folderSet?.size!!
                    adapter?.let { presenter.itemClicked(pos, it.songSet) }
                }

                override fun onItemLongClick(position: Int) {
                    // Change position to match songsets position
                    val pos = position - adapter?.folderSet?.size!!
                    adapter?.let { presenter.itemLongClicked(it.songSet[pos]) }
                }
            }

            adapter?.folderClickListener = object : OnItemClickListener {
                override fun onItemClick(position: Int) {
                    adapter?.let { presenter.folderItemClicked(it.folderSet[position], layoutManager.findLastVisibleItemPosition()) }
                }

                override fun onItemLongClick(position: Int) {
                    adapter?.let { presenter.folderItemLongClicked(it.folderSet[position]) }
                }
            }

            itemView.adapter = adapter

            // Actions
            val actionUp = findViewById<ImageButton>(R.id.folder_up)
            actionUp.setOnClickListener { presenter.upClicked() }

            val actionPlay = findViewById<Button>(R.id.folder_play)
            actionPlay.setOnClickListener { adapter?.songSet?.let { it1 -> presenter.playClick(it1) } }

            val actionQueue = findViewById<Button>(R.id.folder_queue)
            actionQueue.setOnClickListener { adapter?.songSet?.let { it1 -> presenter.queueClick(it1) } }
        }

        presenter.view = this
        presenter.start()
    }

    // Sorting

//    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
//        inflater?.inflate(R.menu.song_sort, menu)
//        super.onCreateOptionsMenu(menu, inflater)
//    }
//
//    override fun onPrepareOptionsMenu(menu: Menu?) {
//        if (menu != null) {
//            when (presenter?.sortOrder) {
//                SortManager.SongSort.DEFAULT -> menu.findItem(R.id.action_sort_default).isChecked = true
//                SortManager.SongSort.NAME -> menu.findItem(R.id.action_sort_title).isChecked = true
//                SortManager.SongSort.TRACK_NUMBER -> menu.findItem(R.id.action_sort_track).isChecked = true
//                SortManager.SongSort.DURATION -> menu.findItem(R.id.action_sort_duration).isChecked = true
//                SortManager.SongSort.DATE -> menu.findItem(R.id.action_sort_date).isChecked = true
//                SortManager.SongSort.YEAR -> menu.findItem(R.id.action_sort_year).isChecked = true
//                SortManager.SongSort.ALBUM_NAME -> menu.findItem(R.id.action_sort_album_name).isChecked = true
//                SortManager.SongSort.ARTIST_NAME -> menu.findItem(R.id.action_sort_artist_name).isChecked = true
//            }
//        }
//        super.onPrepareOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
//        var sortChanged = true
//
//        when (item?.itemId) {
//            R.id.action_sort_default -> presenter?.sortOrder = SortManager.SongSort.DEFAULT
//            R.id.action_sort_title -> presenter?.sortOrder = SortManager.SongSort.NAME
//            R.id.action_sort_track -> presenter?.sortOrder = SortManager.SongSort.TRACK_NUMBER
//            R.id.action_sort_duration -> presenter?.sortOrder = SortManager.SongSort.DURATION
//            R.id.action_sort_year -> presenter?.sortOrder = SortManager.SongSort.YEAR
//            R.id.action_sort_date -> presenter?.sortOrder = SortManager.SongSort.DATE
//            R.id.action_sort_album_name -> presenter?.sortOrder = SortManager.SongSort.ALBUM_NAME
//            R.id.action_sort_artist_name -> presenter?.sortOrder = SortManager.SongSort.ARTIST_NAME
//            R.id.action_sort_ascending -> {
//            }
//            else -> sortChanged = false
//        }
//
//        if (sortChanged) {
//            presenter?.fetchData()
//            activity.supportInvalidateOptionsMenu()
//        }
//
//        return super.onOptionsItemSelected(item)
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.stop()
    }

    override fun setPath(path: String) {
        this.path.text = path
    }

    override fun showHideLoading(show: Boolean) {
    }

    override fun showErrorToast(error: String) {
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }

    override fun populateItems(items: List<Folder>, scrollTo: Int) {
        adapter?.accept(folders = items)
    }

    override fun populateItems(items: List<Song>) {
        adapter?.accept(songs = items)
    }

    override fun setCurrentSong(id: Long) {
        adapter?.currentId = id
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

    override fun showCollectionLongDialog(title: String, items: Observable<List<Song>>) {
        items.take(1)
                .subscribe({ songs ->
                    val mDialog = CollectionLongDialog()
                    val bundle = Bundle()
                    bundle.putString(CollectionLongDialog.ARGUMENT_TITLE, title)
                    bundle.putParcelableArrayList(CollectionLongDialog.ARGUMENT_SONGS, songs as ArrayList<Song>)
                    mDialog.arguments = bundle
                    mDialog.show(fragmentManager, "CollectionLongDialog")
                })
    }

    companion object {
        fun create() = FolderFragment()
    }
}