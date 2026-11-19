package com.udeshcoffee.android.ui.compose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

/*
 * Material motion for page changes (the patterns of MDC's MaterialFadeThrough and
 * MaterialSharedAxis), replacing navigation-compose's defaults (whose pop scales the page down):
 *
 * - No animation between top-level destinations (the navigation bar): the tapped page shows at once.
 * - Shared axis X for moving through the hierarchy (list to detail, detail to detail, Settings):
 *   pages slide 30dp sideways and cross-fade, forwards on open and backwards on close. The
 *   predictive back gesture runs the same close transition (not the library's scale-down).
 */

private const val DURATION = 300
private const val FADE_OUT = 90 // the outgoing page is gone after the first ~30%
private val SlideDistance = 30.dp

private val topLevelRoutes = listOf(HomeRoute::class, FavoritesRoute::class, LibraryRoute::class, SearchRoute::class)

/** One of the navigation bar's destinations. */
fun NavDestination.isTopLevel() = topLevelRoutes.any { hasRoute(it) }

private fun NavBackStackEntry.isTopLevel() = destination.isTopLevel()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTopLevels() =
        initialState.isTopLevel() && targetState.isTopLevel()


private fun sharedAxisXIn(forward: Boolean, slidePx: Int): EnterTransition =
        slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { if (forward) slidePx else -slidePx } +
                fadeIn(tween(DURATION - FADE_OUT, delayMillis = FADE_OUT, easing = LinearOutSlowInEasing))

private fun sharedAxisXOut(forward: Boolean, slidePx: Int): ExitTransition =
        slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { if (forward) -slidePx else slidePx } +
                fadeOut(tween(FADE_OUT, easing = FastOutLinearInEasing))

/** Transitions for the app's NavHost; [density] converts the slide distance. */
class PageTransitions(density: Density) {
    private val slidePx = with(density) { SlideDistance.roundToPx() }

    /**
     * Where the latest navigation bar press landed (a tab page, or a detail page restored with the
     * tab's back stack). Tab presses show their page at once, without the slide that detail pages
     * otherwise get. Any later navigation clears it (see [onDestinationChanged]).
     */
    private var tabTarget: NavDestination? = null
    private var tabPressPending = false

    /** Call right before navigating for a navigation bar press. */
    fun onTabPressed() {
        tabTarget = null
        tabPressPending = true
    }

    /** Hook for NavController's OnDestinationChangedListener (called once per navigation). */
    fun onDestinationChanged(destination: NavDestination) {
        tabTarget = if (tabPressPending) destination else null
        tabPressPending = false
    }

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.instant() =
            betweenTopLevels() || (tabTarget != null && targetState.destination.id == tabTarget?.id)

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (instant()) EnterTransition.None else sharedAxisXIn(forward = true, slidePx)
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (instant()) ExitTransition.None else sharedAxisXOut(forward = true, slidePx)
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (instant()) EnterTransition.None else sharedAxisXIn(forward = false, slidePx)
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (instant()) ExitTransition.None else sharedAxisXOut(forward = false, slidePx)
    }

    // The back gesture has its own transitions (navigation-compose defaults to scaling the page down);
    // use the same pop as the back button so both ways back look the same. The Int is the swipe edge.
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = { popEnter() }
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { popExit() }
}
