package com.udeshcoffee.android.ui.player.player

import android.arch.lifecycle.Observer
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.*
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.MiniPlayerActivity
import com.udeshcoffee.android.ui.common.adapters.PlayerArtAdapter
import com.udeshcoffee.android.ui.common.dialogs.PlayerMoreDialog
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment
import com.udeshcoffee.android.utils.DopeUtil
import com.udeshcoffee.android.views.CustomScroller
import com.udeshcoffee.android.views.ZoomOutPageTransformer
import org.koin.android.ext.android.inject


/**
 * Created by Udathari on 8/25/2017.
 */
class PlayerFragment : Fragment() {

    val viewModel: PlayerViewModel by inject()

    private lateinit var play: ImageButton
    private lateinit var repeat: ImageButton
    private lateinit var shuffle: ImageButton
    private lateinit var favorite: ImageButton

    private lateinit var art: ViewPager

    private lateinit var progress: SeekBar
    private lateinit var title: TextView
    private lateinit var artistAlbum: TextView
    private lateinit var current: TextView
    private lateinit var duration: TextView
    private lateinit var queueSize: TextView
    private lateinit var seekTime: TextView

    private lateinit var playerArtPagerAdapter: PlayerArtAdapter

    // Lyrics
    private var lyricFragment: LyricsFragment? = null

    // Art change listener
    private val pagerChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {}

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            viewModel.artScrolled(position)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.frag_player, container, false)

        // Set up player view
        with(root) {
            val toolbar: Toolbar = findViewById(R.id.toolbar)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                val layout = findViewById<ConstraintLayout>(R.id.player_layout)
                layout.setPadding(0, DopeUtil.getStatusHeight(context), 0, 0)
            }

            toolbar.apply {
                title = ""
                inflateMenu(R.menu.player_menu)
                setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.action_queue -> {
                            showQueueUI()
                        }
                        R.id.action_toggle_lyrics -> {
                            viewModel.lyricsToggle()
                        }
                    }
                    return@setOnMenuItemClickListener false
                }
                setNavigationOnClickListener { showMainUI() }
            }

            art = findViewById(R.id.art_container)
            art.addOnPageChangeListener(pagerChangeListener)
            playerArtPagerAdapter = PlayerArtAdapter(context)
            art.adapter = playerArtPagerAdapter
            setupViewPager()

            val more = root.findViewById<ImageButton>(R.id.more)
            more.setOnClickListener{ viewModel.currentSong.value?.let { showMoreDialog(it) } }

            favorite = root.findViewById(R.id.favorite)
            favorite.setOnClickListener{ viewModel.favToggle() }

            repeat = root.findViewById(R.id.repeat)
            repeat.setOnClickListener { viewModel.changeRepeatMode() }

            shuffle = root.findViewById(R.id.shuffle)
            shuffle.setOnClickListener { viewModel.shuffle() }

            play = root.findViewById(R.id.play)
            play.setOnClickListener { viewModel.playPauseToggle() }

            val next: ImageButton = root.findViewById(R.id.gotoNext)
            next.setOnClickListener { viewModel.gotoNext() }

            val back: ImageButton = root.findViewById(R.id.gotoBack)
            back.setOnClickListener { viewModel.gotoBack() }

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
            queueSize = findViewById(R.id.queue_size)

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
                    context?.let { song.loadSongColor(it, play = play) }
//                    if (animate) {
                    title.fadeOut(150, {
                        title.text = song.title
                        title.fadeIn(150)
                    })
                    artistAlbum.fadeOut(200, {
                        artistAlbum.text = String.format("%s • %s", song.artistName, song.albumName)
                        artistAlbum.fadeIn(200)
                    })
//                    } else {
//                        title.text = song.title
//                        artistAlbum.text = "${song.artistName} • ${song.albumName}"
//                    }
                }
            })
            isPlaying.observe(this@PlayerFragment, Observer {
                it?.let {
                    Log.d(Companion.TAG, "idplay$it")
                    if (!it) {
                        play.setImageResource(R.drawable.ic_play_circle_filled_white_24dp)
                    } else {
                        play.setImageResource(R.drawable.ic_pause_circle_filled_white_24dp)
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
            queue.observe(this@PlayerFragment, Observer {
                it?.let {
                    playerArtPagerAdapter.accept(it)
                }
            })
            playPosition.observe(this@PlayerFragment, Observer {
                it?.let {
                    this@PlayerFragment.queueSize.text = String.format("%d / %d", it + 1, queue.value?.size ?: 0)
                    if (playerArtPagerAdapter.isNotifyDatasetChangedCalled && playerArtPagerAdapter.count > 0)
                        art.setCurrentItem(it, true)
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
            repeatMode.observe(this@PlayerFragment, Observer {
                it?.let {
                    when(it) {
                        MusicService.RepeatMode.NONE -> {
                            repeat.setImageResource(R.drawable.ic_repeat_white_24dp)
                            repeat.alpha = 0.3f
                        }
                        MusicService.RepeatMode.ONE -> {
                            repeat.setImageResource(R.drawable.ic_repeat_one_white_24dp)
                            repeat.alpha = 1f
                        }
                        MusicService.RepeatMode.ALL -> {
                            repeat.setImageResource(R.drawable.ic_repeat_white_24dp)
                            repeat.alpha = 1f
                        }
                    }
                }
            })
            isShuffle.observe(this@PlayerFragment, Observer {
                it?.let {
                    if (!it) {
                        shuffle.alpha = 0.3f
                    } else {
                        shuffle.alpha = 1.0f
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

    override fun onDestroy() {
        super.onDestroy()
        art.removeOnPageChangeListener(pagerChangeListener)
    }

    private fun showMoreDialog(song: Song) {
        val mDialog = PlayerMoreDialog()
        val bundle = Bundle()
        bundle.putParcelable(PlayerMoreDialog.ARGUMENT_SONG, song)
        mDialog.arguments = bundle
        mDialog.show(fragmentManager, "PlayerMoreDialog")
    }

    private fun showQueueUI() {
        (activity as MiniPlayerActivity).showQueue()
    }

    private fun showMainUI() {
        (activity as MiniPlayerActivity).closeNowPlay()
    }

    private fun setupViewPager() {
        art.setPageTransformer(true, ZoomOutPageTransformer())
        try {
            val mScroller = ViewPager::class.java.getDeclaredField("mScroller")
            mScroller.isAccessible = true
            val scroller = CustomScroller(art.context, FastOutSlowInInterpolator())
            // scroller.setFixedDuration(5000);
            mScroller.set(art, scroller)
        } catch (e: NoSuchFieldException) {
        } catch (e: IllegalArgumentException) {
        } catch (e: IllegalAccessException) {
        }
    }

    companion object {
        private const val TAG = "PlayerFragment"
    }
}