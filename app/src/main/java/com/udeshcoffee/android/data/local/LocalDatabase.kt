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

import androidx.room.Database
import androidx.room.RoomDatabase
import com.udeshcoffee.android.data.model.*

/**
 * The app's own data: play stats, artist bios, favorites and playlists.
 * This release starts fresh (no upgrade path from the old Play Store versions), at version 1.
 */
@Database(entities = [SongStat::class, AlbumStat::class, ArtistStat::class, Bio::class,
        Favorite::class, UserPlaylist::class, PlaylistItem::class],
        version = 1, exportSchema = true)
abstract class LocalDatabase : RoomDatabase() {

    abstract fun dataDao(): DataDao

    abstract fun playlistDao(): PlaylistDao
}