package com.udeshcoffee.android.ui.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.theme.LocalSection

/**
 * The section's title on a rounded square in its accent colour (uppercase, 8dp corners), as in the
 * original app's toolbar and the web player's top bar.
 */
@Composable
fun SectionPill(title: String, modifier: Modifier = Modifier) {
    val section = LocalSection.current
    val background by animateColorAsState(section.accent, label = "pill")
    val content by animateColorAsState(section.onAccent, label = "pillText")
    Text(title.uppercase(), color = content, style = MaterialTheme.typography.titleSmall,
            modifier = modifier
                    .background(background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp))
}

/**
 * Scaffold for the section pages: the [title] pill, [actions], then the settings button. With
 * [onBack] it gets a back button (Settings itself, opened over a page).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelScaffold(
        title: String,
        actions: @Composable () -> Unit = {},
        floatingActionButton: @Composable () -> Unit = {},
        onBack: (() -> Unit)? = null,
        showSettings: Boolean = true,
        content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                        title = { SectionPill(title) },
                        navigationIcon = {
                            onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
                        },
                        actions = { actions(); if (showSettings) SettingsButton() },
                        scrollBehavior = scrollBehavior)
            },
            floatingActionButton = floatingActionButton,
            content = content)
}

/** Opens Settings; the last action in every section page's top app bar. */
@Composable
fun SettingsButton() {
    val actions = LocalAppActions.current
    IconButton(onClick = actions::openSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
}
