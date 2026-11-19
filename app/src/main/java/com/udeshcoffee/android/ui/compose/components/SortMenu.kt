package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MenuItemShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
        val options: List<SortOption>,
        val selected: Int,
        val ascending: Boolean?,
        val onSelect: (Int) -> Unit,
        val onAscendingChange: (Boolean) -> Unit = {}
)

/**
 * The sort button and menu used by every list: a Material 3 Expressive grouped menu. Each list
 * is a group of selectable orders (no title), followed by a group with its direction
 * (Ascending / Descending) unless the source orders it. Picking an order closes the menu;
 * changing the direction keeps it open.
 */
@Composable
fun SortMenu(vararg groups: SortGroup) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") }
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            val total = groups.sumOf { if (it.ascending != null) 2 else 1 }
            var index = 0
            groups.forEach { group ->
                if (index > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                DropdownMenuGroup(shapes = MenuDefaults.groupShape(index++, total)) {
                    group.options.forEachIndexed { i, option ->
                        SelectableDropdownMenuItem(
                                selected = option.key == group.selected,
                                onClick = { group.onSelect(option.key); expanded = false },
                                text = { Text(option.label) },
                                shapes = slotShape(i, group.options.size),
                                // An empty slot the size of the check keeps every label aligned.
                                leadingIcon = { Spacer(Modifier.size(MenuDefaults.LeadingIconSize)) },
                                selectedLeadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(MenuDefaults.LeadingIconSize))
                                })
                    }
                }
                group.ascending?.let { ascending ->
                    Spacer(Modifier.height(MenuDefaults.GroupSpacing))
                    DropdownMenuGroup(shapes = MenuDefaults.groupShape(index++, total)) {
                        SelectableDropdownMenuItem(
                                selected = ascending,
                                onClick = { group.onAscendingChange(true) },
                                text = { Text("Ascending") },
                                shapes = slotShape(0, 2),
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(MenuDefaults.LeadingIconSize)) })
                        SelectableDropdownMenuItem(
                                selected = !ascending,
                                onClick = { group.onAscendingChange(false) },
                                text = { Text("Descending") },
                                shapes = slotShape(1, 2),
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(MenuDefaults.LeadingIconSize)) })
                    }
                }
            }
        }
    }
}

/**
 * The item's segmented shape for its position in the group, kept when selected: Material's default
 * morphs the selected item into a separate rounded pill, which looks out of place in a list of
 * options where one is always selected. Selection shows as the tint and check instead.
 */
@Composable
private fun slotShape(index: Int, count: Int): MenuItemShapes {
    val slot = MenuDefaults.itemShape(index, count).shape
    return MenuDefaults.itemShapes(shape = slot, selectedShape = slot)
}
