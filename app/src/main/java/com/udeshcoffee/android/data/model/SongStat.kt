package com.udeshcoffee.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Udathari on 12/19/2017.
 */
@Entity(tableName = "song_stats")
data class SongStat constructor(
        @PrimaryKey @ColumnInfo(name = "songid") var id: Long,
        @ColumnInfo(name = "playcount") var playCount: Int,
        @ColumnInfo(name = "playtime") var playTime: Long
)
