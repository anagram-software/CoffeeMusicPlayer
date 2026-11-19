/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.udeshcoffee.android.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.udeshcoffee.android.data.model.*

/**
 * The Room Database that contains the Task table.
 */
@Database(entities = [SongStat::class, AlbumStat::class, ArtistStat::class, Bio::class,
        EQPreset::class, Favorite::class, Lyric::class, UserPlaylist::class, PlaylistItem::class],
        version = 7, exportSchema = true)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun dataDao(): DataDao

    abstract fun playlistDao(): PlaylistDao

    companion object {

        val Migration_5_6 = object: Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                        "CREATE TABLE song_stats (songid INTEGER, playcount INTEGER, playtime INTEGER, PRIMARY KEY(songid))")
                // Copy the data
                database.execSQL(
                        "INSERT INTO song_stats (songid, playcount, playtime) SELECT songid, rplayedid, rplayed FROM playcount")
                // Remove the old table
                database.execSQL("DROP TABLE playcount");
            }
        }

        /** Adds app-owned playlists (replacing MediaStore playlists). */
        val Migration_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `date_added` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlist_id` INTEGER NOT NULL, `song_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_items_playlist_id` ON `playlist_items` (`playlist_id`)")
            }
        }
    }
}