package com.udeshcoffee.android.ui.main.detail.playlistdetail

import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.interfaces.OnItemSelectListener
import com.udeshcoffee.android.ui.common.adapters.PlaylistAddAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 1/14/2018.
*/

class PlaylistItemAddActivity : AppCompatActivity() {

    private val mediaRepository: MediaRepository by inject()
    private var addAdapter: PlaylistAddAdapter? = null
    private var playlistId: Long? = null
    private var disposable: Disposable? = null

    private var addDeleteToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_item_add)

        val intent = intent
        val title = intent.getStringExtra("title")
        playlistId = intent.getLongExtra("id", 0)

        val toolbar = findViewById<Toolbar>(R.id.playlistitemadd_actionbar)
        setSupportActionBar(toolbar)

        val ab = supportActionBar

        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.title = "Add to " + title

        val count = findViewById<TextView>(R.id.playlistitemadd_count)

        val songView = findViewById<FastScrollRecyclerView>(R.id.playlistitemadd_list)
        //songView.setNestedScrollingEnabled(false);
        addAdapter = PlaylistAddAdapter(count)

        songView.layoutManager = LinearLayoutManager(this)
        songView.adapter = addAdapter
        addAdapter!!.listener = object : OnItemSelectListener {
            override fun onSelectItem(postion: Int) {
                addToPlaylist(addAdapter!!.getItem(postion).id)
            }

            override fun onDeselectItem(postion: Int) {
                deletePlaylistTracks(addAdapter!!.getItem(postion).id)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        disposable = mediaRepository.getSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(addAdapter!!)
    }

    override fun onPause() {
        super.onPause()
        if (disposable != null && !disposable!!.isDisposed)
            disposable!!.dispose()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        this.finish()
        return true
    }

    fun addToPlaylist(audioId: Long) {
        val resolver = this.contentResolver
        val cols = arrayOf("count(*)")
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId!!)
        val cur = resolver.query(uri, cols, null, null, null)
        var base = 0
        if (cur != null) {
            cur.moveToFirst()
            base = cur.getInt(0)
            cur.close()
        }
        val values = ContentValues()
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base)
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId)
        resolver.insert(uri, values)
        if (addDeleteToast != null)
            addDeleteToast!!.cancel()
        addDeleteToast = Toast.makeText(this, "Added to playlist", Toast.LENGTH_SHORT)
        addDeleteToast!!.show()
    }

    fun deletePlaylistTracks(audioId: Long) {
        val resolver = this.contentResolver
        try {
            val uri = MediaStore.Audio.Playlists.Members.getContentUri(
                    "external", playlistId!!)
            val where = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?" // my mistake was I used .AUDIO_ID here

            val audioId1 = java.lang.Long.toString(audioId)
            val whereVal = arrayOf(audioId1)
            resolver.delete(uri, where, whereVal)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (addDeleteToast != null)
            addDeleteToast!!.cancel()
        addDeleteToast = Toast.makeText(this, "Removed from playlist", Toast.LENGTH_SHORT)
        addDeleteToast!!.show()
    }
}
