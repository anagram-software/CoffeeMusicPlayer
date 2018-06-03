package com.udeshcoffee.android.ui

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.transition.Fade
import android.util.Log
import android.view.View
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.loadSongColor
import com.udeshcoffee.android.extensions.replaceFragmentToActivity
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerFragment
import com.udeshcoffee.android.ui.player.player.PlayerFragment
import com.udeshcoffee.android.ui.player.queue.QueueFragment
import com.udeshcoffee.android.views.FadableLayout
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject

/**
 * Created by Udathari on 8/25/2017.
 */

abstract class MiniPlayerActivity : BaseActivity() {

    open val miniPlayerFragment: MiniPlayerFragment by inject()

    private val playerFragment: PlayerFragment by inject()
    private val queueFragment: QueueFragment by inject()

    private var isQueueVisible = false

    private lateinit var controlBack: FadableLayout
    private lateinit var slidingPanel: SlidingUpPanelLayout

    // State
    private var controlBackSongId: Long = -1

    private lateinit var broadcastDisposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        slidingPanel = findViewById(R.id.slidingpanel)
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
        slidingPanel.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                if (slideOffset <= 0.2f) {
                    miniPlayerFragment.setAlpha(1f)
                } else if (slideOffset > 0.2f && slideOffset <= 0.6f) {
                    miniPlayerFragment.setVisibility(View.VISIBLE)
                    miniPlayerFragment.setAlpha(1 - (slideOffset - 0.2f) / 0.4f)
                } else if (slideOffset > 0.6f) {
                    miniPlayerFragment.setVisibility(View.INVISIBLE)
                }
            }

            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED)
                    onPlayerExpanded()
                else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED || newState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    onPlayerCollapsed()
                }

                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    miniPlayerFragment.setVisibility(View.VISIBLE)
                    miniPlayerFragment.setAlpha(1f)
                }
            }

        })

        // Control Back
        controlBack = findViewById(R.id.controls_back)

        // Mini Player Fragment
        replaceFragmentToActivity(miniPlayerFragment, R.id.mini_player_container)

        replaceFragmentToActivity(playerFragment, R.id.player_container)
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(this, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            setControlBack()
                        }
                        MusicService.InternalIntents.SERVICE_CONNECTED -> {
                            setControlBack()
                        }
                    }
                }
        setControlBack()
    }

    override fun onPause() {
        super.onPause()
        if (!broadcastDisposable.isDisposed)
            broadcastDisposable.dispose()
    }

    fun openNowPlay(){
        if (slidingPanel.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED)
            slidingPanel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
    }

    fun closeNowPlay(){
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    fun hideNowPlay(){
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
    }

    fun initNowPlay(){
        if (slidingPanel.panelState == SlidingUpPanelLayout.PanelState.HIDDEN)
            closeNowPlay()
    }

    fun setSlidingView(view: View) {
        slidingPanel.setScrollableView(view)
    }

    private fun setControlBack(){
        val service = getService()
        service?.let {
            it.currentSong()?.let {
                if (controlBackSongId != it.id) {
                    it.loadSongColor(this, controlBack)
                    controlBackSongId = it.id
                }
            }
        }
    }

    fun showQueue() {
        isQueueVisible = true
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.player_container, queueFragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            queueFragment.enterTransition = Fade()
            playerFragment.exitTransition = Fade()
        }
        transaction.commit()
        slidingPanel.isTouchEnabled = false
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    fun showPlayer() {
        isQueueVisible = false
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.player_container, playerFragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            playerFragment.enterTransition = Fade()
            queueFragment.exitTransition = Fade()
        }
        transaction.commit()
        slidingPanel.isTouchEnabled = true
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    open fun canChangeMainContent(): Boolean {
        return slidingPanel.panelState != SlidingUpPanelLayout.PanelState.EXPANDED
    }

    open fun onPlayerExpanded() {}

    open fun onPlayerCollapsed() {}

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressedSuper")
        when {
            isQueueVisible -> showPlayer()
            slidingPanel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED -> closeNowPlay()
            else -> {
                moveTaskToBack(true)
            }
        }
    }
}