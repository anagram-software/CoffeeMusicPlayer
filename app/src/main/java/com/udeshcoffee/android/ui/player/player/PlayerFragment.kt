package com.udeshcoffee.android.ui.player.player

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.*
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.MainActivity
import com.udeshcoffee.android.ui.common.dialogs.PlayerMoreDialog
import com.udeshcoffee.android.ui.main.detail.albumdetail.AlbumDetailFragment
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment
import com.udeshcoffee.android.views.CustomScroller
import com.udeshcoffee.android.views.ZoomOutPageTransformer
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 8/25/2017.
 */
class PlayerFragment : androidx.fragment.app.Fragment() {

    val viewModel: PlayerViewModel by inject()

    private lateinit var play: ImageButton
    private lateinit var favorite: ImageButton

    private lateinit var art: ImageView

    private lateinit var progress: SeekBar
    private lateinit var title: TextView
    private lateinit var artistAlbum: TextView
    private lateinit var current: TextView
    private lateinit var duration: TextView
    private lateinit var seekTime: TextView

    // Lyrics
    private var lyricFragment: LyricsFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.frag_player, container, false)

        // Set up player view
        with(root) {

            art = findViewById(R.id.player_art)

            val more = root.findViewById<ImageButton>(R.id.more)
            more.setOnClickListener{ viewModel.currentSong.value?.let { showMoreDialog(it) } }

            favorite = root.findViewById(R.id.favorite)
            favorite.setOnClickListener{ viewModel.favToggle() }

            play = root.findViewById(R.id.play)
            play.setOnClickListener { viewModel.playPauseToggle() }

            val next: ImageButton = root.findViewById(R.id.gotoNext)
            next.setOnClickListener { viewModel.gotoNext() }

            val back: ImageButton = root.findViewById(R.id.gotoBack)
            back.setOnClickListener { viewModel.gotoBack() }

            val lyrics: Button = root.findViewById(R.id.lyrics)
            lyrics.setOnClickListener { viewModel.lyricsToggle() }

            val queue: Button = root.findViewById(R.id.queue)
            queue.setOnClickListener { showQueueUI() }

            progress = findViewById(R.id.progress)
            progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    if (p2)
                        viewModel.seeking(p1.toLong())
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    viewModel.startSeek()
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    p0?.progress?.toLong()?.let { viewModel.seeked(it) }
                }
            })

            title = findViewById(R.id.title)
            artistAlbum = findViewById(R.id.artist_album)
            current = findViewById(R.id.current)
            duration = findViewById(R.id.duration)

            seekTime = findViewById(R.id.seek_time)
        }

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.apply {
            duration.observe(this@PlayerFragment, Observer {
                it?.let {
                    this@PlayerFragment.progress.max = it.toInt()
                    this@PlayerFragment.duration.setTextWithMilliSecondsToTimer(it)
                }
            })
            progress.observe(this@PlayerFragment, Observer {
                it?.let { this@PlayerFragment.progress.progress = it.toInt() }
            })
            current.observe(this@PlayerFragment, Observer {
                it?.let { it1 -> this@PlayerFragment.current.setTextWithMilliSecondsToTimer(it1) }
            })
            currentSong.observe(this@PlayerFragment, Observer {
                it?.let {song ->
                    context?.let { it1 -> song.loadArtwork(it1, art) }
                    title.fadeOut(150) {
                        title.text = song.title
                        title.fadeIn(150)
                    }
                    artistAlbum.fadeOut(200) {
                        artistAlbum.text = String.format("%s • %s", song.artistName, song.albumName)
                        artistAlbum.fadeIn(200)
                    }
                }
            })
            isPlaying.observe(this@PlayerFragment, Observer {
                it?.let {
                    Log.d(TAG, "idplay$it")
                    if (!it) {
                        play.setImageResource(R.drawable.ic_play_white_24dp)
                    } else {
                        play.setImageResource(R.drawable.ic_pause_white_24dp)
                    }
                }
            })
            isSeekTimeShowing.observe(this@PlayerFragment, Observer {
                it?.let {
                    if (it)
                        this@PlayerFragment.seekTime.fadeIn(200)
                    else
                        this@PlayerFragment.seekTime.fadeOut(200)
                }
            })
            seekTime.observe(this@PlayerFragment, Observer {
                it?.let {
                    this@PlayerFragment.seekTime.setTextWithMilliSecondsToTimer(it)
                }
            })
            isFav.observe(this@PlayerFragment, Observer {
                it?.let {
                    if (it) {
                        favorite.setImageResource(R.drawable.ic_favorite_white_24dp)
                    } else {
                        favorite.setImageResource(R.drawable.ic_favorite_border_white_24dp)
                    }
                }
            })
            isLyricsShowing.observe(this@PlayerFragment, Observer {
                it?.let {
                    if (it) {
                        lyricFragment = LyricsFragment.create()
                        replaceFragment(R.id.lyrics_container, lyricFragment!!)
                        art.fadeOut(200)
                    } else {
                        lyricFragment?.let {
                            removeFragment(it)
                        }
                        art.fadeIn(200)
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

    private fun showMoreDialog(song: Song) {
        val mDialog = PlayerMoreDialog()
        val bundle = Bundle()
        bundle.putParcelable(PlayerMoreDialog.ARGUMENT_SONG, song)
        mDialog.arguments = bundle
        fragmentManager?.let { mDialog.show(it, "PlayerMoreDialog") }
    }

    private fun showQueueUI() {
        (activity as MainActivity).showQueue()
    }

    companion object {
        private const val TAG = "PlayerFragment"
    }
}