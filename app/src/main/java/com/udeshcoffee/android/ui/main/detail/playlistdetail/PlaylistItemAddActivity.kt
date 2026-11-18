package com.udeshcoffee.android.ui.main.detail.playlistdetail

import com.udeshcoffee.android.extensions.padContentForSystemBars
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.interfaces.OnItemSelectListener
import com.udeshcoffee.android.ui.common.adapters.PlaylistAddAdapter
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 1/14/2018.
*/

class PlaylistItemAddActivity : AppCompatActivity() {

    private val mediaRepository: MediaRepository by inject()
    private var addAdapter: PlaylistAddAdapter? = null
    private var playlistId: Long? = null
    private var songsJob: Job? = null

    private var addDeleteToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_item_add)
        padContentForSystemBars()

        val intent = intent
        val title = intent.getStringExtra("title")
        playlistId = intent.getLongExtra("id", 0)

        val toolbar = findViewById<Toolbar>(R.id.playlistitemadd_actionbar)
        setSupportActionBar(toolbar)

        val ab = supportActionBar

        ab!!.setDisplayHomeAsUpEnabled(true)
        ab.title = "Add to " + title

        val count = findViewById<TextView>(R.id.playlistitemadd_count)

        val songView = findViewById<RecyclerView>(R.id.playlistitemadd_list)
        //songView.setNestedScrollingEnabled(false);
        addAdapter = PlaylistAddAdapter(count)

        songView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
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
        songsJob = lifecycleScope.launch {
            mediaRepository.observeSongs().collect { addAdapter?.accept(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        songsJob?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        this.finish()
        return true
    }

    fun addToPlaylist(audioId: Long) {
        val id = playlistId ?: return
        lifecycleScope.launch { mediaRepository.addSongToPlaylist(id, audioId) }
        showToast("Added to playlist")
    }

    fun deletePlaylistTracks(audioId: Long) {
        val id = playlistId ?: return
        lifecycleScope.launch { mediaRepository.removeSongFromPlaylist(id, audioId) }
        showToast("Removed from playlist")
    }

    private fun showToast(message: String) {
        addDeleteToast?.cancel()
        addDeleteToast = Toast.makeText(this, message, Toast.LENGTH_SHORT).also { it.show() }
    }
}
