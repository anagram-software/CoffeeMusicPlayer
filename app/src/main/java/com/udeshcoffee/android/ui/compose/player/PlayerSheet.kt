package com.udeshcoffee.android.ui.compose.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class PlayerSheetValue { Collapsed, Expanded }

/**
 * Material 3 motion: the standard scheme's default spatial spring (damping 0.9, stiffness 700), for
 * movement on screen. `MaterialTheme.motionScheme` is still internal in material3 1.4, so the
 * token values are used directly.
 */
private val MaterialSpatialSpring: AnimationSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 700f)

/**
 * Where the full player is: hidden below the screen (collapsed, the mini player shows) or covering
 * it (expanded), following the finger in between. Shared by the mini player (drag up) and the full
 * player (drag down), so both drive one continuous, spring-settled motion.
 */
@Stable
class PlayerSheetState(initialValue: PlayerSheetValue) {

    internal val anchored = AnchoredDraggableState(initialValue)

    /** Spring used to settle: the Material 3 motion scheme's default spatial spring. */
    internal val animationSpec: AnimationSpec<Float> = MaterialSpatialSpring

    /** Fling speed (px/s) that settles in the fling's direction regardless of position. */
    internal var velocityThreshold = 0f

    val currentValue: PlayerSheetValue get() = anchored.currentValue

    /** Expanded, or on its way there. */
    val isExpanded: Boolean get() = anchored.targetValue == PlayerSheetValue.Expanded

    private val height: Float get() = anchored.anchors.positionOf(PlayerSheetValue.Collapsed)

    /** Distance of the full player's top from the top of the screen, in px. */
    internal val offset: Float
        get() = anchored.offset.takeUnless { it.isNaN() } ?: if (currentValue == PlayerSheetValue.Expanded) 0f else height

    /** 0 when collapsed, 1 when expanded. */
    val progress: Float
        get() {
            val h = height
            if (h.isNaN() || h <= 0f) return if (currentValue == PlayerSheetValue.Expanded) 1f else 0f
            return (1f - offset / h).coerceIn(0f, 1f)
        }

    /** The full player is at least partly on screen. */
    val isVisible: Boolean get() = progress > 0f

    suspend fun expand() = anchored.animateTo(PlayerSheetValue.Expanded, animationSpec)

    suspend fun collapse() = anchored.animateTo(PlayerSheetValue.Collapsed, animationSpec)

    suspend fun snapCollapsed() = anchored.snapTo(PlayerSheetValue.Collapsed)

    /** Call with the screen height in px (the collapsed player sits just below it). */
    fun updateHeight(heightPx: Float) {
        if (heightPx == height) return
        anchored.updateAnchors(DraggableAnchors {
            PlayerSheetValue.Collapsed at heightPx
            PlayerSheetValue.Expanded at 0f
        })
    }

    /** Settles after a drag handed over by nested scrolling, keeping the fling's velocity. */
    internal suspend fun settle(velocity: Float) {
        val target = when {
            velocity > velocityThreshold -> PlayerSheetValue.Collapsed
            velocity < -velocityThreshold -> PlayerSheetValue.Expanded
            else -> anchored.anchors.closestAnchor(offset) ?: currentValue
        }
        anchored.anchoredDrag(target) { anchors, latestTarget ->
            val to = anchors.positionOf(latestTarget)
            if (!to.isNaN()) animate(offset, to, velocity, animationSpec) { value, v -> dragTo(value, v) }
        }
    }

    /**
     * Lets scrollable content inside the player (queue, lyrics) hand over: pulling down past the top
     * of a list drags the player down, and scrolling up first brings a lowered player back.
     */
    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (available.y < 0 && source == NestedScrollSource.UserInput && offset > 0f)
                    Offset(0f, anchored.dispatchRawDelta(available.y))
                else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                if (available.y > 0 && source == NestedScrollSource.UserInput) Offset(0f, anchored.dispatchRawDelta(available.y))
                else Offset.Zero

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (available.y < 0 && offset > 0f) {
                settle(available.y)
                return available
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (offset <= 0f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }

    companion object {
        val Saver = Saver<PlayerSheetState, String>({ it.anchored.settledValue.name }, { PlayerSheetState(PlayerSheetValue.valueOf(it)) })
    }
}

@Composable
fun rememberPlayerSheetState(initiallyExpanded: Boolean = false): PlayerSheetState {
    val state = rememberSaveable(saver = PlayerSheetState.Saver) {
        PlayerSheetState(if (initiallyExpanded) PlayerSheetValue.Expanded else PlayerSheetValue.Collapsed)
    }
    state.velocityThreshold = with(LocalDensity.current) { 400.dp.toPx() }
    return state
}

/** Fling behaviour for dragging the player: snaps to the nearest anchor (or the fling's direction) with the motion spring. */
@Composable
fun rememberPlayerSheetFling(state: PlayerSheetState): TargetedFlingBehavior =
        AnchoredDraggableDefaults.flingBehavior(state.anchored, positionalThreshold = { distance -> distance * 0.25f },
                animationSpec = state.animationSpec)

/** Vertical drag that moves the player (used on the mini player and the full player). */
fun Modifier.playerSheetDraggable(state: PlayerSheetState, fling: TargetedFlingBehavior): Modifier =
        anchoredDraggable(state.anchored, Orientation.Vertical, flingBehavior = fling)

/**
 * The full player, drawn over the app and moved by [state]. Only composed while on screen. Its top
 * corners round as it's dragged down, and the system back gesture shrinks it (predictive back)
 * before it slides away.
 */
@Composable
fun PlayerSheetHost(state: PlayerSheetState, content: @Composable () -> Unit) {
    if (!state.isVisible) return
    val scope = rememberCoroutineScope()
    val fling = rememberPlayerSheetFling(state)
    var backProgress by remember { mutableFloatStateOf(0f) }
    val cornerMax = with(LocalDensity.current) { 28.dp.toPx() }

    PredictiveBackHandler(enabled = state.isExpanded) { events ->
        try {
            events.collect { backProgress = it.progress }
            scope.launch { state.collapse() }
        } finally {
            scope.launch { animate(backProgress, 0f, animationSpec = state.animationSpec) { value, _ -> backProgress = value } }
        }
    }

    Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, state.offset.roundToInt()) }
                    .graphicsLayer {
                        val scale = 1f - 0.1f * backProgress
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        val radius = maxOf((1f - state.progress) * cornerMax * 4f, backProgress * cornerMax).coerceAtMost(cornerMax)
                        shape = if (radius > 0f) RoundedCornerShape(topStart = radius, topEnd = radius,
                                bottomStart = backProgress * cornerMax, bottomEnd = backProgress * cornerMax) else RoundedCornerShape(0f)
                        clip = radius > 0f
                    }
                    .nestedScroll(state.nestedScrollConnection)
                    .playerSheetDraggable(state, fling)) {
        content()
    }
}
