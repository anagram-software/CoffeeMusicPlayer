package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/** Scaffold for top-level destinations (no back button). */
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
            topBar = { TopAppBar(title = { Text(title) }, actions = { actions() }, scrollBehavior = scrollBehavior) },
            floatingActionButton = floatingActionButton,
            content = content)
}
