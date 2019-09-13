package com.udeshcoffee.android.ui.miniplayer

import androidx.lifecycle.Observer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.loadArtwork
import com.udeshcoffee.android.ui.MainActivity
import com.udeshcoffee.android.views.FadableLayout
import org.koin.android.ext.android.inject


/**
* Created by Udathari on 8/25/2017.
*/
class MiniPlayerFragment : androidx.fragment.app.Fragment() {

    private val viewModel: MiniPlayerViewModel by inject()

    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var art: ImageView
    private lateinit var playPause: ImageButton
    private lateinit var progress: ProgressBar
    private lateinit var layout: FadableLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_mini_player, container, false)

        // Set up player view
        with(root) {
            title = findViewById(R.id.mini_player_title)
            subtitle = findViewById(R.id.mini_player_subtitle)
            art = findViewById(R.id.mini_player_art)
            layout = findViewById(R.id.mini_player_layout)
            layout.setOnClickListener { showNowPlayUI() }
            playPause = findViewById(R.id.mini_player_playpause)
            playPause.setOnClickListener { viewModel.playPauseToggle() }
            progress = findViewById(R.id.mini_player_progress)
        }

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            duration.observe(this@MiniPlayerFragment, Observer {
                it?.let { this@MiniPlayerFragment.progress.max = it.toInt() }
            })
            progress.observe(this@MiniPlayerFragment, Observer {
                it?.let { this@MiniPlayerFragment.progress.progress = it.toInt() }
            })
            currentSong.observe(this@MiniPlayerFragment, Observer {
                it?.let {
                    (activity as MainActivity).initNowPlay()
                    title.text = it.title
                    subtitle.text = String.format("%s • %s", it.artistName, it.albumName)
                    it.loadArtwork(context!!, art, layout)
                }
            })
            isPlaying.observe(this@MiniPlayerFragment, Observer {
                it?.let {
                    if (!it) {
                        playPause.setImageResource(R.drawable.ic_play)
                    } else {
                        playPause.setImageResource(R.drawable.ic_pause)
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    fun setAlpha(alpha: Float) {
        layout.alpha = alpha
    }

    fun setVisibility(visibility: Int) {
        layout.visibility = visibility
    }

    private fun showNowPlayUI() {
        (activity as MainActivity).openNowPlay()
    }

}