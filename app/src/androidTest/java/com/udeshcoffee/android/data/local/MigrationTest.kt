package com.udeshcoffee.android.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Database upgrades. Release 2.0.13 (DB v6) predates schema export, so the v6 schema in
 * androidTest/assets is hand-made: the exported v7 schema minus the playlist tables that
 * [LocalDatabase.Migration_6_7] adds (the other entity classes were unchanged).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LocalDatabase::class.java)

    @Test
    fun migrate6To7_keepsDataAndAddsPlaylists() {
        helper.createDatabase(DB, 6).use { db ->
            db.execSQL("INSERT INTO favorites (fav) VALUES (42)")
            db.execSQL("INSERT INTO song_stats (songid, playcount, playtime) VALUES (42, 3, 1000)")
        }

        // Validates the result against the exported v7 schema.
        helper.runMigrationsAndValidate(DB, 7, true, LocalDatabase.Migration_6_7).use { db ->
            db.query("SELECT fav FROM favorites").use { c ->
                c.moveToFirst(); assertEquals(42L, c.getLong(0))
            }
            db.query("SELECT playcount FROM song_stats WHERE songid = 42").use { c ->
                c.moveToFirst(); assertEquals(3, c.getInt(0))
            }
            db.execSQL("INSERT INTO playlists (name, date_added) VALUES ('Mix', 0)")
            db.execSQL("INSERT INTO playlist_items (playlist_id, song_id, position) VALUES (1, 42, 0)")
            // Deleting a playlist removes its items (foreign key cascade).
            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM playlists WHERE id = 1")
            db.query("SELECT COUNT(*) FROM playlist_items").use { c ->
                c.moveToFirst(); assertEquals(0, c.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
