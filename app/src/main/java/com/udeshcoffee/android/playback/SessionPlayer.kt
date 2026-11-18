package com.udeshcoffee.android.playback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

/**
 * The player the media session exposes. Wraps ExoPlayer to add two app behaviours:
 *
 * - **Fade** on play/pause (the "Fade on play/pause" setting).
 * - **Visible shuffle**: like earlier versions, shuffling reorders the queue itself (current
 *   song first, the rest random) instead of using ExoPlayer's hidden shuffle order, so the
 *   queue screen shows the real play order and its index-based edits keep working. Turning
 *   shuffle off restores the original order.
 */
@OptIn(UnstableApi::class)
class SessionPlayer(
        player: Player,
        initialShuffle: Boolean,
        private val isFadeEnabled: () -> Boolean,
        private val onShuffleChanged: (Boolean) -> Unit
) : ForwardingSimpleBasePlayer(player) {

    private var shuffled = initialShuffle
    /** Queue order before shuffling, used to restore it. */
    private var originalOrder: List<MediaItem> = emptyList()
    private var fade: ValueAnimator? = null

    init {
        // Shuffle is ours; the wrapped player always plays in list order.
        player.shuffleModeEnabled = false
    }

    override fun getState(): State = super.getState().buildUpon().setShuffleModeEnabled(shuffled).build()

    // ---- Shuffle ----

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        if (shuffleModeEnabled == shuffled) return Futures.immediateVoidFuture()
        if (shuffleModeEnabled) {
            originalOrder = items()
            shuffleAroundCurrent()
        } else {
            restoreOriginalOrder()
            originalOrder = emptyList()
        }
        shuffled = shuffleModeEnabled
        onShuffleChanged(shuffled)
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        val result = super.handleSetMediaItems(mediaItems, startIndex, startPositionMs)
        if (shuffled) {
            // A new queue while shuffle is on: start with the chosen song, shuffle the rest.
            originalOrder = items()
            shuffleAroundCurrent()
        }
        return result
    }

    /** Moves the current item to the front and shuffles everything else after it. */
    private fun shuffleAroundCurrent() {
        if (player.mediaItemCount < 2) return
        applyOrder(QueueOrder.shuffledAround(items(), player.currentMediaItemIndex))
    }

    /** Puts the queue back in its pre-shuffle order, keeping songs added or removed meanwhile. */
    private fun restoreOriginalOrder() {
        if (player.mediaItemCount < 2 || originalOrder.isEmpty()) return
        applyOrder(QueueOrder.restored(items(), originalOrder))
    }

    /**
     * Rearranges the queue into [target] without touching the current item, so playback
     * continues uninterrupted. [target] must hold the same items as the queue.
     */
    private fun applyOrder(target: List<MediaItem>) {
        val p = player
        val count = p.mediaItemCount
        val current = p.currentMediaItemIndex
        val k = target.indexOf(p.getMediaItemAt(current)).coerceAtLeast(0)
        if (current + 1 < count) p.removeMediaItems(current + 1, count)
        if (current > 0) p.removeMediaItems(0, current)
        p.addMediaItems(0, target.subList(0, k))
        p.addMediaItems(k + 1, target.subList(k + 1, target.size))
    }

    private fun items(): List<MediaItem> = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }

    // ---- Fade ----

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        val p = player
        cancelFade()
        if (!isFadeEnabled()) {
            p.volume = 1f
            p.playWhenReady = playWhenReady
            return Futures.immediateVoidFuture()
        }
        if (playWhenReady) {
            val start = if (p.isPlaying) p.volume else 0f
            p.volume = start
            p.playWhenReady = true
            fadeVolume(start, 1f, FADE_IN_MS) {}
            return Futures.immediateVoidFuture()
        }
        if (!p.isPlaying) {
            p.playWhenReady = false
            p.volume = 1f
            return Futures.immediateVoidFuture()
        }
        val done = SettableFuture.create<Void>()
        fadeVolume(p.volume, 0f, FADE_OUT_MS) {
            p.playWhenReady = false
            p.volume = 1f
            done.set(null)
        }
        return done
    }

    private fun fadeVolume(from: Float, to: Float, durationMs: Long, onEnd: () -> Unit) {
        fade = ValueAnimator.ofFloat(from, to).apply {
            duration = durationMs
            addUpdateListener { player.volume = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) { if (!cancelled) onEnd() }
            })
            start()
        }
    }

    private fun cancelFade() {
        fade?.cancel()
        fade = null
    }

    companion object {
        private const val FADE_IN_MS = 500L
        private const val FADE_OUT_MS = 250L
    }
}
