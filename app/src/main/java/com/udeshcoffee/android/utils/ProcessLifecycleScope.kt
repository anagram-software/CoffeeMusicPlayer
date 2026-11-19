package com.udeshcoffee.android.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-lifetime scope for short one-off work that must not be cancelled with a screen. */
object ProcessLifecycleScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
