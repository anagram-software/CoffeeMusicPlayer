package com.udeshcoffee.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.components.SongRow
import com.udeshcoffee.android.ui.compose.components.SongSortOptions
import com.udeshcoffee.android.ui.compose.components.SortGroup
import com.udeshcoffee.android.ui.compose.components.SortMenu
import com.udeshcoffee.android.ui.compose.components.TextInputDialog
import com.udeshcoffee.android.utils.SortManager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    private val song = Song(1, "Bohemian Rhapsody", 2, "A Night at the Opera", 3, "Queen", 354_000, 1975, 0, 0, 11)

    @Test
    fun songRow_showsSongAndReportsClicks() {
        var clicks = 0
        var more = 0
        rule.setContent {
            MaterialTheme { SongRow(song, onClick = { clicks++ }, onMore = { more++ }, showArtwork = false, isCurrent = true) }
        }
        rule.onNodeWithText("Bohemian Rhapsody").assertIsDisplayed().performClick()
        rule.onNodeWithText("Queen • A Night at the Opera").assertIsDisplayed()
        rule.onNodeWithContentDescription("Playing").assertIsDisplayed()
        rule.onNodeWithContentDescription("Song options").performClick()
        assertEquals(1, clicks)
        assertEquals(1, more)
    }

    @Test
    fun sortMenu_selectsAnOption() {
        var selected = SortManager.SongSort.DEFAULT
        rule.setContent {
            MaterialTheme {
                SortMenu(SortGroup(null, SongSortOptions, selected, true, { selected = it }, {}))
            }
        }
        rule.onNodeWithContentDescription("Sort").performClick()
        rule.onNodeWithText("Date added").performClick()
        assertEquals(SortManager.SongSort.DATE, selected)
    }

    @Test
    fun textInputDialog_needsTextAndTrimsIt() {
        var result: String? = null
        rule.setContent {
            MaterialTheme { TextInputDialog("New playlist", "Name", confirm = "Create", onDismiss = {}) { result = it } }
        }
        rule.onNodeWithText("Create").assertIsNotEnabled()
        rule.onNodeWithText("Name").performTextInput("  Road trip  ")
        rule.onNodeWithText("Create").assertIsEnabled().performClick()
        assertEquals("Road trip", result)
    }
}
