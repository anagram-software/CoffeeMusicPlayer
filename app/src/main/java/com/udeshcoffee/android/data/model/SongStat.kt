package com.udeshcoffee.android.data.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by Udathari on 12/19/2017.
 */
@Entity(tableName = "song_stats")
data class SongStat constructor(
        @PrimaryKey @ColumnInfo(name = "songid") var id: Long,
        @ColumnInfo(name = "playcount") var playCount: Int,
        @ColumnInfo(name = "playtime") var playTime: Long
)
