package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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

val ArtistSortOptions = listOf(
        SortOption(SortManager.ArtistSort.DEFAULT, "Default"),
        SortOption(SortManager.ArtistSort.NAME, "Name"))

/** One sortable list in a [SortMenu]: options, current choice and direction. */
data class SortGroup(
        val title: String?,
        val options: List<SortOption>,
        val selected: Int,
        val ascending: Boolean,
        val onSelect: (Int) -> Unit,
        val onAscendingChange: (Boolean) -> Unit
)

@Composable
fun SortMenu(vararg groups: SortGroup) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groups.forEachIndexed { index, group ->
                if (index > 0) HorizontalDivider()
                group.title?.let { DropdownMenuItem(text = { Text(it) }, onClick = {}, enabled = false) }
                group.options.forEach { option ->
                    DropdownMenuItem(
                            text = { Text(option.label) },
                            trailingIcon = { if (option.key == group.selected) Icon(Icons.Default.Check, contentDescription = "Selected") },
                            onClick = { group.onSelect(option.key); expanded = false })
                }
                DropdownMenuItem(
                        text = { Text("Ascending") },
                        trailingIcon = { Checkbox(checked = group.ascending, onCheckedChange = null) },
                        onClick = { group.onAscendingChange(!group.ascending); expanded = false })
            }
        }
    }
}
