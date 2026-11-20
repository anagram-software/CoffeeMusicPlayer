package com.udeshcoffee.android.ui.compose.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.udeshcoffee.android.data.metabrainz.ListenBrainzStats
import com.udeshcoffee.android.data.metabrainz.TopItem
import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
import java.text.NumberFormat

private val CarouselHeight = 220.dp
/** Space above the carousel, and the same between it and the card below. */
private val SectionGap = 24.dp

/**
 * The connected ListenBrainz user's stats: a Material 3 multi-browse carousel (their
 * totals, the all-time top artist, and this week's top album and song), then this week's top 5 artists and songs. Nothing when not connected.
 */
fun LazyListScope.listenBrainzSection(state: ListenBrainzStatsState, onRetry: () -> Unit) {
    if (state == ListenBrainzStatsState.Hidden) return
    item(key = "listenbrainz") {
        Box(Modifier.padding(top = SectionGap, bottom = SectionGap)) {
            when (state) {
                is ListenBrainzStatsState.Loaded -> StatsCarousel(state.stats)
                is ListenBrainzStatsState.Failed -> Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Couldn't load your ListenBrainz stats", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
                else -> Box(Modifier.fillMaxWidth().height(CarouselHeight), contentAlignment = Alignment.Center) { PageLoadingIndicator() }
            }
        }
    }
    val stats = (state as? ListenBrainzStatsState.Loaded)?.stats ?: return
    item(key = "listenbrainz:top") {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                val numbers = remember { NumberFormat.getIntegerInstance() }
                val hasLists = stats.topArtists.isNotEmpty() || stats.topTracks.isNotEmpty()
                TopList("Top artists this week", stats.topArtists, numbers)
                if (stats.topArtists.isNotEmpty() && stats.topTracks.isNotEmpty())
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                TopList("Top songs this week", stats.topTracks, numbers)
                if (hasLists) HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileFooter(stats)
            }
        }
    }
}

/** Expressive shapes the artwork picks from at random (the more balanced ones, which read well when large and cropped). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val CarouselShapes: List<RoundedPolygon> by lazy {
    with(MaterialShapes) {
        listOf(Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided, Clover4Leaf, Clover8Leaf, Sunny, VerySunny,
                SoftBurst, SoftBoom, Flower, Puffy, PuffyDiamond, Pentagon, Gem, Ghostish, Bun, Arch, Oval)
    }
}

private sealed interface StatsPage {
    object Totals : StatsPage

    /**
     * A big number (with [numberLabel]) above a song, album or artist ([period], then [caption], then [item]), whose artwork
     * is shown in a large random shape. [palette] picks the colour role.
     */
    data class Art(val number: Long, val numberLabel: String, val period: String, val caption: String, val item: TopItem,
                   val fallbackIcon: ImageVector, val palette: Palette) : StatsPage

    enum class Palette { Surface, Tertiary, Secondary }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsCarousel(stats: ListenBrainzStats) {
    val pages = remember(stats) {
        listOfNotNull(StatsPage.Totals,
                stats.topArtistAllTime?.let {
                    StatsPage.Art(it.plays, plays(it.plays), "All time", "Top artist", it, Icons.Default.Person, StatsPage.Palette.Surface)
                },
                stats.topAlbum?.let {
                    StatsPage.Art(it.plays, plays(it.plays), "Weekly", "Top album", it, Icons.Default.Album, StatsPage.Palette.Tertiary)
                },
                stats.topTrack?.let {
                    StatsPage.Art(it.plays, plays(it.plays), "Weekly", "Top song", it, Icons.Default.MusicNote, StatsPage.Palette.Secondary)
                })
    }
    // Each item's artwork gets a different random shape, picked once per visit to Home (not on every refresh).
    val artShapes = remember { CarouselShapes.shuffled().take(3) }
    val carousel = rememberCarouselState { pages.size }
    // The page's 16dp side inset. With all four items it's the carousel's own contentPadding, so items
    // scroll out to the screen edge. With fewer, the carousel's padded start keylines and its end
    // keylines overlap and that padding vanishes abruptly on scroll, so it's a plain inset instead.
    val builtInPadding = pages.size >= 4
    HorizontalMultiBrowseCarousel(
            state = carousel,
            preferredItemWidth = 280.dp,
            itemSpacing = 8.dp,
            contentPadding = if (builtInPadding) PaddingValues(horizontal = 16.dp) else PaddingValues(),
            modifier = Modifier.fillMaxWidth().then(if (builtInPadding) Modifier else Modifier.padding(horizontal = 16.dp))
                    .height(CarouselHeight)) { index ->
        when (val page = pages[index]) {
            StatsPage.Totals -> TotalsItem(stats)
            is StatsPage.Art -> ArtItem(page, artShapes[(index - 1).coerceIn(0, artShapes.lastIndex)])
        }
    }
}

