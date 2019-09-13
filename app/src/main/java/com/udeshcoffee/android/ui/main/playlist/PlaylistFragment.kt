package com.udeshcoffee.android.ui.main.playlist

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.transition.Fade
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.openDrawer
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.ui.common.adapters.PlaylistAdapter
import com.udeshcoffee.android.ui.common.dialogs.CollectionLongDialog
import com.udeshcoffee.android.ui.common.dialogs.NewPlaylistDialog
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 9/28/2017.
*/
class PlaylistFragment: androidx.fragment.app.Fragment() {

    private val mediaRepository: MediaRepository by inject()

    // Lists
    lateinit var playlistAdapter: PlaylistAdapter
    private var disposable: Disposable? = null
    private var actionBar: ActionBar? = null

    init {
        enterTransition = Fade()
        exitTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_playlist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        view.apply {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).setSupportActionBar(toolbar)
            actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.apply {
                title = ""
                setHomeButtonEnabled(true)
            }

            val titleView = findViewById<TextView>(R.id.title_view)
            titleView.text = getString(R.string.info_playlists).toUpperCase()
            titleView.setRoundColor(R.color.playlistAccent)

            val playlistView = findViewById<EmptyRecyclerView>(R.id.list)
            playlistAdapter = PlaylistAdapter()
            playlistView.adapter = playlistAdapter
            playlistView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            playlistAdapter.listener = object : OnItemClickListener {
                override fun onItemClick(position: Int) {
                    showDetailUI(playlistAdapter.getItem(position))
                }

                override fun onItemLongClick(position: Int) {
                    playlistAdapter.getItem(position).let {
                        mediaRepository.getPlaylistSongs(it.id, it.type)
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.take(1)
                                ?.subscribe { songs ->
                                    val mDialog = CollectionLongDialog()
                                    val bundle = Bundle()
                                    bundle.putString(CollectionLongDialog.ARGUMENT_TITLE, it.title)
                                    bundle.putParcelableArrayList(CollectionLongDialog.ARGUMENT_SONGS, songs as ArrayList<Song>)
                                    if (it.type == Playlist.USER) {
                                        bundle.putParcelable(CollectionLongDialog.ARGUMENT_PLAYLIST, it)
                                    }
                                    mDialog.arguments = bundle
                                    fragmentManager?.let { it1 -> mDialog.show(it1, "CollectionLongDialog") }
                                }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.playlist_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {activity?.openDrawer()}
            R.id.action_add_playlist -> {
                fragmentManager?.let { NewPlaylistDialog.create().show(it, "NewPlaylistDialog") }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        dispose()
    }

    fun dispose() {
        disposable?.let {
            if (it.isDisposed)
                it.dispose()
        }
    }

    override fun onResume() {
        super.onResume()
        dispose()
        disposable = mediaRepository.getPlaylists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlistAdapter)
    }

    fun showDetailUI(detail: Playlist) {
        view?.findNavController()?.navigate(R.id.playlistDetailFragment, PlaylistDetailFragment.createBundle(detail))
    }

    companion object {
        fun create() = PlaylistFragment()
    }
}