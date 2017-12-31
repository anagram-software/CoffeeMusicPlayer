package com.udeshcoffee.android.ui.main.playlist

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.doSharedTransaction
import com.udeshcoffee.android.interfaces.OnItemClickListener
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.openDrawer
import com.udeshcoffee.android.recyclerview.EmptyRecyclerView
import com.udeshcoffee.android.setRoundColor
import com.udeshcoffee.android.ui.adapters.PlaylistAdapter
import com.udeshcoffee.android.ui.dialogs.CollectionLongDialog
import com.udeshcoffee.android.ui.dialogs.NewPlaylistDialog
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailFragment
import com.udeshcoffee.android.ui.main.detail.playlistdetail.PlaylistDetailPresenter
import com.udeshcoffee.android.utils.Injection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 9/28/2017.
 */
class PlaylistFragment: Fragment() {

    private lateinit var mediaRepository: MediaRepository

    // Lists
    lateinit var playlistAdapter: PlaylistAdapter
    private var disposable: Disposable? = null
    private var actionBar: ActionBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mediaRepository = Injection.provideMediaRepository(context!!.applicationContext)
        return inflater.inflate(R.layout.frag_playlist, container, false)
    }

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
            playlistView.layoutManager = LinearLayoutManager(context)
            playlistAdapter.listener = object : OnItemClickListener {
                override fun onItemClick(position: Int) {
                    showDetailUI(playlistAdapter.getItem(position))
                }

                override fun onItemLongClick(position: Int) {
                    playlistAdapter.getItem(position).let {
                        mediaRepository.getPlaylistSongs(it.id, it.type)
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.take(1)
                                ?.subscribe({ songs ->
                                    val mDialog = CollectionLongDialog()
                                    val bundle = Bundle()
                                    bundle.putString(CollectionLongDialog.ARGUMENT_TITLE, it.title)
                                    bundle.putParcelableArrayList(CollectionLongDialog.ARGUMENT_SONGS, songs as ArrayList<Song>)
                                    if (it.type == Playlist.USER) {
                                        bundle.putParcelable(CollectionLongDialog.ARGUMENT_PLAYLIST, it)
                                    }
                                    mDialog.arguments = bundle
                                    mDialog.show(fragmentManager, "CollectionLongDialog")
                                })
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.playlist_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {activity?.openDrawer()}
            R.id.action_add_playlist -> {
                val mDialog = NewPlaylistDialog()
                mDialog.show(fragmentManager, "NewPlaylistDialog")
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
        val detailFragment = activity!!.supportFragmentManager.findFragmentByTag(MainActivity.Fragments.PLAYLIST_DETAIL)
                as PlaylistDetailFragment? ?: PlaylistDetailFragment()
        PlaylistDetailPresenter(detail, detailFragment, Injection.provideMediaRepository(context!!.applicationContext),
                Injection.provideDataRepository(context!!.applicationContext))
        doSharedTransaction(R.id.main_container, detailFragment, MainActivity.Fragments.PLAYLIST_DETAIL, detail)
    }
}