package com.udeshcoffee.android.ui.miniplayer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.loadArtwork
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.MiniPlayerActivity
import com.udeshcoffee.android.views.FadableLayout


/**
 * Created by Udathari on 8/25/2017.
 */
class MiniPlayerFragment : Fragment(), MiniPlayerContract.View {

    val TAG = "MiniPlayerFragment"

    override var presenter: MiniPlayerContract.Presenter? = null
    lateinit var title: TextView
    lateinit var subtitle: TextView
    lateinit var art: ImageView
    lateinit var playPause: ImageButton
    lateinit var progress: ProgressBar
    lateinit var layout: FadableLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.frag_mini_player, container, false)

        // Set up player view
        with(root) {
            title = findViewById(R.id.mini_player_title)
            subtitle = findViewById(R.id.mini_player_subtitle)
            art = findViewById(R.id.mini_player_art)
            layout = findViewById(R.id.mini_player_layout)
            layout.setOnClickListener { presenter?.openNowPlay() }
            playPause = findViewById(R.id.mini_player_playpause)
            playPause.setOnClickListener { presenter?.playPauseToggle() }
            progress = findViewById(R.id.mini_player_progress)
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        presenter?.start()
    }

    override fun onPause() {
        super.onPause()
        presenter?.stop()
    }

    override fun initProgress(max: Long) {
        progress.max = max.toInt()
    }

    override fun setProgress(duration: Long) {
        progress.progress = duration.toInt()
    }

    override fun setDetails(song: Song) {
        (activity as MiniPlayerActivity).initNowPlay()
        title.text = song.title
        subtitle.text = String.format("%s • %s", song.artistName, song.albumName)
        song.loadArtwork(context!!, art, layout)
    }

    override fun setAlpha(alpha: Float) {
        layout.alpha = alpha
    }

    override fun setVisibility(visibility: Int) {
        layout.visibility = visibility
    }

    override fun setPlayOrPause(isPlay: Boolean) {
        Log.d(TAG, "idplay" + isPlay)
        if (isPlay) {
            playPause.setImageResource(R.drawable.ic_play)
        } else {
            playPause.setImageResource(R.drawable.ic_pause)
        }
    }

    override fun showNowPlayUI() {
        (activity as MiniPlayerActivity).openNowPlay()
    }

}