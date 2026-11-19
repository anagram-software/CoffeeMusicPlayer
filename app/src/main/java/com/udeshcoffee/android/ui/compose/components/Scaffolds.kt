package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.udeshcoffee.android.ui.compose.LocalAppActions

/** Scaffold for top-level destinations: no back button, [actions] then the settings button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelScaffold(
        title: String,
        actions: @Composable () -> Unit = {},
        floatingActionButton: @Composable () -> Unit = {},
        content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(title = { Text(title) }, actions = { actions(); SettingsButton() }, scrollBehavior = scrollBehavior)
            },
            floatingActionButton = floatingActionButton,
            content = content)
}

/** Opens Settings; the last action in every top-level screen's top app bar. */
@Composable
fun SettingsButton() {
    val actions = LocalAppActions.current
    IconButton(onClick = actions::openSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
}
