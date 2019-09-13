package com.udeshcoffee.android.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Udathari on 12/19/2017.
 */
@Entity(tableName = "album_stats")
data class AlbumStat constructor(
        @PrimaryKey @ColumnInfo(name = "albumid") var id: Long,
        @ColumnInfo(name = "playcount") var playCount: Int,
        @ColumnInfo(name = "playtime") var playTime: Long
)