private fun plays(n: Long) = if (n == 1L) "play" else "plays"

/** Totals: everything listened to, and the album, artist and track counts. */
@Composable
private fun CarouselItemScope.TotalsItem(stats: ListenBrainzStats) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().maskClip(MaterialTheme.shapes.extraLarge).background(colors.primaryContainer).padding(20.dp)) {
        Text(numbers.format(stats.listens), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Listened", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer.copy(alpha = 0.75f), maxLines = 1)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(numbers.format(stats.albums), "Albums", Modifier.weight(1f))
            Stat(numbers.format(stats.artists), "Artists", Modifier.weight(1f))
            Stat(numbers.format(stats.tracks), "Tracks", Modifier.weight(1f))
        }
    }
}

/** The user's ListenBrainz name, and an avatar with their initial, which opens their profile. */
@Composable
private fun ProfileFooter(stats: ListenBrainzStats) {
    val context = LocalContext.current
    val openProfile = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, stats.profileUrl.toUri())) }; Unit }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stats.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ListenBrainz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        // ListenBrainz has no profile pictures.
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                .clickable(onClickLabel = "Open ListenBrainz profile", onClick = openProfile)
                .semantics { contentDescription = "ListenBrainz profile" }, contentAlignment = Alignment.Center) {
            Text(stats.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                maxLines = 1)
    }
}

/**
 * A number and a song or album, with the artwork in a large random expressive shape on the right,
 * cropped by the item's edge.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CarouselItemScope.ArtItem(page: StatsPage.Art, artShape: RoundedPolygon) {
    val item = page.item
    val colors = MaterialTheme.colorScheme
    val (container, onContainer) = when (page.palette) {
        StatsPage.Palette.Surface -> colors.surfaceContainerHighest to colors.onSurface
        StatsPage.Palette.Tertiary -> colors.tertiaryContainer to colors.onTertiaryContainer
        StatsPage.Palette.Secondary -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    val (accent, onAccent) = when (page.palette) {
        StatsPage.Palette.Surface -> colors.primary to colors.onPrimary
        StatsPage.Palette.Tertiary -> colors.tertiary to colors.onTertiary
        StatsPage.Palette.Secondary -> colors.secondary to colors.onSecondary
    }
    val numbers = remember { NumberFormat.getIntegerInstance() }
    Box(Modifier.fillMaxSize().maskClip(MaterialTheme.shapes.extraLarge).background(container)) {
        // Larger than the item and pushed past its right edge, which crops it.
        val art = Modifier.align(Alignment.CenterEnd).offset(x = 80.dp).requiredSize(260.dp).clip(artShape.toShape()).background(accent)
        val image = item.image
        if (image != null) {
            AsyncImage(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = art)
        } else {
            Box(art, contentAlignment = Alignment.Center) {
                Icon(page.fallbackIcon, contentDescription = null, tint = onAccent, modifier = Modifier.offset(x = (-40).dp).size(64.dp))
            }
        }
        Column(Modifier.fillMaxHeight().width(150.dp).padding(16.dp)) {
            Text(numbers.format(page.number), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = onContainer, maxLines = 1)
            Text(page.numberLabel, style = MaterialTheme.typography.labelLarge, color = onContainer.copy(alpha = 0.75f), maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(page.period, style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.6f), maxLines = 1)
            Text(page.caption, style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.75f), maxLines = 1)
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = onContainer,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            item.artist?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A ranked top-5 list (artists, or songs with their artist). */
@Composable
private fun TopList(title: String, items: List<TopItem>, numbers: NumberFormat) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    items.forEachIndexed { i, item ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.artist?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(if (item.plays == 1L) "1 play" else "${numbers.format(item.plays)} plays", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
