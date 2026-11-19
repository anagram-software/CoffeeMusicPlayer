package com.udeshcoffee.android.baselineprofile

import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "com.udeshcoffee.android"

/** Grants what the app asks for at startup so no permission dialog blocks the journey. */
fun MacrobenchmarkScope.grantPermissions() {
    val audio = if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_AUDIO" else "android.permission.READ_EXTERNAL_STORAGE"
    device.executeShellCommand("pm grant $packageName $audio")
    if (Build.VERSION.SDK_INT >= 33) device.executeShellCommand("pm grant $packageName android.permission.POST_NOTIFICATIONS")
}

/**
 * Waits for the app to show (the navigation bar). A fresh install opens the music source setup
 * in Settings: pick this device's music, which restarts the UI on Home.
 */
fun MacrobenchmarkScope.waitForLibrary() {
    device.wait(Until.hasObject(By.text("Home")), 10_000)
    device.findObject(By.text("Use music on this device only"))?.let {
        it.click()
        device.wait(Until.hasObject(By.text("Recently added")), 10_000)
    }
}

/** The common path: Home, then the Library's album, artist and song tabs, then the player. Skips what isn't there. */
fun MacrobenchmarkScope.browseLibrary() {
    waitForLibrary()
    device.findObject(By.scrollable(true))?.let { list ->
        list.setGestureMargin(device.displayWidth / 5)
        list.fling(Direction.DOWN)
        list.fling(Direction.UP)
    }
    device.findObject(By.text("Library"))?.click()
    device.wait(Until.hasObject(By.text("Playlists")), 5_000)
    for (tab in listOf("Albums", "Artists", "Songs")) {
        device.findObject(By.text(tab))?.click()
        device.waitForIdle()
    }
    // Start something if nothing is queued (a fresh install), so the player can be opened.
    if (!device.hasObject(By.desc("Open player"))) {
        device.findObject(By.text("Shuffle all"))?.click()
        device.wait(Until.hasObject(By.desc("Open player")), 5_000)
    }
    device.findObject(By.desc("Open player"))?.let {
        it.click()
        device.wait(Until.hasObject(By.desc("Close player")), 5_000)
        device.findObject(By.desc("Pause"))?.click()
        device.findObject(By.text("Up next"))?.click()
        device.waitForIdle()
        device.pressBack() // queue -> player
        device.pressBack() // player -> library
        device.waitForIdle()
    }
}
