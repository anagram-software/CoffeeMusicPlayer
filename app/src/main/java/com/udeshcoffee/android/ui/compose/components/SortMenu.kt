package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.udeshcoffee.android.utils.SortManager

/** A sort order choice for [SortMenu]. */
data class SortOption(val key: Int, val label: String)

val SongSortOptions = listOf(
        SortOption(SortManager.SongSort.DEFAULT, "Default"),
        SortOption(SortManager.SongSort.NAME, "Title"),
        SortOption(SortManager.SongSort.TRACK_NUMBER, "Track number"),
        SortOption(SortManager.SongSort.DURATION, "Duration"),
        SortOption(SortManager.SongSort.DATE, "Date added"),
        SortOption(SortManager.SongSort.YEAR, "Year"),
        SortOption(SortManager.SongSort.ALBUM_NAME, "Album"),
        SortOption(SortManager.SongSort.ARTIST_NAME, "Artist"))

val AlbumSortOptions = listOf(
        SortOption(SortManager.AlbumSort.DEFAULT, "Default"),
        SortOption(SortManager.AlbumSort.NAME, "Title"),
        SortOption(SortManager.AlbumSort.YEAR, "Year"),
        SortOption(SortManager.AlbumSort.ARTIST_NAME, "Artist"))

val DeviceArtistSortOptions = listOf(
        SortOption(SortManager.ArtistSort.NAME, "Alphabetical"),
        SortOption(SortManager.ArtistSort.COUNT, "Most songs"))

val ServerArtistSortOptions = listOf(
        SortOption(SortManager.ArtistSort.NAME, "Alphabetical"),
        SortOption(SortManager.ArtistSort.COUNT, "Most albums"))

/** Artists: A to Z by name, most songs / albums first by count. */
fun artistNaturalAscending(order: Int) = order != SortManager.ArtistSort.COUNT

val PlaylistSortOptions = listOf(
        SortOption(SortManager.PlaylistSort.RECENTLY_PLAYED, "Recently played"),
        SortOption(SortManager.PlaylistSort.NAME, "Name"),
        SortOption(SortManager.PlaylistSort.SONG_COUNT, "Number of songs"),
        SortOption(SortManager.PlaylistSort.RECENTLY_ADDED, "Recently added"))

/** Playlists: A to Z by name, otherwise most recent / most songs first. */
fun playlistNaturalAscending(order: Int) = order == SortManager.PlaylistSort.NAME

/**
 * One sortable list in a [SortMenu]: options, current choice and direction. [ascending] is null
 * for lists the source orders itself (a server's album lists), which then show no direction control.
 */
data class SortGroup(
        val title: String?,
        val options: List<SortOption>,
        val selected: Int,
        val ascending: Boolean?,
        val onSelect: (Int) -> Unit,
        val onAscendingChange: (Boolean) -> Unit = {}
)

private val MenuCardShape = RoundedCornerShape(16.dp)
private val MenuItemShape = RoundedCornerShape(12.dp)

/**
 * The sort button and menu used by every list, in the Material 3 Expressive style (built from
 * [DropdownMenu], as this Material 3 version has no expressive menu): each group is a rounded card
 * (cards separated by gaps), the chosen order is a filled rounded item with a check, and the
 * direction is a segmented Ascending / Descending control. Picking an order closes the menu;
 * changing the direction keeps it open.
 */
@Composable
fun SortMenu(vararg groups: SortGroup) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                shape = MenuCardShape, containerColor = Color.Transparent, tonalElevation = 0.dp, shadowElevation = 0.dp) {
            Column(Modifier.padding(horizontal = 4.dp).width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                groups.forEach { group -> SortGroupCard(group, onPicked = { expanded = false }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortGroupCard(group: SortGroup, onPicked: () -> Unit) {
    Surface(shape = MenuCardShape, color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 3.dp,
            modifier = Modifier.widthIn(min = 220.dp).fillMaxWidth()) {
        Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            group.title?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp))
            }
            group.options.forEach { option ->
                val selected = option.key == group.selected
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(MenuItemShape)
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { group.onSelect(option.key); onPicked() }
                        .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                    if (selected) {
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            group.ascending?.let { ascending ->
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp)) {
                    SegmentedButton(selected = ascending, onClick = { group.onAscendingChange(true) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp)) }) {
                        Text("Ascending", maxLines = 1)
                    }
                    SegmentedButton(selected = !ascending, onClick = { group.onAscendingChange(false) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp)) }) {
                        Text("Descending", maxLines = 1)
                    }
                }
            }
        }
    }
}
