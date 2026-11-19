package com.udeshcoffee.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the startup and library-browsing code paths into the app's baseline profile. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        grantPermissions()
        pressHome()
        startActivityAndWait()
        browseLibrary()
    }
}
